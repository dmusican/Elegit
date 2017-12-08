package elegit;

import elegit.exceptions.CancelledAuthorizationException;
import elegit.exceptions.MissingRepoException;
import elegit.models.AuthMethod;
import elegit.models.ClonedRepoHelper;
import elegit.models.ExistingRepoHelper;
import elegit.models.RepoHelper;
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
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class LocalSshAuthenticationTests {

    @ClassRule
    public static final TestingLogPath testingLogPath = new TestingLogPath();

    @Rule
    public final TestingRemoteAndLocalRepos testingRemoteAndLocalRepos =
            new TestingRemoteAndLocalRepos(false);
    private Path directoryPath;

    private static final Logger console = LogManager.getLogger("briefconsolelogger");

    private static final String testPassword = "a_test_password";


    @Before
    public void setUp() throws Exception {
        console.info("Unit test started");
        directoryPath = testingRemoteAndLocalRepos.getDirectoryPath();
    }

    // http://www.jcraft.com/jsch/examples/Logger.java.html
    public static class MyLogger implements com.jcraft.jsch.Logger {
        static java.util.Hashtable<Integer,String> name=new java.util.Hashtable<>();
        static{
            name.put(DEBUG, "DEBUG: ");
            name.put(INFO, "INFO: ");
            name.put(WARN, "WARN: ");
            name.put(ERROR, "ERROR: ");
            name.put(FATAL, "FATAL: ");
        }
        public boolean isEnabled(int level){
            return true;
        }
        public void log(int level, String message){
            System.err.print(name.get(level));
            System.err.println(message);
        }
    }


    @Test
    public void testSshPrivateKey() throws Exception {
        // Uncomment this to get detail SSH logging info, for debugging
        //JSch.setLogger(new AuthenticatedCloneTest.MyLogger());

        // Set up test SSH server.
        SshServer sshd = SshServer.setUpDefaultServer();

        // Provide SSH server with public and private key info that client will be connecting with
        InputStream passwordFileStream = getClass().getResourceAsStream("/rsa_key1_passphrase.txt");
        Scanner scanner = new Scanner(passwordFileStream);
        String passphrase = scanner.next();
        console.info("phrase is " + passphrase);

        String privateKeyFileLocation = "/rsa_key1";
        InputStream privateKeyStream = getClass().getResourceAsStream(privateKeyFileLocation);
        FilePasswordProvider filePasswordProvider = FilePasswordProvider.of(passphrase);
        KeyPair kp = SecurityUtils.loadKeyPairIdentity("testkey", privateKeyStream, filePasswordProvider);
        ArrayList<KeyPair> pairs = new ArrayList<>();
        pairs.add(kp);
        KeyPairProvider hostKeyProvider = new MappedKeyPairProvider(pairs);
        sshd.setKeyPairProvider(hostKeyProvider);

        // Need to use a non-standard port, as there may be an ssh server already running on this machine
        sshd.setPort(2222);

        // Set up a fall-back password authenticator to help in diagnosing failed test
        sshd.setPasswordAuthenticator(new PasswordAuthenticator() {
            public boolean authenticate(String username, String password, ServerSession session) {
                fail("Tried to use password instead of public key authentication");
                return false;
            }
        });

        // This replaces the role of authorized_keys.
        Collection<PublicKey> allowedKeys = new ArrayList<>();
        allowedKeys.add(kp.getPublic());
        sshd.setPublickeyAuthenticator(new KeySetPublickeyAuthenticator(allowedKeys));

        // Amazingly useful Git command setup provided by Mina.
        sshd.setCommandFactory(new GitPackCommandFactory(directoryPath.toString()));

        // Start the SSH test server.
        sshd.start();

        // Create temporary known_hosts file.
        Path knownHostsFileLocation = directoryPath.resolve("testing_known_hosts");
        Files.createFile(knownHostsFileLocation);

        // Clone the bare repo, using the SSH connection, to the local.
        String remoteURL = "ssh://localhost:2222/"+testingRemoteAndLocalRepos.getRemoteBrief();
        console.info("Connecting to " + remoteURL);
        Path local = testingRemoteAndLocalRepos.getLocalFull();
        ClonedRepoHelper helper =
                new ClonedRepoHelper(local, remoteURL, passphrase,
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

        // Shut down test SSH server
        sshd.stop();
    }

    @Test
    public void testSshPassword() throws Exception {

        String remoteURL = setUpTestSshServer();

        console.info("Connecting to " + remoteURL);
        Path local = testingRemoteAndLocalRepos.getLocalFull();
        ClonedRepoHelper helper =
                new ClonedRepoHelper(local, remoteURL,testPassword,
                                     new ElegitUserInfoTest(null, null));
        helper.obtainRepository(remoteURL);

        assertEquals(helper.getCompatibleAuthentication(), AuthMethod.SSH);
        helper.fetch(false);
        PushCommand command = helper.prepareToPushAll();
        helper.pushAll(command);
    }

    @Test
    public void testLsSshPassword() throws Exception {
        String remoteURL = setUpTestSshServer();
        TransportCommand command = Git.lsRemoteRepository().setRemote(remoteURL);
        RepoHelper helper = new RepoHelper(new ElegitUserInfoTest(testPassword, null));
        helper.wrapAuthentication(command);
        command.call();
    }



    private String setUpTestSshServer() throws IOException, GitAPIException, CancelledAuthorizationException, MissingRepoException, GeneralSecurityException {
        Path local = testingRemoteAndLocalRepos.getLocalFull();
        Path remote = testingRemoteAndLocalRepos.getRemoteFull();

        // Set up remote repo
        Path remoteFilePath = remote.resolve("file.txt");
        Files.write(remoteFilePath, "testSshPassword".getBytes());
        ArrayList<Path> paths = new ArrayList<>();
        paths.add(remoteFilePath);
        ExistingRepoHelper helperServer = new ExistingRepoHelper(remote, null);
        helperServer.addFilePathsTest(paths);
        helperServer.commit("Initial unit test commit");

        // Set up test SSH server.
        SshServer sshd = SshServer.setUpDefaultServer();

        // All of this key set up is gratuitous, but it's the only way that I was able to get sshd to start up.
        // In the end, it is ignore, and the password is used.

        // Provide SSH server with public and private key info that client will be connecting with
        InputStream passwordFileStream = getClass().getResourceAsStream("/rsa_key1_passphrase.txt");
        Scanner scanner = new Scanner(passwordFileStream);
        String passphrase = scanner.next();
        console.info("phrase is " + passphrase);

        String privateKeyFileLocation = "/rsa_key1";
        InputStream privateKeyStream = getClass().getResourceAsStream(privateKeyFileLocation);
        FilePasswordProvider filePasswordProvider = FilePasswordProvider.of(passphrase);
        KeyPair kp = SecurityUtils.loadKeyPairIdentity("testkey", privateKeyStream, filePasswordProvider);
        ArrayList<KeyPair> pairs = new ArrayList<>();
        pairs.add(kp);
        KeyPairProvider hostKeyProvider = new MappedKeyPairProvider(pairs);
        sshd.setKeyPairProvider(hostKeyProvider);

        // Need to use a non-standard port, as there may be an ssh server already running on this machine
        sshd.setPort(2222);

        // Set up a fall-back password authenticator to help in diagnosing failed test
        sshd.setPasswordAuthenticator(
                (username, password, session) -> {
                    if (password.equals(testPassword)) {
                        return true;
                    } else {
                        fail("Tried to use password instead of public key authentication");
                        return false;
                    }
                });


        // This replaces the role of authorized_keys.
        Collection<PublicKey> allowedKeys = new ArrayList<>();
        allowedKeys.add(kp.getPublic());
        sshd.setPublickeyAuthenticator(new KeySetPublickeyAuthenticator(allowedKeys));

        // Amazingly useful Git command setup provided by Mina.
        sshd.setCommandFactory(new GitPackCommandFactory(directoryPath.toString()));

        // Start the SSH test server.
        sshd.start();

        // Create temporary known_hosts file.
        Path knownHostsFileLocation = directoryPath.resolve("testing_known_hosts");
        Files.createFile(knownHostsFileLocation);

        // Clone the bare repo, using the SSH connection, to the local.
        return "ssh://localhost:2222/"+testingRemoteAndLocalRepos.getRemoteBrief();
    }



}
