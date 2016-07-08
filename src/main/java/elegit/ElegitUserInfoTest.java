package elegit;

import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;
import com.sun.tools.doclets.formats.html.SourceToHTMLConverter;
import javafx.application.Platform;
import javafx.scene.control.TextInputDialog;

import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.FutureTask;

/**
 * Class for purposes for JSch authentication (which JGit uses). This is the text-based version used
 * for unit tests.
 */
public class ElegitUserInfoTest implements UserInfo, UIKeyboardInteractive {

    private String password;
    private String passphrase;

    public ElegitUserInfoTest(String password, String passphrase) {
        this.password = password;
        this.passphrase = passphrase;
    }

    @Override
    public String[] promptKeyboardInteractive(String destination, String name, String introduction,
                                              String[] prompt, boolean[] echo) {
        System.out.println("Destination: " + destination);
        System.out.println("Name: " + name);
        System.out.println("Introduction" + introduction);
        Scanner scanner = new Scanner(System.in);
        String[] results = new String[prompt.length];
        for (int i=0; i < prompt.length; i++) {
            System.out.print(i + ". " + prompt[i] + "(" + echo[i] + "): ");
            results[i] = scanner.nextLine().trim();
        }
        return results;
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
        System.out.println("Asking for password.");
        return true;
    }

    @Override
    public boolean promptPassphrase(String s) {
        System.out.println("Kindly enter your passphrase: ");
        Scanner scanner = new Scanner(System.in);
        passphrase = scanner.nextLine().trim();
        return passphrase.length() > 0;
//
//        final boolean response;
//        FutureTask<Boolean> task = new FutureTask<Boolean>(new Runnable() {
//            @Override
//            public void run() {
//                TextInputDialog prompt = new TextInputDialog();
//                prompt.setTitle("SSH public key authentication");
//                prompt.setHeaderText("SSH public key authentication");
//                prompt.setContentText("Enter your passphrase:");
//                Optional<String> result = prompt.showAndWait();
//                if (result.isPresent()) {
//                    passphrase = result.get();
//                    response = true;
//                } else {
//                    passphrase = "";
//                    response = false;
//                }
//            }
//        }}, response};
    }

    @Override
    public boolean promptYesNo(String s) {
        System.out.print("(y)es or (n)o? ");
        Scanner scanner = new Scanner(System.in);
        String answer = scanner.next();
        return answer.equals("y");
    }

    @Override
    public void showMessage(String s) {
        System.out.println(s);
    }
}
