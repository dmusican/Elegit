package main.java.elegit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;

import static org.junit.Assert.*;

/**
 * Created by dmusican on 2/13/16.
 */
public class SessionModelTest {

    private Path directoryPath;

    @Before
    public void setUp() throws Exception {
        this.directoryPath = Files.createTempDirectory("unitTestRepos");
        directoryPath.toFile().deleteOnExit();
    }

    @After
    public void tearDown() throws Exception {

    }

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testSetAuthenticationPref() throws Exception {
        SessionModel sessionModel = SessionModel.getSessionModel();
        String pathname = "___hellothere___";
        sessionModel.setAuthPref(pathname,AuthMethod.SSHPASSWORD);
        assertEquals(AuthMethod.SSHPASSWORD,sessionModel.getAuthPref(pathname));
        boolean foundIt = false;
        for (String s : sessionModel.listAuthPaths())
            if (s.equals(pathname))
                foundIt = true;
        assertEquals(foundIt,true);
        sessionModel.removeAuthPref(pathname);
        exception.expect(NoSuchElementException.class);
        exception.expectMessage("AuthPref not present");
        sessionModel.getAuthPref(pathname);
    }

    @Test
    public void testAuthMethodValues() throws Exception {
        AuthMethod http = AuthMethod.HTTP;
        AuthMethod https = AuthMethod.HTTPS;
        AuthMethod sshpassword = AuthMethod.SSHPASSWORD;
        assertEquals(http.getEnumValue(),0);
        assertEquals(https.getEnumValue(),1);
        assertEquals(sshpassword.getEnumValue(),2);
        assertNotEquals(AuthMethod.HTTP,AuthMethod.getEnumFromValue(1));
        assertEquals(AuthMethod.HTTP,AuthMethod.getEnumFromValue(0));
        assertEquals(AuthMethod.HTTPS,AuthMethod.getEnumFromValue(1));
        assertEquals(AuthMethod.SSHPASSWORD,AuthMethod.getEnumFromValue(2));

    }

    @Test
    public void testSeeAuthPrefs() throws Exception {
        SessionModel sessionModel = SessionModel.getSessionModel();
        String pathname = directoryPath.toString();
        System.out.println("..." + pathname);
        sessionModel.setAuthPref(pathname,AuthMethod.SSHPASSWORD);
        System.out.println(sessionModel.getAuthPref(pathname));
        // Gotta fix the preferences. See my todos.
        fail();
        for (String s : sessionModel.listAuthPaths()) {
            System.out.println(s);
        }
    }

}