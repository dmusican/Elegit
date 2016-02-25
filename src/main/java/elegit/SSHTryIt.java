package main.java.elegit;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.util.FS;

import java.io.File;

/**
 * Created by dmusican on 1/31/16.
 */
public class SSHTryIt {
    public static void main(String[] args) throws GitAPIException {
        System.out.println("hey");
        CloneCommand cloneCommand = Git.cloneRepository();
        String remoteURL= "ssh://.....";
        cloneCommand.setURI(remoteURL);
        // Explained http://www.codeaffine.com/2014/12/09/jgit-authentication/
        SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
            @Override
            protected void configure(OpenSshConfig.Host host, Session session) {
                // do nothing
            }

            @Override
            protected JSch createDefaultJSch(FS fs) throws JSchException {
                JSch defaultJSch = super.createDefaultJSch(fs);
                defaultJSch.addIdentity("/Users/...../.ssh/...",
                                        "my password");
                return defaultJSch;
            }
        };
        cloneCommand.setTransportConfigCallback(
                new TransportConfigCallback() {
                    @Override
                    public void configure(Transport transport) {
                        SshTransport sshTransport = (SshTransport) transport;
                        sshTransport.setSshSessionFactory(sshSessionFactory);
                    }
                });

        cloneCommand.setDirectory(new File("/Users/...../temp/....."));
        Git cloneCall = cloneCommand.call();

        cloneCall.close();
        Repository repo = cloneCall.getRepository();
        LsRemoteCommand lsRemoteCommand = new LsRemoteCommand(repo);

        lsRemoteCommand.setTransportConfigCallback(
                new TransportConfigCallback() {
                    @Override
                    public void configure(Transport transport) {
                        SshTransport sshTransport = (SshTransport) transport;
                        sshTransport.setSshSessionFactory(sshSessionFactory);
                    }
                });

        lsRemoteCommand.setRemote(remoteURL);
        lsRemoteCommand.call();

    }
}
