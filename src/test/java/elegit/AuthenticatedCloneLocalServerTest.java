package elegit;

import elegit.exceptions.ExceptionAdapter;
import elegit.models.AuthMethod;
import elegit.models.ClonedRepoHelper;
import elegit.models.ExistingRepoHelper;
import elegit.models.LocalBranchHelper;
import elegit.sshauthentication.ElegitUserInfoTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;

import static org.junit.Assert.assertEquals;

public class AuthenticatedCloneLocalServerTest {

    private static Path logPath;

    static {
        try {
            logPath = Files.createTempDirectory("elegitLogs");
        } catch (IOException e) {
            throw new ExceptionAdapter(e);
        }

        logPath.toFile().deleteOnExit();

        System.setProperty("logFolder", logPath.toString());
    }

    private static final Logger console = LogManager.getLogger("briefconsolelogger");

    private Path directoryPath;

    @Before
    public void setUp() throws Exception {
        console.info("Unit test started");

        directoryPath = Files.createTempDirectory("unitTestRepos");
        directoryPath.toFile().deleteOnExit();
    }

    @After
    public void tearDown() throws Exception {
        removeAllFilesFromDirectory(this.logPath.toFile());
        removeAllFilesFromDirectory(directoryPath.toFile());
    }

    // Helper tear-down method:
    void removeAllFilesFromDirectory(File dir) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) removeAllFilesFromDirectory(file);
            file.delete();
        }
    }

    @Test
    public void testSshPrivateKey() throws Exception {

        SshServer sshd = SshServer.setUpDefaultServer();

        // Need to use a non-standard port, as there may be an ssh server already running on this machine
        sshd.setPort(2222);
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File("hostkey.ser")));

        sshd.setPublickeyAuthenticator(new PublickeyAuthenticator() {
            public boolean authenticate(String username, PublicKey key, ServerSession session) {
                return true;
            }
        });

        sshd.start();


        Path remote = directoryPath.resolve("remote");
        Path local = directoryPath.resolve("local");

        console.info("Remote path = " + remote);
        console.info("Local path = " + local);

        Git remoteHandle = Git.init().setDirectory(remote.toFile()).setBare(true).call();

        String remoteURL = "ssh://localhost:2222"+remote;
        String passphrase = "thepassphrase";
        ClonedRepoHelper helper = new ClonedRepoHelper(local, remoteURL, passphrase,
                                                       new ElegitUserInfoTest(null, passphrase));
        helper.obtainRepository(remoteURL);
        assertEquals(helper.getCompatibleAuthentication(), AuthMethod.SSH);
        helper.fetch(false);

        Path fileLocation = local.resolve("README.md");

        FileWriter fw = new FileWriter(fileLocation.toString(), true);
        fw.write("start");
        fw.close();
        helper.addFilePathTest(fileLocation);
        helper.commit("Appended to file");


        PushCommand command = helper.prepareToPushAll();
        helper.pushAll(command);


    }
}
