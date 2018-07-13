package elegitfx.commandLineTests;

import elegit.controllers.SessionController;
import elegit.exceptions.ExceptionAdapter;
import elegit.gui.WorkingTreePanelView;
import elegit.models.ExistingRepoHelper;
import elegit.models.LocalBranchHelper;
import elegit.monitors.RepositoryMonitor;
import elegit.sshauthentication.ElegitUserInfoTest;
import elegit.treefx.TreeLayout;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.sshd.server.SshServer;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.revwalk.RevCommit;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.robot.Motion;
import org.testfx.util.WaitForAsyncUtils;
import sharedrules.TestUtilities;
import sharedrules.TestingRemoteAndLocalReposRule;

import java.io.FileWriter;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.io.File.separator;
import static junit.framework.TestCase.assertEquals;

/**
 * Created by grenche on 6/13/18.
 * Methods that are used by a variety of/all the tests related to testing the command line tool
 */
public class CommandLineTestUtilities extends ApplicationTest {

    private static final Logger logger = LogManager.getLogger("briefconsolelogger");
    private static final Logger console = LogManager.getLogger("briefconsolelogger");

    private static final Random random = new Random(90125);

    /**
     * Checks the TextArea to see if its contents are equal to the command that is expected to be there.
     * Currently super ugly hack because the TextArea is not in MainView, so need to go through HBox and ScrollPane.
     */
    public void checkCommandLineText(String command) {
        interact(() -> {
        HBox commandLine = lookup("#commandLine").query();
        List<Node> children = commandLine.getChildren();

        for (int i = 0; i < children.size(); i++) {
            Node child = children.get(i);
            if (child.getId().equals("commandBar")) {

                ScrollPane scrollPane = (ScrollPane) child;
                TextArea currentCommand = (TextArea) scrollPane.getContent();

                console.info("Checking the text...");
                assertEquals(command, currentCommand.getText());
            }
        }});
    }

    public String cloneRepoUsingButtons(TestingRemoteAndLocalReposRule testingRemoteAndLocalRepos, Path directoryPath) throws Exception {
        /***** Same/Similar code to cloning a repo in SshCloneFXTest *****/

        // Set up remote repo
        Path remote = testingRemoteAndLocalRepos.getRemoteFull();
        Path remoteFilePath = remote.resolve("file.txt");
        Files.write(remoteFilePath, "testSshPassword".getBytes());
        ExistingRepoHelper helperServer = new ExistingRepoHelper(remote, null);
        helperServer.addFilePathTest(remoteFilePath);
        RevCommit firstCommit = helperServer.commit("Initial unit test commit");
        console.info("firstCommit name = " + firstCommit.getName());

        // Uncomment this to get detail SSH logging info, for debugging
//         JSch.setLogger(new DetailedSshLogger());

        // Set up test SSH server.
        try (SshServer sshd = SshServer.setUpDefaultServer()) {

            String remoteURL = TestUtilities.setUpTestSshServer(sshd,
                    directoryPath,
                    testingRemoteAndLocalRepos.getRemoteFull(),
                    testingRemoteAndLocalRepos.getRemoteBrief());

            console.info("Connecting to " + remoteURL);

            InputStream passwordFileStream = getClass().getResourceAsStream("/rsa_key1_passphrase.txt");
            Scanner scanner = new Scanner(passwordFileStream);
            String passphrase = scanner.next();
            String privateKeyFileLocation = "/rsa_key1";
            Path knownHostsFileLocation = directoryPath.resolve("testing_known_hosts");

            clickOn("#loadNewRepoButton")
                    .clickOn("#cloneOption")
                    .clickOn("#remoteURLField")
                    .write(remoteURL)
                    .clickOn("#enclosingFolderField")
                    .write(testingRemoteAndLocalRepos.getDirectoryPath().toString())
                    .doubleClickOn("#repoNameField")
                    .write(testingRemoteAndLocalRepos.getLocalBrief().toString())
                    .clickOn("#cloneButton")

                    // Enter in private key location
                    .clickOn("#repoInputDialog")
                    .write(getClass().getResource(privateKeyFileLocation).getFile())
                    .clickOn("#repoInputDialogOK")

                    // Enter in known hosts location
                    .clickOn("#repoInputDialog")
                    .write(knownHostsFileLocation.toString())
                    .clickOn("#repoInputDialogOK")

                    // Useless HTTP login screen that should eventually go away
                    .clickOn("#loginButton");


            RepositoryMonitor.pause();

            WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS,
                    () -> lookup("Yes").query() != null);
            WaitForAsyncUtils.waitForFxEvents();
            clickOn("Yes");

            WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS,
                    () -> lookup("#sshprompt").query() != null);
            WaitForAsyncUtils.waitForFxEvents();

            // Enter passphrase
            clickOn("#sshprompt")
                    .write(passphrase)
                    .write("\n");

            // Wait until a node is in the graph, indicating clone is done
            WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS,
                    () -> lookup(".tree-cell").query() != null);
            WaitForAsyncUtils.waitForFxEvents();
            sleep(100);

            Assert.assertEquals(0, ExceptionAdapter.getWrappedCount());

            // Shut down test SSH server
            sshd.stop();

            /***** End of copied code *****/

            String paths = remoteURL + " " + testingRemoteAndLocalRepos.getDirectoryPath().toString() + separator +
                    testingRemoteAndLocalRepos.getLocalBrief().toString();

            WaitForAsyncUtils.waitForFxEvents();
            sleep(1000);

            return paths;
        }
    }

    public RevCommit setupTestRepo(Path directoryPath, SessionController sessionController) throws Exception {
        logger.info("Temp directory: " + directoryPath);
        Path remote = directoryPath.resolve("remote");
        Path local = directoryPath.resolve("local");
        Git.init().setDirectory(remote.toFile()).setBare(true).call();
        Git.cloneRepository().setDirectory(local.toFile()).setURI("file://" + remote).call();

        ExistingRepoHelper helper = new ExistingRepoHelper(local, new ElegitUserInfoTest());

        Path fileLocation = local.resolve("README.md");

        FileWriter fw = new FileWriter(fileLocation.toString(), true);
        fw.write("start");
        fw.close();
        helper.addFilePathTest(fileLocation);
        RevCommit commit = helper.commit("Appended to file");

        for (int i = 0; i < 4; i++) {
            LocalBranchHelper branchHelper = helper.getBranchModel().createNewLocalBranch("branch" + i);
            branchHelper.checkoutBranch();
            fw = new FileWriter(fileLocation.toString(), true);
            fw.write("commit number: " + (i + 1));
            fw.close();
            helper.addFilePathTest(fileLocation);
            commit = helper.commit("Appended to file");
        }

        // Just push all untracked local branches
        PushCommand command = helper.prepareToPushAll(untrackedLocalBranches -> untrackedLocalBranches);
        helper.pushAll(command);

        logger.info(remote);
        logger.info(local);

        // Used to slow down commit adds. Set to help cause bug we saw where highlight commit was happening before
        // layout was done
        TreeLayout.cellRenderTimeDelay.set(1000);

        SessionController.gitStatusCompletedOnce = new CountDownLatch(1);

        interact(() -> sessionController.handleLoadExistingRepoOption(local));
        SessionController.gitStatusCompletedOnce.await();

        logger.info("Layout done");

        return commit;
    }

    public String[] clickReset(RevCommit commit, String resetType) {
        // Click on last commit and checkout the README.md file
        rightClickOn(Matchers.hasToString(commit.getName()))
                .clickOn("Reset...")
                // Weird thing where if the menu chain gets too big, it moves in a way that causes the menu to disappear
                .clickOn("Advanced", Motion.HORIZONTAL_FIRST)
                .clickOn("reset --" + resetType, Motion.HORIZONTAL_FIRST);

        // Get the name of the commit
        final String[] id = getCommitId(commit);

        console.info("Finished clicking reset.");
        return id;
    }

    public String[] getCommitId(RevCommit commit) {
        final String[] id = new String[1];
        interact(() -> id[0] = commit.getName());
        console.info("Got the commit id: " + id[0]);
        return id;
    }

    public void addFile(Path filePath, Path directory, SessionController sessionController) throws Exception {
        ArrayList<Path> filePaths = new ArrayList<>();
        filePaths.add(filePath);
        addFile(filePaths, directory, sessionController);
    }
    //need to click box
    public void addFile(ArrayList<Path> filePaths, Path directory, SessionController sessionController) throws Exception{
        ExistingRepoHelper helper = new ExistingRepoHelper(directory, new ElegitUserInfoTest());
        for(Path filePath : filePaths){
            FileWriter fw = new FileWriter(filePath.toString(), true);
            fw.write("start");
            fw.close();
        }
        helper.addFilePathsTest(filePaths, false);
        helper.commit("made initial file");
        interact(() -> sessionController.handleLoadExistingRepoOption(directory));
        RepositoryMonitor.unpause();
        String fileName="";
        for(Path filePath : filePaths) {
            FileWriter fw = new FileWriter(filePath.toString(), true);
            fw.write("update");
            fw.close();
            fileName = filePath.getFileName().toString();
        }
        final String lookUpFile = fileName;
        WaitForAsyncUtils.waitFor(20, TimeUnit.SECONDS,
                () -> lookup(lookUpFile).queryAll().size() == 2);
        sleep(100);
        // When looking up the file, it registers multiple nodes since it is nested inside a tree. Pick the
        // checkbox of interest.
        WorkingTreePanelView workingTree = lookup("#workingTreePanelView").query();

        logger.info("Before clicking add");
        interact(() -> {
            workingTree.setAllFilesSelected(true);
        });
        clickOn("Add");

        // Wait for file to also be added to index pane
        logger.info("When add seems to be done");
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS,
                () -> lookup(lookUpFile).queryAll().size() == 3);
    }

}
