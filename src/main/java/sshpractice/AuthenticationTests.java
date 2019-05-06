package sshpractice;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.TransportGitSsh;
import org.eclipse.jgit.util.FS;

import java.util.Scanner;

public class AuthenticationTests {

    private static SshSessionFactory setupSshSessionFactory() {
        return new JschConfigSessionFactory() {
            private String passphrase;
            private String password;
            @Override
            protected void configure(OpenSshConfig.Host host, Session session) {
                session.setUserInfo(new UserInfo() {
                    @Override
                    public String getPassphrase() {
                        return passphrase;
                    }

                    @Override
                    public String getPassword() {
                        return password;
                    }

                    @Override
                    public boolean promptPassword(String message) {
                        System.out.println(message);
                        System.out.print("Enter password: ");
                        Scanner scanner = new Scanner(System.in);
                        password = scanner.next();
                        return true;
                    }

                    @Override
                    public boolean promptPassphrase(String message) {
                        System.out.println(message);
                        System.out.print("Enter passphrase: ");
                        Scanner scanner = new Scanner(System.in);
                        passphrase = scanner.next();
                        return true;
                    }

                    @Override
                    public boolean promptYesNo(String message) {
                        return false;
                    }

                    @Override
                    public void showMessage(String message) {
                        System.out.println(message);
                    }
                });
            }};
    }

    public static void main(String[] args) throws Exception {

        TransportCommand command = Git.lsRemoteRepository()
                .setRemote("ssh://.........");

        SshSessionFactory sshSessionFactory= setupSshSessionFactory();

        command.setTransportConfigCallback(
                transport -> {
                    SshTransport sshTransport = (SshTransport) transport;
                    sshTransport.setSshSessionFactory(sshSessionFactory);
                });

        System.out.println(command.call());
        System.out.println(command.call());

    }
}
