package elegit;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
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
import java.util.NoSuchElementException;
import java.util.Scanner;

import static org.junit.Assert.*;

public class AuthenticatedCloneTest {

    private Path directoryPath;
    private String testFileLocation;
    Path logPath;


    @Before
    public void setUp() throws Exception {
        initializeLogger();
        this.directoryPath = Files.createTempDirectory("unitTestRepos");
        directoryPath.toFile().deleteOnExit();
        testFileLocation = System.getProperty("user.home") + File.separator +
                           "elegitTests" + File.separator;
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
    public void testCloneHttpNoPassword() throws Exception {
        Path repoPath = directoryPath.resolve("testrepo");
        // Clone from dummy repo:
        String remoteURL = "https://github.com/TheElegitTeam/TestRepository.git";

        ClonedRepoHelper helper = new ClonedRepoHelper(repoPath, remoteURL);
        assertNotNull(helper);

    }

    @Test
    public void testLsHttpNoPassword() throws Exception {

        TransportCommand command =
                Git.lsRemoteRepository().setRemote("https://github.com/TheElegitTeam/TestRepository.git");
        command.call();
    }

    /* The httpUsernamePassword should contain three lines, containing:
        repo http(s) address
        username
        password
     */
    @Test
    public void testHttpUsernamePassword() throws Exception {
        Path repoPath = directoryPath.resolve("testrepo");
        File authData = new File(testFileLocation + "httpUsernamePassword.txt");

        // If a developer does not have this file present, test should just pass.
        if (!authData.exists())
            return;

        Scanner scanner = new Scanner(authData);
        String remoteURL = scanner.next();
        String username = scanner.next();
        String password = scanner.next();
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(username, password);
        ClonedRepoHelper helper = new ClonedRepoHelper(repoPath, remoteURL, credentials);
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
    public void testLsHttpUsernamePassword() throws Exception {

        Path repoPath = directoryPath.resolve("testrepo");
        File authData = new File(testFileLocation + "httpUsernamePassword.txt");

        // If a developer does not have this file present, test should just pass.
        if (!authData.exists())
            return;

        Scanner scanner = new Scanner(authData);
        String remoteURL = scanner.next();
        String username = scanner.next();
        String password = scanner.next();
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(username, password);

        TransportCommand command = Git.lsRemoteRepository().setRemote(remoteURL);
        RepoHelper.wrapAuthentication(command, remoteURL, credentials);
        command.call();
    }

    /* The sshPassword should contain two lines:
        repo ssh address
        password
     */
    @Test
    public void testLsSshPassword() throws Exception {

        Path repoPath = directoryPath.resolve("testrepo");
        File authData = new File(testFileLocation + "sshPassword.txt");

        // If a developer does not have this file present, test should just pass.
        if (!authData.exists())
            return;

        Scanner scanner = new Scanner(authData);
        String remoteURL = scanner.next();
        String password = scanner.next();

        TransportCommand command = Git.lsRemoteRepository().setRemote(remoteURL);
        RepoHelper.wrapAuthentication(command, remoteURL, password);
        command.call();
    }

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testSshPassword() throws Exception {
        Path repoPath = directoryPath.resolve("testrepo");
        File authData = new File(testFileLocation + "sshPassword.txt");

        // If a developer does not have this file present, test should just pass.
        if (!authData.exists())
            return;

        Scanner scanner = new Scanner(authData);
        String remoteURL = scanner.next();
        String password = scanner.next();
        ClonedRepoHelper helper = new ClonedRepoHelper(repoPath, remoteURL, password);
        helper.fetch();
        helper.pushAll();
        helper.pushTags();
        SessionModel sm = SessionModel.getSessionModel();
        String pathname = repoPath.toString();
        assertEquals(sm.getAuthPref(pathname), AuthMethod.SSHPASSWORD);
        assertNotEquals(sm.getAuthPref(pathname), AuthMethod.HTTPS);

        sm.removeAuthPref(pathname);
        exception.expect(NoSuchElementException.class);
        exception.expectMessage("AuthPref not present");
        sm.getAuthPref(pathname);
    }

}
