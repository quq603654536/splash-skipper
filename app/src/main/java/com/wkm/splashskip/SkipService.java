package com.wkm.splashskip;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.KeyguardManager;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public final class SkipService extends AccessibilityService {
    public static volatile boolean connected;
    private static final Set<String> BLOCKED=new HashSet<>(Arrays.asList(
        "android","com.android.systemui","com.android.settings","com.miui.home","com.miui.securitycenter",
        "com.lbe.security.miui","com.android.permissioncontroller","com.google.android.permissioncontroller",
        "com.android.packageinstaller","com.miui.packageinstaller","com.google.android.packageinstaller",
        "com.android.incallui","com.android.dialer","com.android.server.telecom"));
    private final Handler handler=new Handler(Looper.getMainLooper());
    private Store store;
    private final LaunchSession session=new LaunchSession();
    private boolean pending;
    private boolean observationPending;
    private final Runnable scanTask=()->{ pending=false; scan(); };
    private final Runnable observeTask=()->{observationPending=false;observeWindow();};

    @Override protected void onServiceConnected() {
        super.onServiceConnected();store=new Store(this);connected=true;Log.i("SplashSkip","Service connected");
        observationPending=true;handler.postDelayed(observeTask,100);
    }
    public static boolean systemPackage(String pkg) { return pkg==null || BLOCKED.contains(pkg) || pkg.startsWith("com.android.inputmethod") || pkg.startsWith("com.google.android.inputmethod"); }
    private boolean eligible(String pkg) { return !getPackageName().equals(pkg) && !systemPackage(pkg) && store.allowed(pkg); }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if(store==null || !store.enabled())return;
        CharSequence p=event.getPackageName();
        String pkg=value(p);
        int type=event.getEventType();
        if(type==AccessibilityEvent.TYPE_WINDOWS_CHANGED || type==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || !pkg.equals(session.packageName())) {
            if(!observationPending){observationPending=true;handler.postDelayed(observeTask,100);}
        }
        if(type==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && !pkg.isEmpty() && !pkg.equals("com.android.systemui")) {
            String cls=value(event.getClassName());
            boolean resumed=cls.endsWith("Activity")||cls.contains(".activity.")||cls.contains(".activities.");
            observePackage(pkg,event.getWindowId(),resumed);
        }
        if(pkg.equals(session.packageName()) && eligible(pkg) && session.active(SystemClock.elapsedRealtime()))schedule(110);
    }
    private void observePackage(String pkg,int window,boolean resumed){
        if(session.observe(pkg,window,resumed,SystemClock.elapsedRealtime())){
            handler.removeCallbacks(scanTask);pending=false;
        }
    }
    private void observeWindow(){
        if(store==null||!store.enabled())return;
        AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null)return;
        try{
            String pkg=value(root.getPackageName());
            if(!pkg.isEmpty()&&!pkg.equals("com.android.systemui")){
                observePackage(pkg,root.getWindowId(),false);
                if(eligible(pkg)&&session.active(SystemClock.elapsedRealtime()))schedule(100);
            }
        }finally{root.recycle();}
    }
    private void schedule(long delay) { if(!pending){ pending=true;handler.postDelayed(scanTask,delay); } }
    private static String value(CharSequence s) { return s==null?"":s.toString(); }
    private void scan() {
        String foreground=session.packageName();
        if(store==null || !store.enabled() || !session.active(SystemClock.elapsedRealtime()) || !eligible(foreground))return;
        PowerManager power=getSystemService(PowerManager.class);
        KeyguardManager keyguard=getSystemService(KeyguardManager.class);
        if((power!=null&&!power.isInteractive()) || (keyguard!=null&&keyguard.isKeyguardLocked()))return;
        AccessibilityNodeInfo root=getRootInActiveWindow();
        if(root==null){schedule(300);return;}
        if(!foreground.equals(value(root.getPackageName()))){
            root.recycle();schedule(150);return;
        }
        List<AccessibilityNodeInfo> nodes=new ArrayList<>(); nodes.add(root);
        boolean ad=false,sensitive=false;
        try {
            for(int i=0;i<nodes.size() && i<600;i++) {
                AccessibilityNodeInfo node=nodes.get(i);
                if(!node.isVisibleToUser())continue;
                String t=value(node.getText()),d=value(node.getContentDescription());
                ad |= SkipMatcher.adMarker(t)||SkipMatcher.adMarker(d);
                sensitive |= SkipMatcher.sensitive(t)||SkipMatcher.sensitive(d);
                for(int j=0;j<node.getChildCount() && nodes.size()<600;j++) {
                    AccessibilityNodeInfo child=node.getChild(j); if(child!=null)nodes.add(child);
                }
            }
            if(sensitive)return;
            int sw=getResources().getDisplayMetrics().widthPixels,sh=getResources().getDisplayMetrics().heightPixels;
            float density=getResources().getDisplayMetrics().density;
            AccessibilityNodeInfo best=null; Rect bestBounds=null; int bestScore=0;
            for(AccessibilityNodeInfo node:nodes) {
                if(!node.isVisibleToUser() || !node.isEnabled())continue;
                String label=value(node.getText()),description=value(node.getContentDescription());
                if(!SkipMatcher.possibleLabel(label)&&!SkipMatcher.possibleLabel(description))continue;
                Rect b=new Rect(); node.getBoundsInScreen(b);
                int a=SkipMatcher.score(label,node.getViewIdResourceName(),b.left,b.top,b.width(),b.height(),sw,sh,density,ad,false,store.enhanced());
                int c=SkipMatcher.score(description,node.getViewIdResourceName(),b.left,b.top,b.width(),b.height(),sw,sh,density,ad,false,store.enhanced());
                if(Math.max(a,c)>bestScore){best=node;bestBounds=b;bestScore=Math.max(a,c);}
            }
            if(best!=null && bestBounds!=null) {
                if(clickNode(best,bestBounds)) { session.handled(true);store.record(foreground,"控件点击");Log.i("SplashSkip","Clicked skip in "+foreground); }
                else if(store.gestures()) {
                    final String pkg=foreground;
                    Path path=new Path();path.moveTo(bestBounds.exactCenterX(),bestBounds.exactCenterY());
                    GestureDescription gesture=new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(path,0,50)).build();
                    session.handled(true);
                    boolean dispatched=dispatchGesture(gesture,new GestureResultCallback(){
                        @Override public void onCompleted(GestureDescription g){ store.record(pkg,"兼容点击");Log.i("SplashSkip","Tapped skip in "+pkg); }
                        @Override public void onCancelled(GestureDescription g){ Log.i("SplashSkip","Gesture cancelled"); }
                    },null);
                    if(!dispatched)session.handled(false);
                }
            }
        } catch(RuntimeException e) { Log.w("SplashSkip","Window changed during scan"); }
        finally { for(AccessibilityNodeInfo n:nodes)n.recycle(); }
        if(!session.handled())schedule(session.age(SystemClock.elapsedRealtime())<2000?250:600);
    }
    private boolean clickNode(AccessibilityNodeInfo node,Rect candidate) {
        if(node.isClickable() && node.performAction(AccessibilityNodeInfo.ACTION_CLICK))return true;
        AccessibilityNodeInfo parent=node.getParent();
        for(int i=0;i<2 && parent!=null;i++) {
            Rect b=new Rect();parent.getBoundsInScreen(b);
            boolean small=b.width()<=candidate.width()+Math.round(40*getResources().getDisplayMetrics().density)
                && b.height()<=candidate.height()+Math.round(32*getResources().getDisplayMetrics().density);
            if(small && parent.isClickable() && parent.isEnabled() && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)){parent.recycle();return true;}
            AccessibilityNodeInfo next=parent.getParent();parent.recycle();parent=next;
        }
        if(parent!=null)parent.recycle();
        return false;
    }
    @Override protected void dump(FileDescriptor fd,PrintWriter out,String[] args){
        out.println("SplashSkip serviceConnected="+connected+" enabled="+(store!=null&&store.enabled()));
        out.println("foreground="+session.packageName()+" sessionAgeMs="+session.age(SystemClock.elapsedRealtime())+" handled="+session.handled()+" scanPending="+pending);
        PowerManager power=getSystemService(PowerManager.class);KeyguardManager keyguard=getSystemService(KeyguardManager.class);
        out.println("interactive="+(power!=null&&power.isInteractive())+" keyguardLocked="+(keyguard!=null&&keyguard.isKeyguardLocked()));
    }
    @Override public void onInterrupt() { handler.removeCallbacksAndMessages(null);pending=false;observationPending=false; }
    @Override public void onDestroy() { connected=false;handler.removeCallbacksAndMessages(null);super.onDestroy(); }
}
