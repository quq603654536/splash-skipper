import com.wkm.splashskip.LaunchSession;
public class SessionTest {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args){
        LaunchSession s=new LaunchSession();
        check(s.observe("app.one",10,true,1000),"first foreground starts");
        s.handled(true);
        check(!s.observe("app.one",10,true,2000),"duplicate state does not reset a click");
        check(s.handled(),"handled preserved");
        check(s.observe("launcher",11,false,3000),"window-only home transition is observed");
        check(s.observe("app.one",10,true,4000),"return to same app re-arms");
        check(s.active(4100),"return has active scan window");
        check(!s.observe("app.one",10,false,7000),"content updates do not extend window");
        check(!s.active(17000),"expired normally");
        check(s.observe("app.one",10,true,18000),"resume re-arms even if launcher event was missing");
        check(s.active(18100),"same package active again");
        check(s.observe("app.one",12,true,19000),"new activity window re-arms");
        check(!s.observe("app.one",12,false,20000),"window observer does not continually restart scan");
        check(!s.observe(null,12,true,21000),"null package ignored");
        System.out.println("PASS: foreground/resume session regressions");
    }
}
