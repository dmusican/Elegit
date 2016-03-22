package main.java.elegit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

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

    @Test
    public void testSetAuthenticationPref() throws Exception {
        SessionModel sessionModel = SessionModel.getSessionModel();
        String pathname = directoryPath.toString();
        sessionModel.setAuthPref(pathname,AuthMethod.SSHPASSWORD);
        assertEquals(AuthMethod.SSHPASSWORD,sessionModel.getAuthPref(pathname));
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
//        for (String s : sessionModel.listAuthPaths()) {
//            System.out.println(s);
//        }
    }

}