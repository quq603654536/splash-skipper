package com.wkm.splashskip;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.Set;

public final class Store {
    private final Context context;
    private final SharedPreferences preferences;
    public Store(Context c) { context=c.getApplicationContext(); preferences=context.getSharedPreferences("settings",0); }
    public boolean enabled() { return preferences.getBoolean("enabled",true); }
    public void enabled(boolean v) { preferences.edit().putBoolean("enabled",v).apply(); }
    public boolean enhanced() { return preferences.getBoolean("enhanced",false); }
    public void enhanced(boolean v) { preferences.edit().putBoolean("enhanced",v).apply(); }
    public boolean gestures() { return preferences.getBoolean("gestures",true); }
    public void gestures(boolean v) { preferences.edit().putBoolean("gestures",v).apply(); }
    public long count() { return preferences.getLong("count",0); }
    public Set<String> excluded() { return new HashSet<>(preferences.getStringSet("excluded",new HashSet<String>())); }
    public boolean allowed(String pkg) { return !excluded().contains(pkg); }
    public void allowed(String pkg, boolean v) {
        Set<String> s=excluded(); if(v)s.remove(pkg);else s.add(pkg);
        preferences.edit().putStringSet("excluded",s).apply();
    }
    public JSONArray records() {
        try { return new JSONArray(preferences.getString("records","[]")); }
        catch(Exception e) { return new JSONArray(); }
    }
    public void record(String pkg, String route) {
        try {
            String name=pkg;
            try { name=context.getPackageManager().getApplicationLabel(context.getPackageManager().getApplicationInfo(pkg,0)).toString(); }
            catch(Exception ignored) {}
            JSONObject row=new JSONObject(); row.put("package",pkg); row.put("name",name);
            row.put("time",System.currentTimeMillis()); row.put("route",route);
            JSONArray old=records(), next=new JSONArray(); next.put(row);
            for(int i=0;i<Math.min(old.length(),99);i++)next.put(old.get(i));
            preferences.edit().putLong("count",count()+1).putString("records",next.toString()).apply();
        } catch(Exception ignored) {}
    }
    public void clearRecords() { preferences.edit().putString("records","[]").putLong("count",0).apply(); }
}
