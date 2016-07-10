package elegit;

import com.sun.org.apache.regexp.internal.RE;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * @author shangd
 */
public class BranchManagerUiUpdateTest {

    private static final String REMOTE_URL = "https://github.com/shangdrk/BranchManagerUiUpdateTestRepo";
    private static final String BRANCH_NAME = "random";

    Path logPath;

    private Path directoryPath;
    private Path repoPath;
    private ClonedRepoHelper helper;

    @Before
    public void setUp() throws Exception {
        initializeLogger();
        cloneTestRepo();
    }

    @After
    public void tearDown() throws Exception {
        removeAllFilesFromDirectory(directoryPath.toFile());
    }

    void initializeLogger() {
        // Create a temp directory for the files to be placed in
        try {
            this.logPath = Files.createTempDirectory("elegitLogs");
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.logPath.toFile().deleteOnExit();
        System.setProperty("logFolder", logPath.toString());
    }

    void cloneTestRepo() {
        try {
            directoryPath = Files.createTempDirectory("branchManagerUiUpdateTest");
        } catch (IOException e) {
            e.printStackTrace();
        }
        directoryPath.toFile().deleteOnExit();

        repoPath = directoryPath.resolve("testRepo");
        try {
            helper = new ClonedRepoHelper(repoPath, REMOTE_URL, new UsernamePasswordCredentialsProvider("",""));
            helper.obtainRepository(REMOTE_URL);
        } catch (Exception e) {
            e.printStackTrace();
        }

        assertNotNull(helper);
    }

    void removeAllFilesFromDirectory(File dir) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) removeAllFilesFromDirectory(file);
            file.delete();
        }
    }

    @Test
    public void testMergeSelectedBranchWithCurrent() throws Exception {
        // first fetch from remote
        RefSpec fetchSpec = new RefSpec
                ("+refs/heads/*:refs/remotes/origin/*");
        new Git(helper.getRepo()).fetch().setRefSpecs(fetchSpec).call();

        // start tracking the remote branch "random"
        new Git(helper.getRepo()).checkout().
                setCreateBranch(true).
                setForce(true).
                setName("random").
                setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).
                setStartPoint("origin/random").
                call();

        // make changes to local file and commit
        File file = Paths.get(this.repoPath.toString(), "test.txt").toFile();
        assertTrue(file.exists());

        try(PrintWriter fileTextWriter = new PrintWriter( file )){
            fileTextWriter.println("Add a line to the file");
        }

        this.helper.addFilePath(file.toPath());
        this.helper.commit("Modified test.txt in a unit test!");

        new Git(helper.getRepo()).checkout().setName("master").call();

        // merge master into random
        MergeCommand merge = new Git(helper.getRepo()).merge();
        merge.include(helper.getRepo().resolve("refs/heads/random"));

        MergeResult mergeResult = merge.call();
        assertEquals(mergeResult.getMergeStatus(), MergeResult.MergeStatus.FAST_FORWARD);

        // TODO: fix this if you ever run it
        //CommitTreeController.update(helper);

        // after update, should expect the heads of two branches
        // are the same commit
        helper.getBranchModel().updateLocalBranches();
        assertEquals(helper.getBranchModel().getBranchListTyped(BranchModel.BranchType.LOCAL).size(), 2);

        String masterHead = helper.getBranchModel().getBranchListTyped(BranchModel.BranchType.LOCAL).get(0).getHeadId().getName();
        String randomHead = helper.getBranchModel().getBranchListTyped(BranchModel.BranchType.LOCAL).get(1).getHeadId().getName();

        assertEquals(masterHead, randomHead);
    }
}
