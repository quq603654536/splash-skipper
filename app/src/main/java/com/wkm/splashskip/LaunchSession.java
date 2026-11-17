package com.wkm.splashskip;

/** Foreground session accounting independent of Android event delivery. */
public final class LaunchSession {
    public static final long WINDOW_MS=12_000;
    private String pkg="";
    private int window=-1;
    private long enteredAt;
    private boolean handled;
    public String packageName(){return pkg;}
    public long age(long now){return Math.max(0,now-enteredAt);}
    public boolean handled(){return handled;}
    public void handled(boolean value){handled=value;}
    public boolean active(long now){return !handled && age(now)<WINDOW_MS;}
    public boolean observe(String next,int windowId,boolean activityResumed,long now){
        if(next==null||next.isEmpty())return false;
        boolean changed=!next.equals(pkg);
        boolean newActivity=activityResumed && window!=-1 && windowId!=-1 && window!=windowId;
        boolean returned=activityResumed && age(now)>=WINDOW_MS;
        if(changed||newActivity||returned){pkg=next;window=windowId;enteredAt=now;handled=false;return true;}
        if(windowId!=-1)window=windowId;
        return false;
    }
}
