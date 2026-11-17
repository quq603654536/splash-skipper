package com.wkm.splashskip;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class MainActivity extends Activity {
    private static final int GREEN=Color.rgb(13,119,98),INK=Color.rgb(24,43,37),MUTED=Color.rgb(105,122,115),BG=Color.rgb(244,247,245);
    private final Handler handler=new Handler(Looper.getMainLooper());
    private Store store;
    private LinearLayout root,body,nav;
    private TextView serviceStatus,countText;
    private Switch master;
    private int tab=0;
    private boolean resumed;
    private List<App> apps;
    private final Runnable tick=new Runnable(){public void run(){refreshStatus();if(resumed)handler.postDelayed(this,1000);}};
    private static final class App { String pkg,name; android.graphics.drawable.Drawable icon; }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);store=new Store(this);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        root=column();root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            if(android.os.Build.VERSION.SDK_INT>=30){
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
                v.setPadding(bars.left,bars.top,bars.right,bars.bottom);
            } else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(root);root.requestApplyInsets();render();
    }
    @Override protected void onResume(){super.onResume();resumed=true;handler.removeCallbacks(tick);handler.post(tick);}
    @Override protected void onPause(){resumed=false;handler.removeCallbacks(tick);super.onPause();}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);super.onDestroy();}

    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView text(String value,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setLineSpacing(dp(3),1);return t;
    }
    private void gap(LinearLayout parent,int height){View v=new View(this);parent.addView(v,new LinearLayout.LayoutParams(1,dp(height)));}
    private LinearLayout card(LinearLayout parent){
        LinearLayout c=column();c.setPadding(dp(20),dp(20),dp(20),dp(20));c.setBackground(shape(Color.WHITE,22));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(14);parent.addView(c,p);return c;
    }
    private Button button(String label,boolean primary,Runnable action){
        Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(15);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setTextColor(primary?Color.WHITE:GREEN);b.setBackground(shape(primary?GREEN:Color.rgb(233,244,238),14));
        b.setPadding(dp(14),dp(10),dp(14),dp(10));b.setMinHeight(dp(48));b.setMinimumHeight(dp(48));b.setOnClickListener(v->action.run());return b;
    }
    private void heading(LinearLayout c,String title,String subtitle){c.addView(text(title,19,INK,true));if(!subtitle.isEmpty()){gap(c,7);c.addView(text(subtitle,13,MUTED,false));}}

    private void render(){
        root.removeAllViews();serviceStatus=null;countText=null;master=null;
        LinearLayout header=column();header.setPadding(dp(24),dp(22),dp(24),dp(14));
        header.addView(text(tab==0?"开屏快跳":tab==1?"应用范围":"跳过记录",29,INK,true));root.addView(header);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);
        body=column();body.setPadding(dp(20),dp(4),dp(20),dp(20));scroll.addView(body);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(tab==0)home();else if(tab==1)applications();else history();
        nav=row();nav.setPadding(dp(14),dp(10),dp(14),dp(10));nav.setBackgroundColor(Color.WHITE);
        String[] labels={"概览","应用","记录"};
        for(int i=0;i<3;i++){final int selected=i;Button b=button(labels[i],tab==i,()->{tab=selected;render();});
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(46),1);p.leftMargin=dp(4);p.rightMargin=dp(4);nav.addView(b,p);}
        root.addView(nav);refreshStatus();
    }
    private void home(){
        LinearLayout hero=card(body);hero.setBackground(shape(GREEN,24));
        serviceStatus=text("检查服务状态…",13,Color.rgb(207,236,224),true);hero.addView(serviceStatus);gap(hero,10);
        countText=text(Long.toString(store.count()),42,Color.WHITE,true);hero.addView(countText);
        hero.addView(text("累计跳过点击",13,Color.rgb(207,236,224),false));

        LinearLayout control=card(body);
        master=setting(control,"自动跳过","",store.enabled(),v->{store.enabled(v);refreshStatus();});
        gap(control,12);control.addView(button("无障碍设置",true,this::accessibility),new LinearLayout.LayoutParams(-1,dp(48)));

        LinearLayout setup=card(body);heading(setup,"后台设置","");gap(setup,12);
        LinearLayout shortcuts=row();setup.addView(shortcuts);
        LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,dp(48),1);left.rightMargin=dp(5);
        LinearLayout.LayoutParams right=new LinearLayout.LayoutParams(0,dp(48),1);right.leftMargin=dp(5);
        shortcuts.addView(button("自启动",false,this::autostart),left);
        shortcuts.addView(button("省电策略",false,this::appInfo),right);gap(setup,10);
        setup.addView(text("开启自启动 · 省电无限制 · 锁定后台",12,MUTED,false));

        LinearLayout options=card(body);
        setting(options,"增强识别","可能跳过首次引导",store.enhanced(),v->store.enhanced(v));gap(options,18);
        setting(options,"兼容点击","控件无法点击时模拟触摸",store.gestures(),v->store.gestures(v));

        TextView version=text("1.0.3",11,MUTED,false);version.setGravity(Gravity.CENTER);body.addView(version);
    }
    private interface Changed{void set(boolean value);}
    private Switch setting(LinearLayout parent,String title,String detail,boolean value,Changed change){
        LinearLayout r=row(),labels=column();labels.addView(text(title,16,INK,true));
        if(!detail.isEmpty()){gap(labels,5);labels.addView(text(detail,12,MUTED,false));}
        r.addView(labels,new LinearLayout.LayoutParams(0,-2,1));Switch toggle=new Switch(this);toggle.setContentDescription(title);toggle.setChecked(value);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.leftMargin=dp(12);r.addView(toggle,p);
        toggle.setOnCheckedChangeListener((b,v)->change.set(v));parent.addView(r);
        return toggle;
    }
    private void refreshStatus(){
        if(countText!=null)countText.setText(Long.toString(store.count()));
        if(master!=null && master.isChecked()!=store.enabled())master.setChecked(store.enabled());
        if(serviceStatus!=null){
            boolean enabled=accessibilityEnabled();
            serviceStatus.setText(!store.enabled()?"● 已暂停":SkipService.connected?"● 服务已连接":enabled?"● 连接中":"○ 未开启无障碍");
        }
    }
    private boolean accessibilityEnabled(){
        String enabled=Settings.Secure.getString(getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        ComponentName name=new ComponentName(this,SkipService.class);
        if(enabled!=null)for(String v:enabled.split(":"))if(name.equals(ComponentName.unflattenFromString(v)))return true;
        return false;
    }
    private void accessibility(){
        Intent i=new Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS");i.putExtra(Intent.EXTRA_COMPONENT_NAME,new ComponentName(this,SkipService.class));
        if(!open(i))open(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }
    private boolean open(Intent i){try{startActivity(i);return true;}catch(Exception e){return false;}}
    private void appInfo(){open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));}
    private void autostart(){
        Intent i=new Intent();i.setComponent(new ComponentName("com.miui.securitycenter","com.miui.permcenter.autostart.AutoStartManagementActivity"));
        if(!open(i)){Toast.makeText(this,"请在应用信息中设置后台运行权限",Toast.LENGTH_LONG).show();appInfo();}
    }
    private void applications(){
        EditText search=new EditText(this);search.setSingleLine(true);search.setTextSize(15);search.setHint("搜索应用");search.setPadding(dp(16),dp(12),dp(16),dp(12));search.setBackground(shape(Color.WHITE,14));body.addView(search,new LinearLayout.LayoutParams(-1,dp(52)));gap(body,14);
        LinearLayout list=column();body.addView(list);TextView loading=text("正在读取应用列表…",14,MUTED,false);list.addView(loading);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){showApps(list,s.toString());}public void afterTextChanged(Editable e){}});
        if(apps!=null){showApps(list,"");return;}
        new Thread(()->{
            List<App> result=new ArrayList<>();Set<String> seen=new HashSet<>();
            Intent i=new Intent(Intent.ACTION_MAIN);i.addCategory(Intent.CATEGORY_LAUNCHER);
            for(ResolveInfo info:getPackageManager().queryIntentActivities(i,0)){
                String pkg=info.activityInfo.packageName;
                if(pkg.equals(getPackageName())||SkipService.systemPackage(pkg)||!seen.add(pkg))continue;
                try { App a=new App();a.pkg=pkg;a.name=info.loadLabel(getPackageManager()).toString();a.icon=info.loadIcon(getPackageManager());result.add(a); }
                catch(RuntimeException ignored) {}
            }
            Collections.sort(result,(a,b)->a.name.compareToIgnoreCase(b.name));
            runOnUiThread(()->{if(isFinishing()||isDestroyed())return;apps=result;if(tab==1)showApps(list,search.getText().toString());});
        },"app-list").start();
    }
    private void showApps(LinearLayout list,String query){
        if(apps==null)return;list.removeAllViews();String q=query.toLowerCase(Locale.ROOT);int count=0;
        for(App a:apps){
            if(!a.name.toLowerCase(Locale.ROOT).contains(q)&&!a.pkg.toLowerCase(Locale.ROOT).contains(q))continue;
            count++;LinearLayout c=card(list);c.setPadding(dp(14),dp(14),dp(14),dp(14));LinearLayout r=row();c.addView(r);
            ImageView icon=new ImageView(this);icon.setImageDrawable(a.icon);r.addView(icon,new LinearLayout.LayoutParams(dp(40),dp(40)));
            LinearLayout labels=column();labels.setPadding(dp(12),0,dp(8),0);labels.addView(text(a.name,15,INK,true));TextView pkg=text(a.pkg,10,MUTED,false);pkg.setMaxLines(2);labels.addView(pkg);
            r.addView(labels,new LinearLayout.LayoutParams(0,-2,1));Switch s=new Switch(this);s.setContentDescription(a.name+"自动跳过");s.setChecked(store.allowed(a.pkg));s.setOnCheckedChangeListener((b,v)->store.allowed(a.pkg,v));r.addView(s);
        }
        if(count==0)list.addView(text("没有匹配的应用",14,MUTED,false));
    }
    private void history(){
        JSONArray records=store.records();
        if(records.length()==0){LinearLayout empty=card(body);heading(empty,"暂无记录","");return;}
        SimpleDateFormat format=new SimpleDateFormat("MM-dd HH:mm:ss",Locale.getDefault());
        for(int i=0;i<records.length();i++){
            JSONObject record=records.optJSONObject(i);if(record==null)continue;
            LinearLayout c=card(body);heading(c,record.optString("name"),format.format(new Date(record.optLong("time")))+"  ·  "+record.optString("route"));
        }
        body.addView(button("清空记录",false,()->{store.clearRecords();render();}),new LinearLayout.LayoutParams(-1,dp(48)));
    }
}
