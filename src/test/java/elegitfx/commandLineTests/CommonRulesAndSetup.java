package elegitfx.commandLineTests;

import elegit.Main;
import junit.framework.TestCase;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestName;
import org.testfx.framework.junit.TestFXRule;
import sharedrules.TestingLogPathRule;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by grenche on 6/13/18.
 */
public class CommonRulesAndSetup extends ExternalResource {
    @ClassRule
    public static final LoggingInitializationStart loggingInitializationStart = new LoggingInitializationStart();

    @ClassRule public static final TestingLogPathRule testingLogPath = new TestingLogPathRule();

    @Rule
    public TestFXRule testFXRule = new TestFXRule();

    @Rule public TestName testName = new TestName();

    public Path directoryPath;

    private static Logger logger = LoggingInitializationStart.getLogger();

    private static Logger console = LoggingInitializationStart.getConsole();

    @Before
    public void setup() throws Exception {
        logger.info("Unit test started");
        console.info("Unit test started");
        directoryPath = Files.createTempDirectory("unitTestRepos");
        directoryPath.toFile().deleteOnExit();
        logger.info("Test name: " + testName.getMethodName());
        console.info("Test name: " + testName.getMethodName());
    }

    @After
    public void tearDown() {
        logger.info("Tearing down");
        TestCase.assertEquals(0, Main.getAssertionCount());
    }

    public static Logger getConsole() {
        return console;
    }

    public static Logger getLogger() {
        return logger;
    }

    public Path getDirectoryPath() {
        return directoryPath;
    }
}
