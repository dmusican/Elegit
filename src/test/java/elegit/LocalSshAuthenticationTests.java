package elegit;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import elegit.exceptions.CancelledAuthorizationException;
import elegit.exceptions.ExceptionAdapter;
import elegit.exceptions.MissingRepoException;
import elegit.models.AuthMethod;
import elegit.models.ClonedRepoHelper;
import elegit.models.ExistingRepoHelper;
import elegit.models.RepoHelper;
import elegit.sshauthentication.DetailedSshLogger;
import elegit.sshauthentication.ElegitUserInfoTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.common.keyprovider.MappedKeyPairProvider;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.git.pack.GitPackCommandFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.pubkey.KeySetPublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.TransportGitSsh;
import org.eclipse.jgit.transport.TransportProtocol;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.FS;
import org.junit.*;
import sharedrules.TestUtilities;
import sharedrules.TestingLogPathRule;
import sharedrules.TestingRemoteAndLocalReposRule;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class LocalSshAuthenticationTests {

    @ClassRule
    public static final TestingLogPathRule testingLogPath = new TestingLogPathRule();

    @BeforeClass
    public static void setUpClass() {
        // Uncomment for debugging purposes
//        TestingRemoteAndLocalReposRule.doNotDeleteTgoempFiles();
    }

    @Rule
    public final TestingRemoteAndLocalReposRule testingRemoteAndLocalRepos =
            new TestingRemoteAndLocalReposRule(false);
    private Path directoryPath;

    private static final Logger console = LogManager.getLogger("briefconsolelogger");

    private static final String testPassword = "a_test_password";

    // Used for each test; declared up here so that it can be stopped if need be in
    private SshServer sshd;

    @Before
    public void setUp()  {
        console.info("Unit test started");
        directoryPath = testingRemoteAndLocalRepos.getDirectoryPath();
        TestingRemoteAndLocalReposRule.doNotDeleteTempFiles();
        sshd = null;
    }

    @After
    public void tearDown() throws Exception {
        if (sshd != null) {
            sshd.stop();
        }
    }

    @Test
    public void testSshPrivateKey() throws Exception {
        // Uncomment this to get detail SSH logging info, for debugging
//        JSch.setLogger(new DetailedSshLogger());

        // Set up test SSH server.
        sshd = SshServer.setUpDefaultServer();

        String remoteURL = TestUtilities.setUpTestSshServer(sshd,
                                                            directoryPath,
                                                            testingRemoteAndLocalRepos.getRemoteFull(),
                                                            testingRemoteAndLocalRepos.getRemoteBrief());

        console.info("Connecting to " + remoteURL);
        Path local = testingRemoteAndLocalRepos.getLocalFull();
        InputStream passwordFileStream = TestUtilities.class.getResourceAsStream("/rsa_key1_passphrase.txt");
        Scanner scanner = new Scanner(passwordFileStream);
        String passphrase = scanner.next();
        console.info("phrase is " + passphrase);
        String privateKeyFileLocation = "/rsa_key1";


        ClonedRepoHelper helper =
                new ClonedRepoHelper(local, "",
                                     new ElegitUserInfoTest(null, passphrase),
                                     getClass().getResource(privateKeyFileLocation).getFile(),
                                     directoryPath.resolve("testing_known_hosts").toString());
        helper.obtainRepository(remoteURL);

        // Verify that it is an SSH connection, then try a fetch
        assertEquals(helper.getCompatibleAuthentication(), AuthMethod.SSH);
        helper.fetch(false);

        // Create a new test file at the local repo
        Path fileLocation = local.resolve("README.md");
        FileWriter fw = new FileWriter(fileLocation.toString(), true);
        fw.write("start");
        fw.close();

        // Commit, and push to remote
        helper.addFilePathTest(fileLocation);
        helper.commit("Appended to file");
        PushCommand command = helper.prepareToPushAll();
        helper.pushAll(command);

        // Other methods to test if authentication is succeeding
        helper.getRefsFromRemote(false);
    }


    @Test
    public void testSshPassword() throws Exception {

        sshd = SshServer.setUpDefaultServer();
        String remoteURL = TestUtilities.setUpTestSshServer(sshd,
                                                            directoryPath,
                                                            testingRemoteAndLocalRepos.getRemoteFull(),
                                                            testingRemoteAndLocalRepos.getRemoteBrief());

        console.info("Connecting to " + remoteURL);
        Path local = testingRemoteAndLocalRepos.getLocalFull();
        ClonedRepoHelper helper =
                new ClonedRepoHelper(local, testPassword,
                                     new ElegitUserInfoTest(null, null),
                                     null, null);
        helper.obtainRepository(remoteURL);

        assertEquals(helper.getCompatibleAuthentication(), AuthMethod.SSH);
        helper.fetch(false);
        PushCommand command = helper.prepareToPushAll();
        helper.pushAll(command);

    }

    @Test
    public void testLsSshPassword() throws Exception {
        sshd = SshServer.setUpDefaultServer();
        String remoteURL = TestUtilities.setUpTestSshServer(sshd,
                                                            directoryPath,
                                                            testingRemoteAndLocalRepos.getRemoteFull(),
                                                            testingRemoteAndLocalRepos.getRemoteBrief());
        TransportCommand command = Git.lsRemoteRepository().setRemote(remoteURL);
        RepoHelper helper = new RepoHelper(null, new ElegitUserInfoTest(testPassword, null));
        helper.wrapAuthentication(command);
        command.call();
    }


    @Test
    public void testTransportProtocols() throws Exception {
        List<TransportProtocol> protocols = TransportGitSsh.getTransportProtocols();
        for (TransportProtocol protocol : protocols) {
            System.out.println(protocol + " " + protocol.getName());
            for (String scheme : protocol.getSchemes()) {
                System.out.println("\t" + scheme);
            }
        }
        for (TransportProtocol protocol : protocols) {
            if (protocol.canHandle(new URIish("https://anything.com/repo.git"))) {
                assertEquals(protocol.getName(), "HTTP");
                assertNotEquals(protocol.getName(), "SSH");
            }

            if (protocol.canHandle(new URIish("ssh://anything.com/repo.git"))) {
                assertEquals(protocol.getName(), "SSH");
                assertNotEquals(protocol.getName(), "HTTP");
            }
        }
    }

    private int passphrasePromptCount;

    @Test
    public void testForRepeatAuthentication() throws Exception {
        //JSch.setLogger(new DetailedSshLogger());
        // Get local SSH server running
        sshd = SshServer.setUpDefaultServer();
        String remoteURL = TestUtilities.setUpTestSshServer(sshd,
                                                            directoryPath,
                                                            testingRemoteAndLocalRepos.getRemoteFull(),
                                                            testingRemoteAndLocalRepos.getRemoteBrief());

        // Get test authentication files
        InputStream passwordFileStream = TestUtilities.class.getResourceAsStream("/rsa_key1_passphrase.txt");
        Scanner scanner = new Scanner(passwordFileStream);
        String passphrase = scanner.next();
        String privateKeyFileLocation = "/rsa_key1";

        // Set up Git command
        String privateKeyLocationString = Paths.get(getClass().getResource(privateKeyFileLocation).toURI()).toFile().toString();
        SshSessionFactory sshSessionFactory = new TestSessionFactory(privateKeyLocationString, passphrase);
        TransportCommand command = Git.lsRemoteRepository().setRemote(remoteURL);
        command.setTransportConfigCallback(
                transport -> {
                    SshTransport sshTransport = (SshTransport) transport;
                    sshTransport.setSshSessionFactory(sshSessionFactory);
                });

        // Set up mock .ssh/config file
        System.setProperty("user.home",directoryPath.resolve("home").toString());
        Path sshDir = directoryPath.resolve("home").resolve(".ssh");
        try {
            Files.createDirectories(sshDir);

            FileWriter fw = new FileWriter(sshDir.resolve("config").toString(), true);
            fw.write("Host localhost\n");
            fw.write("  HostName localhost\n");
            fw.close();
        } catch (IOException e) {
            throw new ExceptionAdapter(e);
        }

        // Verify that calling the command twice still only checks ssh passphrase once, which was a bug that used
        // to happen
        passphrasePromptCount = 0;
        System.out.println(command.call());
        System.out.println(command.call());
        assertEquals(1, passphrasePromptCount);

    }

    private class TestSessionFactory extends JschConfigSessionFactory {

        private String passphrase;
        private String privateKeyLocationString;

        public TestSessionFactory(String privateKeyLocationString, String passphrase) {
            this.passphrase = passphrase;
            this.privateKeyLocationString = privateKeyLocationString;
        }

        @Override
        protected JSch createDefaultJSch( FS fs ) throws JSchException {
            JSch defaultJSch = super.createDefaultJSch( fs );
            defaultJSch.addIdentity(privateKeyLocationString);
            return defaultJSch;
        }

        @Override
        protected void configure(OpenSshConfig.Host hc, Session session) {
            session.setUserInfo(new UserInfo() {
                @Override
                public String getPassphrase() {
                    console.info("Getting passphrase");
                    passphrasePromptCount++;
                    return passphrase;
                }

                @Override
                public String getPassword() {
                    return null;
                }

                @Override
                public boolean promptPassword(String message) {
                    return true;
                }

                @Override
                public boolean promptPassphrase(String message) {
                    console.info("Prompting passphrase");
                    return true;
                }

                @Override
                public boolean promptYesNo(String message) {
                    return true;
                }

                @Override
                public void showMessage(String message) {
                }
            });
        }
    }



}
