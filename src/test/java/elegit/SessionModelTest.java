package elegit;

import elegit.exceptions.ExceptionAdapter;
import elegit.models.AuthMethod;
import elegit.models.SessionModel;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import sharedrules.TestUtilities;
import sharedrules.TestingLogPathRule;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;

import static org.junit.Assert.*;

/**
 * Created by dmusican on 2/13/16.
 */
public class SessionModelTest {

    @ClassRule
    public static final TestingLogPathRule testingLogPath = new TestingLogPathRule();

    private Path directoryPath;
    Path logPath;

    @Before
    public void setUp() throws Exception {
        TestUtilities.setupTestEnvironment();
        initializeLogger();
        this.directoryPath = Files.createTempDirectory("unitTestRepos");
        directoryPath.toFile().deleteOnExit();
    }

    @After
    public void tearDown() throws Exception {
        removeAllFilesFromDirectory(this.logPath.toFile());
        TestUtilities.cleanupTestEnvironment();
    }

    // Helper method to avoid annoying traces from logger
    void initializeLogger() {
        // Create a temp directory for the files to be placed in
        try {
            this.logPath = Files.createTempDirectory("elegitLogs");
        } catch (IOException e) {
            throw new ExceptionAdapter(e);
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
    public void testPathnameHash() throws Exception {
        SessionModel sessionModel = SessionModel.getSessionModel();
        String pathname =  directoryPath.toString();
        System.out.println(sessionModel.hashPathname(pathname));
    }
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testSetAuthenticationPref() throws Exception {
        SessionModel sessionModel = SessionModel.getSessionModel();
        String pathname =  directoryPath.toString();
        sessionModel.setAuthPref(pathname, AuthMethod.SSH);
        assertEquals(AuthMethod.SSH,sessionModel.getAuthPref(pathname));
        boolean foundIt = false;
        for (String s : sessionModel.listAuthPaths())
            if (s.equals(sessionModel.hashPathname(pathname)))
                foundIt = true;
        assertEquals(foundIt,true);
        AuthMethod authBack = sessionModel.getAuthPref(pathname);
        assertEquals(authBack,AuthMethod.SSH);
        sessionModel.removeAuthPref(pathname);

        // Throw error on not there
        exception.expect(NoSuchElementException.class);
        exception.expectMessage("AuthPref not present");
        authBack = sessionModel.getAuthPref(pathname);
    }

    @Test
    public void testAuthMethodValues() throws Exception {
        AuthMethod http = AuthMethod.HTTP;
        AuthMethod https = AuthMethod.HTTPS;
        AuthMethod ssh = AuthMethod.SSH;
        assertEquals(http.getEnumValue(),0);
        assertEquals(https.getEnumValue(),1);
        assertEquals(ssh.getEnumValue(),2);
        assertNotEquals(AuthMethod.HTTP,AuthMethod.getEnumFromValue(1));
        assertEquals(AuthMethod.HTTP,AuthMethod.getEnumFromValue(0));
        assertEquals(AuthMethod.HTTPS,AuthMethod.getEnumFromValue(1));
        assertEquals(AuthMethod.SSH,AuthMethod.getEnumFromValue(2));

    }

    @Test
    public void testSeeAuthPrefs() throws Exception {
        SessionModel sessionModel = SessionModel.getSessionModel();
        String pathname = directoryPath.toString();
        System.out.println("..." + pathname);
        sessionModel.setAuthPref(pathname,AuthMethod.SSH);
//        System.out.println(sessionModel.getAuthPref(pathname));
//        for (String s : sessionModel.listAuthPaths()) {
//            System.out.println(s);
//        }
    }

}