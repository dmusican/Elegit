package elegit.sshauthentication;

import com.jcraft.jsch.UserInfo;
import org.apache.http.annotation.ThreadSafe;

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
        System.out.println(s);
        return true;
    }

    @Override
    public boolean promptPassphrase(String s) {
        System.out.println(s);
        return true;
    }

    @Override
    public boolean promptYesNo(String s) {
        System.out.println(s);

        // If the question is:
        /////////////////////////////////////////
        // The authenticity of host '...' can't be established.
        // RSA key fingerprint is ...
        // Are you sure you want to continue connecting?
        ////////////////////////////////////////
        // then for test purposes, the answer should be yes.

        if (s.startsWith("The authenticity of host")) {
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
