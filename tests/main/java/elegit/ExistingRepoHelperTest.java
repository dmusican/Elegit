package main.java.elegit;

import org.eclipse.jgit.api.Git;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * Created by dmusican on 2/24/16.
 */
public class ExistingRepoHelperTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }


    @Test
    public void testExistingRepoOpen() throws Exception {
        File localPath = File.createTempFile("TestGitRepo","");
        localPath.delete();

        Git git = Git.cloneRepository()
                    .setURI("https://github.com/dmusican/testrepo.git")
                    .setDirectory(localPath)
                    .call();


        String username = null;
        ExistingRepoHelper repoHelper = new ExistingRepoHelper(Paths.get(localPath.getAbsolutePath()));
        git.close();
    }
}