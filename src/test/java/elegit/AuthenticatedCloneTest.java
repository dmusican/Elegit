package elegit;

import com.jcraft.jsch.*;
import elegit.gui.ClonedRepoHelperBuilder;
import elegit.gui.RepoHelperBuilder;
import elegit.models.AuthMethod;
import elegit.models.ClonedRepoHelper;
import elegit.models.RepoHelper;
import elegit.sshauthentication.ElegitUserInfoTest;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.transport.*;
import org.junit.*;
import org.junit.rules.ExpectedException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

import static org.junit.Assert.*;

public class AuthenticatedCloneTest {

    private Path directoryPath;
    private String testFileLocation;
    Path logPath;

    // Used to indicate that if password files are missing, then tests should just pass
    private boolean looseTesting;

    private static final String GITHUB_REMOTE_URL = "https://github.com/TheElegitTeam/testrepo.git";
    private static final String BITBUCKET_REMOTE_URL = "https://musicant@bitbucket.org/musicant/bbtestrepo.git";

    @Before
    public void setUp() throws Exception {
        initializeLogger();
        this.directoryPath = Files.createTempDirectory("unitTestRepos");
        directoryPath.toFile().deleteOnExit();
        testFileLocation = System.getProperty("user.home") + File.separator +
                           "elegitTests" + File.separator;
        File strictTestingFile = new File(testFileLocation + "strictAuthenticationTesting.txt");
        looseTesting = !strictTestingFile.exists();
    }

    @After
    public void tearDown() throws Exception {
        removeAllFilesFromDirectory(this.logPath.toFile());
    }

    // Helper method to avoid annoying traces from logger
    void initializeLogger() {
        // Create a temp directory for the files to be placed in
        try {
            this.logPath = Files.createTempDirectory("elegitLogs");
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.logPath.toFile().deleteOnExit();
        System.setProperty("logFolder", logPath.toString());
    }

    // Helper tear-down method:
    void removeAllFilesFromDirectory(File dir) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) removeAllFilesFromDirectory(file);
            file.delete();
        }
    }

    @Test
    public void testCloneHttpNoPassword() throws Exception {
        Path repoPath = directoryPath.resolve("testrepo");
        // Clone from dummy repo:
        String remoteURL = "https://github.com/TheElegitTeam/TestRepository.git";

        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider("", "");
        ClonedRepoHelper helper = new ClonedRepoHelper(repoPath, remoteURL, credentials);
        assertNotNull(helper);
        helper.obtainRepository(remoteURL);

    }

    @Test
    public void testLsHttpNoPassword() throws Exception {
        testLsHttpUsernamePassword("httpNoUsernamePassword.txt");
    }

    @Test
    public void testHttpUsernamePasswordPublic() throws Exception {
        testHttpUsernamePassword("httpUsernamePassword.txt", GITHUB_REMOTE_URL);
    }

    @Test
    public void testHttpUsernamePasswordPrivate() throws Exception {
        testHttpUsernamePassword("httpUsernamePasswordPrivate.txt", BITBUCKET_REMOTE_URL);
    }

    /* The httpUsernamePassword should contain three lines, containing:
        repo http(s) address
        username
        password
     */
    public void testHttpUsernamePassword(String filename, String remoteURL) throws Exception {
        Path repoPath = directoryPath.resolve("testrepo");
        File authData = new File(testFileLocation + filename);

        // If a developer does not have this file present, test should just pass.
        if (!authData.exists() && looseTesting)
            return;

        Scanner scanner = new Scanner(authData);
        String ignoreURL = scanner.next();
        String username = scanner.next();
        String password = scanner.next();
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(username, password);
        try {
            ClonedRepoHelper helper = new ClonedRepoHelper(repoPath, remoteURL, credentials);
            helper.obtainRepository(remoteURL);
            assertEquals(helper.getCompatibleAuthentication(), AuthMethod.HTTP);
            helper.fetch(false);
            Path fileLocation = repoPath.resolve("README.md");
            System.out.println(fileLocation);
            FileWriter fw = new FileWriter(fileLocation.toString(), true);
            fw.write("1");
            fw.close();
            helper.addFilePathTest(fileLocation);
            helper.commit("Appended to file");
            PushCommand command = helper.prepareToPushAll();
            helper.pushAll(command);
        } catch (TransportException e) {
            e.printStackTrace();
            fail("Test failed; it is likely that you have not name/password correctly in the file " +
                 "or you do not have access to the Bitbucket repo. Note that httpUsernamePassword.txt " +
                 "should have GitHub authentication info; httpUsernamePasswordPrivate.txt should have" +
                 "Bitbucket authentication info.");
        }
    }


    @Test
    public void testHttpBadUsernamePasswordPublic() throws Exception {
        testHttpBadUsernamePassword("httpUsernamePassword.txt", GITHUB_REMOTE_URL);
    }


    /* The httpUsernamePassword should contain three lines, containing:
        repo http(s) address
        username
        password
        -------
        This is a version of the test where the username password is entered incorrectly at first,
        and needs to be fixed later.
     */
    public void testHttpBadUsernamePassword(String filename, String remoteURL) throws Exception {
        Path repoPath = directoryPath.resolve("testrepo");
        File authData = new File(testFileLocation + filename);

        // If a developer does not have this file present, test should just pass.
        if (!authData.exists() && looseTesting)
            return;

        Scanner scanner = new Scanner(authData);
        String ignoreURL = scanner.next();
        String username = scanner.next();
        String password = scanner.next();
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider("", "");
        try {
            ClonedRepoHelper helper = new ClonedRepoHelper(repoPath, remoteURL, credentials);
            helper.obtainRepository(remoteURL);
            assertEquals(helper.getCompatibleAuthentication(), AuthMethod.HTTP);
            helper.fetch(false);
            Path fileLocation = repoPath.resolve("README.md");
            System.out.println(fileLocation);
            FileWriter fw = new FileWriter(fileLocation.toString(), true);
            fw.write("1");
            fw.close();
            helper.addFilePathTest(fileLocation);
            helper.commit("Appended to file");
            credentials = new UsernamePasswordCredentialsProvider(username, password);
            helper.setOwnerAuth(credentials);
            PushCommand command = helper.prepareToPushAll();
            helper.pushAll(command);
            helper.pushTags();
        } catch (TransportException e) {
            e.printStackTrace();
            fail("Test failed; it is likely that you have not name/password correctly in the file " +
                 "or you do not have access to the Bitbucket repo. Note that httpUsernamePassword.txt " +
                 "should have GitHub authentication info; httpUsernamePasswordPrivate.txt should have" +
                 "Bitbucket authentication info.");
        }
    }

    @Test
    public void testLshHttpUsernamePasswordPublic() throws Exception {
        testLsHttpUsernamePassword("httpUsernamePassword.txt");
    }

    @Test
    public void testLshHttpUsernamePasswordPrivate() throws Exception {
        testLsHttpUsernamePassword("httpUsernamePasswordPrivate.txt");
    }

    public void testLsHttpUsernamePassword(String filename) throws Exception {

        File authData = new File(testFileLocation + filename);

        // If a developer does not have this file present, test should just pass.
        if (!authData.exists())
            return;

        Scanner scanner = new Scanner(authData);
        String ignoreURL = scanner.next();
        String username = scanner.next();
        String password = scanner.next();
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(username, password);

        TransportCommand command = Git.lsRemoteRepository().setRemote(GITHUB_REMOTE_URL);
        RepoHelper helper = new RepoHelper("");
        helper.wrapAuthentication(command, credentials);
        command.call();
    }

    @Test
    // Test Https access, with empty string credentials, to see if it works for a repo that is public
    // ... and verify it fails with a bad username or password
    public void testLsHttpUsernamePasswordEmpty() throws Exception {

        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider("a", "asdas");

        TransportCommand command =
                Git.lsRemoteRepository().setRemote("https://github.com/TheElegitTeam/TestRepository.git");
        //RepoHelper.wrapAuthentication(command, credentials);
        command.call();
    }


    /* The sshPassword should contain two lines:
        repo ssh address
        password
     */
    @Test
    public void testLsSshPassword() throws Exception {

        File urlFile = new File(testFileLocation + "sshPasswordURL.txt");
        Path passwordFile = Paths.get(testFileLocation, "sshPasswordPassword.txt");

        // If a developer does not have this file present, test should just pass.
        if ((!urlFile.exists() || !Files.exists(passwordFile) && looseTesting))
            return;

        Scanner scanner = new Scanner(urlFile);
        String remoteURL = scanner.next();

        List<String> userCredentials = Files.readAllLines(passwordFile);
        TransportCommand command = Git.lsRemoteRepository().setRemote(remoteURL);
        RepoHelper helper = new RepoHelper(new ElegitUserInfoTest(userCredentials.get(0), null));
        helper.wrapAuthentication(command);
        command.call();
    }

    @Rule
    public ExpectedException exception = ExpectedException.none();


    @Test
    public void testSshPassword() throws Exception {
        Path repoPath = directoryPath.resolve("testrepo");
        File urlFile = new File(testFileLocation + "sshPasswordURL.txt");
        Path passwordFile = Paths.get(testFileLocation, "sshPasswordPassword.txt");

        // If a developer does not have this file present, test should just pass.
        if ((!urlFile.exists() || !Files.exists(passwordFile) && looseTesting))
            return;

        Scanner scanner = new Scanner(urlFile);
        String remoteURL = scanner.next();
        scanner.close();
        scanner = new Scanner(passwordFile);
        String password = scanner.next();

        //ClonedRepoHelper helper = new ClonedRepoHelper(repoPath, remoteURL, new ElegitUserInfoTest(password, null));
        ClonedRepoHelper helper = new ClonedRepoHelper(repoPath, remoteURL, password,
                                                       new ElegitUserInfoTest(password, null));
        helper.obtainRepository(remoteURL);
        assertEquals(helper.getCompatibleAuthentication(), AuthMethod.SSH);
        helper.fetch(false);
        PushCommand command = helper.prepareToPushAll();
        helper.pushAll(command);
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

        JSch.setLogger(new MyLogger());
        Path repoPath = directoryPath.resolve("testrepo");
        File urlFile = new File(testFileLocation + "sshPrivateKeyURL.txt");
        File passwordFile = new File(testFileLocation + "sshPrivateKeyPassword.txt");

        // If a developer does not have this file present, test should just pass.
        if ((!urlFile.exists() || !passwordFile.exists()) && looseTesting)
            return;

        Scanner scanner = new Scanner(urlFile);
        String remoteURL = scanner.next();
        scanner.close();
        scanner = new Scanner(passwordFile);
        String passphrase = scanner.next();

        ClonedRepoHelper helper = new ClonedRepoHelper(repoPath, remoteURL, passphrase,
                                                       new ElegitUserInfoTest(null, passphrase));
        helper.obtainRepository(remoteURL);
        assertEquals(helper.getCompatibleAuthentication(), AuthMethod.SSH);
        helper.fetch(false);
        PushCommand command = helper.prepareToPushAll();
        helper.pushAll(command);
        scanner.close();
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
        System.out.println();
        for (TransportProtocol protocol : protocols) {
            if (protocol.canHandle(new URIish("https://github.com/TheElegitTeam/TestRepository.git"))) {
                assertEquals(protocol.getName(), "HTTP");
                assertNotEquals(protocol.getName(), "SSH");
            }

            if (protocol.canHandle(new URIish("git@github.com:TheElegitTeam/TestRepository.git"))) {
                assertEquals(protocol.getName(), "SSH");
                assertNotEquals(protocol.getName(), "HTTP");
            }
        }
    }

    @Test
    public void testSshCallback() throws Exception {

        File urlFile = new File(testFileLocation + "keypairTesting.txt");

        // If a developer does not have this file present, test should just pass.
        if (!urlFile.exists() && looseTesting) {
            System.out.println("Ignoring keypair testing. Create a keypairTesting.txt file if you wish.");
            return;

        }

        LsRemoteCommand command = Git.lsRemoteRepository();
        //command.setRemote("https://github.com/TheElegitTeam/TestRepository.git");
        command.setRemote("git@github.com:TheElegitTeam/TestRepository.git");

        SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
            @Override
            protected void configure(OpenSshConfig.Host host, Session session) {
                // do nothing
            }
        };

        command.setTransportConfigCallback(new TransportConfigCallback() {
            @Override
            public void configure(Transport transport) {
                System.out.println(transport.getClass());
                // This cast will fail if SSH is not the protocol used
                SshTransport sshTransport = (SshTransport) transport;
                sshTransport.setSshSessionFactory(sshSessionFactory);

            }
        });
        // Command will fail if config not set up correctly; uses public/private key

        try {
            command.call();
        } catch (TransportException e) {
            fail("Public/private key authentication failed. You should set this up in your ssh/.config.");

        }

    }


    @Test
    public void testCloneRepositoryWithCheckshHttpUsernamePasswordPublic() throws Exception {
        testCloneRepositoryWithChecksHttpUsernamePassword("httpUsernamePassword.txt");
    }

    @Test
    public void testCloneRepositoryWithChecksHttpUsernamePasswordPrivate() throws Exception {
        testCloneRepositoryWithChecksHttpUsernamePassword("httpUsernamePasswordPrivate.txt");
    }


    private void testCloneRepositoryWithChecksHttpUsernamePassword(String filename) throws Exception {
        File authData = new File(testFileLocation + filename);

        // If a developer does not have this file present, test should just pass.
        if (!authData.exists())
            return;

        Scanner scanner = new Scanner(authData);
        String ignoreURL = scanner.next();
        String username = scanner.next();
        String password = scanner.next();

        Path repoPath = directoryPath.resolve("testrepo");

        RepoHelperBuilder.AuthDialogResponse response =
                new RepoHelperBuilder.AuthDialogResponse(null, username, password, false);

        ClonedRepoHelperBuilder.cloneRepositoryWithChecks(GITHUB_REMOTE_URL, repoPath, response,
                                                          new ElegitUserInfoTest());

    }


}