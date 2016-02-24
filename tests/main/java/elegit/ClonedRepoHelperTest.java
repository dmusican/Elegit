package main.java.elegit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * Created by grahamearley on 2/21/16.
 */
public class ClonedRepoHelperTest {

    Path directoryPath;
    String remoteURL;
    String username;

    @Before
    public void setUp() throws Exception {
        // Clone to the current directory:
        this.directoryPath = Paths.get("");

        // Clone from dummy repo:
        this.remoteURL = "https://github.com/TheElegitTeam/TestRepository.git";

        this.username = "Dummy_Username";
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testClonedRepoHelperConstructor() throws Exception {
        ClonedRepoHelper helper = new ClonedRepoHelper(directoryPath, remoteURL, username);
        assertNotNull(helper.getRepo());
    }
}