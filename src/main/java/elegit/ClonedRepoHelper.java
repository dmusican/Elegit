package main.java.elegit;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import main.java.elegit.exceptions.CancelledAuthorizationException;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.util.FS;
import java.io.IOException;
import java.nio.file.Path;

/**
 * A RepoHelper implementation for a repository cloned into an empty folder.
 */
public class ClonedRepoHelper extends RepoHelper {
    public ClonedRepoHelper(Path directoryPath, String remoteURL) throws IOException, GitAPIException, CancelledAuthorizationException{
        super(directoryPath, remoteURL);
    }

    public ClonedRepoHelper(Path directoryPath, String remoteURL, UsernamePasswordCredentialsProvider ownerAuth)
            throws GitAPIException, IOException, CancelledAuthorizationException {
        super(directoryPath, remoteURL, ownerAuth);
    }

    /**
     * Clones the repository into the desired folder and returns
     * the JGit Repository object.
     *
     * @return the RepoHelper's associated Repository object.
     * @throws GitAPIException if the `git clone` call fails.
     */
    @Override
    protected Repository obtainRepository() throws GitAPIException, CancelledAuthorizationException {
        CloneCommand cloneCommand = Git.cloneRepository();
        cloneCommand.setURI(this.remoteURL);
        wrapAuthentication(cloneCommand);
        cloneCommand.setDirectory(this.localPath.toFile());
        Git cloneCall = cloneCommand.call();

        cloneCall.close();
        return cloneCall.getRepository();
    }

    private void wrapAuthentication(CloneCommand cloneCommand) {
        if (this.remoteURL.startsWith("https://") ||
            this.remoteURL.startsWith("http://")) {

            cloneCommand.setCredentialsProvider(super.ownerAuth);
        } else if (this.remoteURL.substring(0,6).equals("ssh://")) {
            // Explained http://www.codeaffine.com/2014/12/09/jgit-authentication/
            SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
                @Override
                protected void configure(OpenSshConfig.Host host, Session session ) {
                    // do nothing
                }
                @Override
                protected JSch createDefaultJSch(FS fs ) throws JSchException {
                    JSch defaultJSch = super.createDefaultJSch( fs );
                    defaultJSch.addIdentity( "/Users/dmusican/.ssh/mathcs",
                                             "my password");
                    return defaultJSch;
                }
            };
            cloneCommand.setTransportConfigCallback(
                    new TransportConfigCallback() {
                        @Override
                        public void configure( Transport transport ) {
                            SshTransport sshTransport = (SshTransport)transport;
                            sshTransport.setSshSessionFactory( sshSessionFactory );
                        }
                     });
        }
    }
}
