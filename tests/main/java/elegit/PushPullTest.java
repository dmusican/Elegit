package main.java.elegit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by dmusican on 4/4/16.
 */
public class PushPullTest {


    private Path directoryPath;
    private String testFileLocation;


    @Before
    public void setUp() throws Exception {
        this.directoryPath = Files.createTempDirectory("unitTestRepos");
        directoryPath.toFile().deleteOnExit();
        testFileLocation = System.getProperty("user.home") + File.separator +
                           "elegitTests" + File.separator;
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testPushPull() throws Exception {
        String remoteURL = "https://github.com/TheElegitTeam/PushPullTests.git";

        // Repo that will push
        Path repoPathPush = directoryPath.resolve("pushpull1");
        ClonedRepoHelper helperPush = new ClonedRepoHelper(repoPathPush, remoteURL);
        assertNotNull(helperPush);

        // Repo that will pull
        Path repoPathPull = directoryPath.resolve("pushpull2");
        ClonedRepoHelper helperPull = new ClonedRepoHelper(repoPathPull, remoteURL);
        assertNotNull(helperPull);

        // Update the file, then commit and push
        Path readmePath = repoPathPush.resolve("README.md");
        System.out.println(readmePath);

//        // Need to make the "newFile.txt" actually exist:
//        Files.createFile(readPath);
//
//        try(PrintWriter newPathTextWriter = new PrintWriter(newPath.toString() )){
//            newPathTextWriter.println("Dummy text for the new file to commit");
//        }
//
//        this.helper.addFilePath(newPath);
//        this.helper.commit("Added a new file in a unit test!");
//
//        assertTrue(this.helper.hasUnpushedCommits());

    }


}
