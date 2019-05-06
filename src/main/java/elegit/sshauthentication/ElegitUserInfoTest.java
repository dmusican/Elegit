package elegit.sshauthentication;

import com.jcraft.jsch.UserInfo;
import net.jcip.annotations.ThreadSafe;

/**
 * Class for purposes for JSch authentication (which JGit uses). This is the text-based version used
 * for unit tests.
 */
@ThreadSafe
public class ElegitUserInfoTest implements UserInfo {

    private final String password;
    private final String passphrase;

    public ElegitUserInfoTest() {
        password = null;
        passphrase = null;
    }

    public ElegitUserInfoTest(String password, String passphrase) {
        this.password = password;
        this.passphrase = passphrase;
    }

    @Override
    public String getPassphrase() {
        return passphrase;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean promptPassword(String s) {
        System.out.println("Prompting password...\n " + s);
        return true;
    }

    @Override
    public boolean promptPassphrase(String s) {
        System.out.println("Prompting passphrase...\n" + s);
        return true;
    }

    @Override
    public boolean promptYesNo(String s) {

        // If the question is:
        /////////////////////////////////////////
        // The authenticity of host '...' can't be established.
        // RSA key fingerprint is ...
        // Are you sure you want to continue connecting?
        ////////////////////////////////////////
        // then for test purposes, the answer should be yes.
        // Likewise, for a warning that REMOTE IDENTIFICATION HAS CHANGED,
        // this seems unavoidable when password testing. So at least form a unit testing perspective,
        // we'll consider it acceptable.

        if (s.startsWith("The authenticity of host")) {
            return true;
        } else if (s.startsWith("WARNING: REMOTE HOST IDENTIFICATION HAS CHANGED!")) {
            return true;
        } else {
            throw new RuntimeException("promptYesNo case not handled.");
        }
    }

    @Override
    public void showMessage(String s) {
        System.out.println(s);
    }
}
