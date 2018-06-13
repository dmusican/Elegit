package elegitfx.commandLineTests;

import elegit.exceptions.ExceptionAdapter;
import elegit.models.ExistingRepoHelper;
import elegit.monitors.RepositoryMonitor;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.sshd.server.SshServer;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Assert;
import org.junit.Rule;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;
import sharedrules.TestUtilities;
import sharedrules.TestingRemoteAndLocalReposRule;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertEquals;

/**
 * Created by grenche on 6/13/18.
 * Methods that are used by a variety of/all the tests related to testing the command line tool
 */
public class CommandLineTestUtilities extends ApplicationTest {

    private static final Logger console = LogManager.getLogger("briefconsolelogger");

    /**
     * Checks the TextArea to see if its contents are equal to the command that is expected to be there.
     * Currently super ugly hack because the TextArea is not in MainView, so need to go through HBox and ScrollPane.
     */
    public void checkCommandLineText(String command) {
        HBox commandLine = lookup("#commandLine").query();
        List<Node> children = commandLine.getChildren();

        for(int i = 0; i < children.size(); i++) {
            Node child = children.get(i);
            if (child.getId().equals("commandBar")) {

                ScrollPane scrollPane = (ScrollPane) child;
                TextArea currentCommand = (TextArea) scrollPane.getContent();

                console.info("Checking the text...");
                assertEquals(command, currentCommand.getText());
            }
        }
    }

    public void cloneRepoUsingButtons(TestingRemoteAndLocalReposRule testingRemoteAndLocalRepos, Path directoryPath) throws Exception {
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
        }
    }
}
