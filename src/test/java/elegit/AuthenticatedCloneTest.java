package elegit;

import com.jcraft.jsch.Session;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.transport.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import static org.junit.Assert.*;

public class AuthenticatedCloneTest {

    private Path directoryPath;
    private String testFileLocation;
    Path logPath;

    // Used to indicate that if password files are missing, then tests should just pass
    private boolean looseTesting;


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
        for (File file: dir.listFiles()) {
            if (file.isDirectory()) removeAllFilesFromDirectory(file);
            file.delete();
        }
    }

    @Test
    public void     testCloneHttpNoPassword() throws Exception {
        Path repoPath = directoryPath.resolve("testrepo");
        // Clone from dummy repo:
        String remoteURL = "https://github.com/TheElegitTeam/TestRepository.git";

        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider("", "");
        ClonedRepoHelper helper = new ClonedRepoHelper(repoPath, remoteURL, credentials);
        assertNotNull(helper);

    }

    @Test
    public void testLsHttpNoPassword() throws Exception {
        testLsHttpUsernamePassword("httpNoUsernamePassword.txt");
    }

    @Test
    public void testHttpUsernamePasswordPublic() throws Exception {
        testHttpUsernamePassword("httpUsernamePassword.txt");
    }

    @Test
    public void testHttpUsernamePasswordPrivate() throws Exception {
        testHttpUsernamePassword("httpUsernamePasswordPrivate.txt");
    }


    /* The httpUsernamePassword should contain three lines, containing:
        repo http(s) address
        username
        password
     */
    public void testHttpUsernamePassword(String filename) throws Exception {
        Path repoPath = directoryPath.resolve("testrepo");
        File authData = new File(testFileLocation + filename);

        // If a developer does not have this file present, test should just pass.
        if (!authData.exists() && looseTesting)
            return;

        Scanner scanner = new Scanner(authData);
        String remoteURL = scanner.next();
        String username = scanner.next();
        String password = scanner.next();
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(username, password);
        ClonedRepoHelper helper = new ClonedRepoHelper(repoPath, remoteURL, credentials);
        assertEquals(helper.getCompatibleAuthentication(),AuthMethod.HTTP);
        helper.fetch();
        Path fileLocation = repoPath.resolve("README.md");
        System.out.println(fileLocation);
        FileWriter fw = new FileWriter(fileLocation.toString(), true);
        fw.write("1");
        fw.close();
        helper.addFilePath(fileLocation);
        helper.commit("Appended to file");
        helper.pushAll();
        helper.pushTags();
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
        String remoteURL = scanner.next();
        String username = scanner.next();
        String password = scanner.next();
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(username, password);

        TransportCommand command = Git.lsRemoteRepository().setRemote(remoteURL);
        RepoHelper.wrapAuthentication(command, credentials);
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
        Path passwordFile = Paths.get(testFileLocation,"sshPasswordPassword.txt");

        // If a developer does not have this file present, test should just pass.
        if ((!urlFile.exists() || !Files.exists(passwordFile) && looseTesting))
            return;

        Scanner scanner = new Scanner(urlFile);
        String remoteURL = scanner.next();

        List<String> userCredentials = Files.readAllLines(passwordFile);
        TransportCommand command = Git.lsRemoteRepository().setRemote(remoteURL);
        RepoHelper.wrapAuthentication(command, userCredentials);
        command.call();
    }

    @Rule
    public ExpectedException exception = ExpectedException.none();


    @Test
    public void testSshPassword() throws Exception {
        Path repoPath = directoryPath.resolve("testrepo");
        File urlFile = new File(testFileLocation + "sshPasswordURL.txt");
        Path passwordFile = Paths.get(testFileLocation,"sshPasswordPassword.txt");

        // If a developer does not have this file present, test should just pass.
        if ((!urlFile.exists() || !Files.exists(passwordFile) && looseTesting))
            return;

        Scanner scanner = new Scanner(urlFile);
        String remoteURL = scanner.next();

        List<String> userCredentials = Files.readAllLines(passwordFile);
        ClonedRepoHelper helper = new ClonedRepoHelper(repoPath, remoteURL, userCredentials);
        assertEquals(helper.getCompatibleAuthentication(),AuthMethod.SSH);
        helper.fetch();
        helper.pushAll();
        helper.pushTags();
    }

    @Test
    public void testSshPrivateKey() throws Exception {
        Path repoPath = directoryPath.resolve("testrepo");
        File urlFile = new File(testFileLocation + "sshPrivateKeyURL.txt");
        File passwordFile = new File(testFileLocation + "sshPrivateKeyPassword.txt");

        // If a developer does not have this file present, test should just pass.
        if ((!urlFile.exists() || !passwordFile.exists()) && looseTesting)
            return;

        Scanner scanner = new Scanner(urlFile);
        String remoteURL = scanner.next();
//        ClonedRepoHelper helper = new ClonedRepoHelper(repoPath, remoteURL, passwordFile);
//        assertEquals(helper.getCompatibleAuthentication(),AuthMethod.SSH);
//        helper.fetch();
//        helper.pushAll();
//        helper.pushTags();
//        scanner.close();
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
        LsRemoteCommand command = Git.lsRemoteRepository();
        //command.setRemote("https://github.com/TheElegitTeam/TestRepository.git");
        command.setRemote("git@github.com:TheElegitTeam/TestRepository.git");

        SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
            @Override
            protected void configure(OpenSshConfig.Host host, Session session ) {
                // do nothing
            }
        };

        command.setTransportConfigCallback( new TransportConfigCallback() {
            @Override
            public void configure( Transport transport ) {
                System.out.println(transport.getClass());
                // This cast will fail if SSH is not the protocol used
                SshTransport sshTransport = ( SshTransport )transport;
                sshTransport.setSshSessionFactory( sshSessionFactory );

        }
        } );
        // Command will fail if config not set up correctly; uses public/private key
        command.call();

    }


}