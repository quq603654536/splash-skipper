package com.wkm.splashskip.fixture;
import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.TextView;
public final class MainActivity extends Activity {
    private String scenario;
    private FrameLayout root;
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    @Override public void onCreate(Bundle b){
        super.onCreate(b);getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        scenario=getIntent().getStringExtra("case");if(scenario==null)scenario="countdown";
        root=new FrameLayout(this);root.setBackgroundColor(Color.rgb(235,241,237));setContentView(root);
        TextView title=new TextView(this);title.setGravity(Gravity.CENTER);title.setTextSize(25);title.setTextColor(Color.rgb(20,60,45));
        String center=scenario.equals("onboarding")?"欢迎使用\n首次使用引导":scenario.equals("privacy")?"隐私政策\n此页面应保持不动":"模拟广告\n仅用于本地测试";
        title.setText(center);root.addView(title,new FrameLayout.LayoutParams(-1,-1));
        TextView skip=new TextView(this);skip.setText(scenario.equals("plainAd")||scenario.equals("onboarding")?"跳过":"跳过 5s");
        skip.setTextSize(15);skip.setTextColor(Color.WHITE);skip.setBackgroundColor(Color.rgb(45,70,60));skip.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(dp(105),dp(44),Gravity.TOP|Gravity.RIGHT);p.topMargin=dp(65);p.rightMargin=dp(18);root.addView(skip,p);
        if(scenario.equals("gesture")){
            skip.setClickable(false);skip.setOnTouchListener((v,e)->{if(e.getAction()==MotionEvent.ACTION_UP)passed();return true;});
        }else skip.setOnClickListener(v->passed());
        Log.i("SplashSkipFixture","READY "+scenario);
    }
    private void passed(){
        root.removeAllViews();TextView t=new TextView(this);t.setText("已进入测试首页\nPASS: "+scenario);t.setTextSize(25);t.setTextColor(Color.rgb(13,119,98));t.setGravity(Gravity.CENTER);root.addView(t,new FrameLayout.LayoutParams(-1,-1));
        Log.i("SplashSkipFixture","CLICKED "+scenario);
    }
}
