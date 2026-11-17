import com.wkm.splashskip.SkipMatcher;
public class MatcherTest {
    static int checks;
    static void expect(boolean wanted,String label,String id,int x,int y,int w,int h,boolean ad,boolean sensitive,boolean enhanced){
        boolean actual=SkipMatcher.score(label,id,x,y,w,h,1080,2400,3,ad,sensitive,enhanced)>0;
        checks++;if(wanted!=actual)throw new AssertionError(label+" expected "+wanted+" got "+actual);
    }
    public static void main(String[] args){
        expect(true,"跳过 5s","",830,110,170,85,false,false,false);
        expect(true,"跳过 | 5s","",830,110,170,85,false,false,false);
        expect(true,"Skip Ad button","",830,110,170,85,false,false,false);
        expect(true,"跳过广告按钮","",830,110,170,85,false,false,false);
        expect(true,"5秒 跳过","",830,110,170,85,false,false,false);
        expect(true,"跳过广告","",830,110,170,85,false,false,false);
        expect(true,"Skip Ad","",830,110,170,85,false,false,false);
        expect(true,"跳過廣告","",830,110,170,85,false,false,false);
        expect(true,"跳过(3)","",830,110,170,85,false,false,false);
        expect(true,"跳过","vendor:id/splash_skip",830,110,170,85,false,false,false);
        expect(true,"跳过","",830,110,170,85,true,false,false);
        expect(false,"跳过","",830,110,170,85,false,false,false);
        expect(true,"跳过","",830,110,170,85,false,false,true);
        expect(false,"跳过5s","",830,110,170,85,true,true,true);
        expect(false,"跳过5s","",20,110,170,85,true,false,false);
        expect(false,"跳过5s","",830,1500,170,85,true,false,false);
        expect(false,"点击跳过广告领取奖励","",830,110,170,85,true,false,true);
        expect(false,"同意并继续","",830,110,170,85,true,false,true);
        expect(false,"关闭","",830,110,170,85,true,false,true);
        expect(false,"跳过5s","",600,110,460,500,true,false,true);
        expect(false,"跳过5s","",1000,110,170,85,true,false,true);
        expect(false,"","vendor:id/splash_skip",830,110,170,85,true,false,true);
        if(!SkipMatcher.sensitive("请阅读隐私政策")||!SkipMatcher.sensitive("输入验证码")||!SkipMatcher.sensitive("Privacy policy"))throw new AssertionError("sensitive detection");
        if(!SkipMatcher.adMarker("广告")||SkipMatcher.adMarker("普通首页"))throw new AssertionError("ad marker");
        System.out.println("PASS: "+checks+" matching cases plus sensitive/ad marker checks");
    }
}
