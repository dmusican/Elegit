package elegitfx.commandLineTests;

import elegit.Main;
import junit.framework.TestCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestName;
import sharedrules.TestUtilities;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by grenche on 6/13/18.
 * Shared setup and teardown code used in most test
 */
public class CommonRulesAndSetup extends ExternalResource {

    public Path directoryPath;

    private static final Logger console = LogManager.getLogger("briefconsolelogger");
    private static final Logger logger = LogManager.getLogger("consolelogger");

    @Before
    public Path setup(TestName testName) throws Exception {
        logger.info("Unit test started");
        console.info("Unit test started");
        directoryPath = Files.createTempDirectory("unitTestRepos");
        directoryPath.toFile().deleteOnExit();
        logger.info("Test name: " + testName.getMethodName());
        console.info("Test name: " + testName.getMethodName());
        return directoryPath;
    }

    @After
    public void tearDown() {
        logger.info("Tearing down");
        TestUtilities.cleanupTestFXEnvironment();
        TestCase.assertEquals(0, Main.getAssertionCount());
    }
}
