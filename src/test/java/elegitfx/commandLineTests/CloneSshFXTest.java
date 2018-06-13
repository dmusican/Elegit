package elegitfx.commandLineTests;

import javafx.stage.Stage;
import org.apache.logging.log4j.Logger;
import org.junit.*;
import org.testfx.framework.junit.ApplicationTest;
import sharedrules.TestUtilities;
import sharedrules.TestingRemoteAndLocalReposRule;

import java.nio.file.Path;

/**
 * Created by grenche on 6/13/18.
 * Tests the Terminal Command window after cloning a repo via SSH
 */
public class CloneSshFXTest extends ApplicationTest {

    @Rule
    public final CommonRulesAndSetup commonRulesAndSetup = new CommonRulesAndSetup();

    private static Logger logger = CommonRulesAndSetup.getLogger();

    private static Logger console = CommonRulesAndSetup.getConsole();

    public Path directoryPath = commonRulesAndSetup.getDirectoryPath();

    @Rule
    public final TestingRemoteAndLocalReposRule testingRemoteAndLocalRepos =
            new TestingRemoteAndLocalReposRule(false);

    public final CommandLineTestUtilities commandLineTestUtilities = new CommandLineTestUtilities();

    @Before
    public void setup() {
        directoryPath = testingRemoteAndLocalRepos.getDirectoryPath();
    }

    @Override
    public void start(Stage stage) throws Exception {
        TestUtilities.commonTestFxStart(stage);
    }

    @Test
    public void sshCloneTest() throws Exception {
        // Clones a test repo via clicking the ribbon button
        commandLineTestUtilities.cloneRepoUsingButtons(testingRemoteAndLocalRepos, directoryPath);

        // Checks that the text in the command line box is what is expected.
        commandLineTestUtilities.checkCommandLineText("git clone ssh://localhost:2222/remote "
                + directoryPath + "/local");

        console.info("Test passed.");
    }
}
