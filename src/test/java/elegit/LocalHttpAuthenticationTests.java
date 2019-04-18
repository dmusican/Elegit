/*
 * Note that this code contains some content from the JGit testing libraries, as is allowed via the following
 * license, which we repeat as required:
 */

/*
 *
 * Copyright (C) 2010, 2017 Google Inc.
 * and other copyright owners as documented in the project's IP log.
 *
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Distribution License v1.0 which
 * accompanies this distribution, is reproduced below, and is
 * available at http://www.eclipse.org/org/documents/edl-v10.php
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Eclipse Foundation, Inc. nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package elegit;

import elegit.exceptions.ConflictingFilesException;
import elegit.exceptions.ExceptionAdapter;
import elegit.exceptions.MissingRepoException;
import elegit.exceptions.NoTrackingException;
import elegit.gui.ClonedRepoHelperBuilder;
import elegit.gui.RepoHelperBuilder;
import elegit.models.AuthMethod;
import elegit.models.BranchHelper;
import elegit.models.BranchModel;
import elegit.models.ClonedRepoHelper;
import elegit.models.CommitHelper;
import elegit.models.ExistingRepoHelper;
import elegit.models.LocalBranchHelper;
import elegit.models.RemoteBranchHelper;
import elegit.models.RepoHelper;
import elegit.sshauthentication.ElegitUserInfoTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.http.server.GitServlet;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.junit.http.HttpTestCase;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.HttpTransport;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.transport.http.apache.HttpClientConnectionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import sharedrules.JGitTestingRepositoryRule;
import sharedrules.TestUtilities;
import sharedrules.TestingLogPathRule;
import sharedrules.TestingRemoteAndLocalReposRule;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LocalHttpAuthenticationTests extends HttpTestCase {


    @ClassRule
    public static final TestingLogPathRule testingLogPath = new TestingLogPathRule();

    @Rule
    public final TestingRemoteAndLocalReposRule testingRemoteAndLocalRepos =
            new TestingRemoteAndLocalReposRule(false);

    @Rule
    public final JGitTestingRepositoryRule jGitTestingRepositoryRule = new JGitTestingRepositoryRule();

    private URIish remoteURI;
    private URIish authURI;
    private TestRepository<Repository> jGitTestRepo;

    private static final String EDIT_STRING = "Lorem Ipsum";

    public LocalHttpAuthenticationTests() {
		HttpTransport.setConnectionFactory(new HttpClientConnectionFactory());
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();

		TestUtilities.setupTestEnvironment();

		jGitTestRepo = jGitTestingRepositoryRule.getJgitTestRepo();
		remoteURI = jGitTestingRepositoryRule.getRemoteURI();
		authURI = jGitTestingRepositoryRule.getAuthURI();

        // Set up secondary remote repo
        Path remoteFull = testingRemoteAndLocalRepos.getRemoteFull();
        System.out.println("remote full is " + remoteFull);

        Repository db = new FileRepository(remoteFull.toString());

        Path remoteFilePath = remoteFull.resolve("file.txt");
        Files.write(remoteFilePath, "hello".getBytes());
        //ArrayList<Path> paths = new ArrayList<>();
        //paths.add(remoteFilePath);
        ExistingRepoHelper helperServer = new ExistingRepoHelper(remoteFull, null);
        helperServer.addFilePathTest(remoteFilePath);
        helperServer.commit("Initial unit test commit");

        System.out.println("Location is " + db.getDirectory());
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        TestUtilities.cleanupTestEnvironment();
    }


    @Test
    public void testCloneHttpNoPassword() throws Exception {

        Path localFull = testingRemoteAndLocalRepos.getLocalFull();

        System.out.println(remoteURI);

        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider("", "");
        ClonedRepoHelper helper = new ClonedRepoHelper(localFull, credentials);
        assertNotNull(helper);
        helper.obtainRepository(remoteURI.toString());

    }


    @Test
	public void testCloneHttpWithUsernamePassword() throws Exception {
        Path localFull = testingRemoteAndLocalRepos.getLocalFull();

        System.out.println(authURI);

        UsernamePasswordCredentialsProvider credentials =
                new UsernamePasswordCredentialsProvider("agitter", "letmein");
        System.out.println("Local path is " + localFull);
        ClonedRepoHelper helper = new ClonedRepoHelper(localFull, credentials);
        assertNotNull(helper);
        helper.obtainRepository(authURI.toString());


        assertEquals(helper.getCompatibleAuthentication(), AuthMethod.HTTP);
        helper.fetch(false);
        Path fileLocation = localFull.resolve("file.txt");
        //console.info("File location is " + fileLocation);
        FileWriter fw = new FileWriter(fileLocation.toString(), true);
        fw.write("1");
        fw.close();
        ArrayList<Path> paths = new ArrayList<>();
        paths.add(fileLocation.getFileName());
        helper.addFilePaths(paths);
        helper.commit("Appended to file");
        PushCommand command = helper.prepareToPushAll();
        helper.pushAll(command);

	}

    @Test
    public void testLsHttpPublicUsernamePassword() throws Exception {

        UsernamePasswordCredentialsProvider credentials =
                new UsernamePasswordCredentialsProvider("", "");

        TransportCommand command = Git.lsRemoteRepository().setRemote(remoteURI.toString());
        RepoHelper helper = new RepoHelper();
        helper.setOwnerAuth(credentials);
        helper.wrapAuthentication(command);
        command.call();
    }

    @Test
    public void testLsHttpPublicUsernamePasswordEmpty() throws Exception {

        UsernamePasswordCredentialsProvider credentials =
                new UsernamePasswordCredentialsProvider("a", "asdas");

        TransportCommand command = Git.lsRemoteRepository().setRemote(remoteURI.toString());
        RepoHelper helper = new RepoHelper();
        helper.setOwnerAuth(credentials);
        helper.wrapAuthentication(command);
        command.call();
    }

    @Test
    public void testLsHttpPrivateUsernamePassword() throws Exception {

        System.out.println(authURI);
        UsernamePasswordCredentialsProvider credentials =
                new UsernamePasswordCredentialsProvider("agitter", "letmein");

        TransportCommand command = Git.lsRemoteRepository().setRemote(authURI.toString());
        RepoHelper helper = new RepoHelper();
        helper.setOwnerAuth(credentials);
        helper.wrapAuthentication(command);
        command.call();
    }

    @Test
    public void testCloneRepositoryPublicWithChecksHttpUsernamePassword() throws Exception {

        RepoHelperBuilder.AuthDialogResponse response =
                new RepoHelperBuilder.AuthDialogResponse(null, "", "", false);

        Path localFull = testingRemoteAndLocalRepos.getLocalFull();
        ClonedRepoHelperBuilder.cloneRepositoryWithChecks(remoteURI.toString(), localFull, response,
                                                          new ElegitUserInfoTest(), null, null);

    }

    @Test
    public void testCloneRepositoryPrivateWithChecksHttpUsernamePassword() throws Exception {

        RepoHelperBuilder.AuthDialogResponse response =
                new RepoHelperBuilder.AuthDialogResponse(null, "agitter", "letmein", false);

        Path localFull = testingRemoteAndLocalRepos.getLocalFull();
        ClonedRepoHelperBuilder.cloneRepositoryWithChecks(authURI.toString(), localFull, response,
                                                          new ElegitUserInfoTest(), null, null);

    }

    /* This is a version of the test where the username password is entered incorrectly at first, which works for a
     * public repo, but later needs to be specified for a push.
     * and needs to be fixed later.
     */
    @Test
    public void testHttpBadUsernamePassword() throws Exception {

        System.out.println("remote " + remoteURI);
        System.out.println("auth " + authURI);
        Path localFull = testingRemoteAndLocalRepos.getLocalFull();
        System.out.println("local " + localFull);
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider("", "");
        ClonedRepoHelper helper = new ClonedRepoHelper(localFull, credentials);
        helper.obtainRepository(remoteURI.toString());
        assertEquals(helper.getCompatibleAuthentication(), AuthMethod.HTTP);
        helper.fetch(false);
        Path fileLocation = localFull.resolve("README.md");
        System.out.println(fileLocation);
        FileWriter fw = new FileWriter(fileLocation.toString(), true);
        fw.write("1");
        fw.close();
        helper.addFilePathTest(fileLocation);
        helper.commit("Appended to file");

        // https://stackoverflow.com/questions/12799573/add-remote-via-jgit
        Git git = new Git(helper.getRepo());
        StoredConfig config = git.getRepository().getConfig();
        config.setString("remote","origin","url",authURI.toString());
        config.save();
        credentials = new UsernamePasswordCredentialsProvider("agitter", "letmein");
        helper.setOwnerAuth(credentials);
        PushCommand command = helper.prepareToPushAll();
        helper.pushAll(command);
        String commitName = helper.getAllCommits().iterator().next().getName();
        helper.getTagModel().tag("aTestTag", commitName);
        command = helper.prepareToPushTags(true);
        helper.pushTags(command);
    }

    // Test to make sure creating a local branch lets us push and
    // that pushing will create the new branch.
    @Test
    public void testResetFile() throws Exception {

        // Repo that will commit to master
        Path repoPath = testingRemoteAndLocalRepos.getLocalFull();
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider("agitter",
                                                                                                  "letmein");
        ClonedRepoHelper helper = new ClonedRepoHelper(repoPath, credentials);
        assertNotNull(helper);
        helper.obtainRepository(authURI.toString());


        Git git = new Git(helper.getRepo());

        /* ********************* FILE RESET SECTION ********************* */
        // Single file reset
        Path filePath = repoPath.resolve("modify.txt");
        Files.write(filePath, EDIT_STRING.getBytes(), StandardOpenOption.APPEND);
        helper.addFilePathTest(filePath);
        // Check that the file is staged
        assertEquals(1,git.status().call().getChanged().size());
        // Reset the file and check that it worked
        helper.reset(filePath);
        assertEquals(0,git.status().call().getChanged().size());

        // Multiple file reset
        Path readPath = repoPath.resolve("README.md");
        Files.write(readPath, EDIT_STRING.getBytes(), StandardOpenOption.APPEND);
        ArrayList<Path> paths = new ArrayList<>();
        paths.add(filePath);
        paths.add(readPath);
        // Add both files and check that they are staged.
        helper.addFilePathsTest(paths, false);
        assertEquals(2,git.status().call().getChanged().size());
        // Reset both the files and check that it worked
        helper.reset(paths);
        assertEquals(0,git.status().call().getChanged().size());

        /* ********************* COMMIT RESET SECTION ********************* */
        helper.getBranchModel().updateAllBranches();
        String oldHead = helper.getBranchModel().getCurrentBranch().getCommit().getName();

        modifyAddFile(helper, filePath);
        helper.commit("Modified a file");

        helper.getBranchModel().updateAllBranches();
        assertEquals(false, oldHead.equals(helper.getBranchModel().getCurrentBranch().getCommit().getName()));

        // hard reset (to previous commit)
        helper.reset("HEAD~1", ResetCommand.ResetType.HARD);
        helper.getBranchModel().updateAllBranches();
        // Check that the files in the index and working directory got reset
        assertEquals(0, git.status().call().getModified().size()
                + git.status().call().getChanged().size());
        assertEquals(oldHead, helper.getBranchModel().getCurrentBranch().getCommit().getName());

        // mixed reset (to HEAD)
        // modify and add file, then reset to head
        modifyAddFile(helper, filePath);
        helper.reset("HEAD", ResetCommand.ResetType.MIXED);
        helper.getBranchModel().updateAllBranches();
        // Check that the file in the index got reset
        assertEquals(0, git.status().call().getChanged().size());
        assertEquals(1, git.status().call().getModified().size());


        // soft reset (to HEAD~1)
        // commit, then put changes in index and wd, check that they stayed
        helper.addFilePathTest(filePath);
        helper.commit("modified file");
        modifyFile(readPath);
        modifyAddFile(helper, filePath);
        helper.reset("HEAD~1", ResetCommand.ResetType.SOFT);
        helper.getBranchModel().updateAllBranches();

        assertEquals(oldHead, helper.getBranchModel().getCurrentBranch().getCommit().getName());
        assertEquals(1, git.status().call().getChanged().size());
        assertEquals(1, git.status().call().getModified().size());
    }

    // Test to make sure creating a local branch lets us push and
    // that pushing will create the new branch.
    @Test
    public void testRevertFile() throws Exception {
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider("agitter",
                                                                                                  "letmein");

        // Repo that will commit to master
        Path repoPath = testingRemoteAndLocalRepos.getLocalFull();
        ClonedRepoHelper helper = new ClonedRepoHelper(repoPath, credentials);
        assertNotNull(helper);
        helper.obtainRepository(authURI.toString());


        Path filePath = repoPath.resolve("modify.txt");


        /* ********************* SINGLE REVERT SECTION ********************* */
        // make a commit, then revert it, check that changes occurred

        helper.getBranchModel().refreshHeadIds();
        String oldHead = helper.getBranchModel().getCurrentBranchHead().getName();

        modifyAddFile(helper, filePath);
        helper.commit("Modified file #1");
        helper.updateModel();
        helper.getBranchModel().refreshHeadIds();
        assertEquals(false, helper.getBranchModel().getCurrentBranchHead().getName().equals(oldHead));
        helper.revert(helper.getBranchModel().getCurrentBranchHead());
        helper.updateModel();
        helper.getBranchModel().refreshHeadIds();
        CommitHelper firstRevert = helper.getBranchModel().getCurrentBranchHead();
        // The EDIT_TEXT should have been reverted
        assertEquals(1, Files.readAllLines(filePath).size());
        // And a new HEAD should be there
        assertEquals(false, helper.getBranchModel().getCurrentBranchHead().getName().equals(oldHead));

        /* ********************* MULTIPLE REVERT SECTION ********************* */
        // make 2 more commits revert first revert and third commit, check content
        Path readPath = repoPath.resolve("README.md");
        modifyAddFile(helper, readPath, "Keep Text\n");
        helper.commit("Modified file #2");

        modifyAddFile(helper, readPath, "Revert Text");
        helper.commit("Modified file #3");
        helper.updateModel();
        List<CommitHelper> commitsToRevert = new ArrayList<>();
        helper.getBranchModel().refreshHeadIds();
        commitsToRevert.add(firstRevert);
        commitsToRevert.add(helper.getBranchModel().getCurrentBranchHead());

        // Revert and check content
        helper.revertHelpers(commitsToRevert);
        helper.getBranchModel().refreshHeadIds();
        assertEquals(3, Files.readAllLines(readPath).size());
        assertEquals("Keep Text", Files.readAllLines(readPath).get(2));
        assertEquals(EDIT_STRING, Files.readAllLines(filePath).get(1));
    }

    @Test
    public void testStash() throws Exception {
        Path repoPath = testingRemoteAndLocalRepos.getLocalFull();
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider("",
                                                                                                  "");
        ClonedRepoHelper helper = new ClonedRepoHelper(repoPath, credentials);
        assertNotNull(helper);
        helper.obtainRepository(remoteURI.toString());

        Path modify = repoPath.resolve("modify.txt");
        modifyFile(modify);

        Path untracked = Paths.get(repoPath.toString(), "new.txt");
        Files.createFile(untracked);

        Git git = new Git(helper.getRepo());

        Status status = git.status().call();
        assertEquals(1, status.getModified().size());
        assertEquals(1, status.getUntracked().size());

        // Save only tracked files
        helper.stashSave(false);

        status = git.status().call();
        assertEquals(0, status.getModified().size());
        assertEquals(1, status.getUntracked().size());

        // Stash untracked files
        helper.stashSave(true);

        status = git.status().call();
        assertEquals(0, status.getModified().size());
        assertEquals(0, status.getUntracked().size());

        List<CommitHelper> stashList = helper.stashList();
        assertEquals(2, stashList.size());

        // Apply a given stash
        helper.stashApply(stashList.get(0).getName(), false);

        status = git.status().call();
        assertEquals(0, status.getModified().size());
        assertEquals(1, status.getUntracked().size());

        // Clear all the stashes
        helper.stashClear();

        stashList = helper.stashList();
        assertEquals(0, stashList.size());
    }

    private void modifyFile(Path file) throws Exception {
        Files.write(file, EDIT_STRING.getBytes(), StandardOpenOption.APPEND);
    }

    private void modifyAddFile(RepoHelper helper, Path file) throws Exception {
        Files.write(file, EDIT_STRING.getBytes(), StandardOpenOption.APPEND);
        helper.addFilePathTest(file);
    }

    private void modifyAddFile(RepoHelper helper, Path file, String editString) throws Exception {
        Files.write(file, editString.getBytes(), StandardOpenOption.APPEND);
        helper.addFilePathTest(file);
    }

    @Test
    public void testPushPullBothClonedExisting() throws Exception {
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider("agitter",
                                                                                                  "letmein");

        Path directoryPath = testingRemoteAndLocalRepos.getDirectoryPath();

        // Repo that will push
        Path repoPathPush = directoryPath.resolve("pushpull1");
        ClonedRepoHelper helperPush = new ClonedRepoHelper(repoPathPush, credentials);
        assertNotNull(helperPush);
        helperPush.obtainRepository(authURI.toString());

        // Repo that will pull
        Path repoPathPull = directoryPath.resolve("pushpull2");
        ClonedRepoHelper clonedHelperPull = new ClonedRepoHelper(repoPathPull, credentials);
        assertNotNull(clonedHelperPull);
        clonedHelperPull.obtainRepository(authURI.toString());
        ExistingRepoHelper existingHelperPull = new ExistingRepoHelper(repoPathPull, new ElegitUserInfoTest());
        existingHelperPull.setOwnerAuth(credentials);

        // Update the file, then commit and push
        Path readmePath = repoPathPush.resolve("README.md");
        System.out.println(readmePath);
        String timestamp = "testPushPullBothClonedExisting " + (new Date()).toString() + "\n";
        Files.write(readmePath, timestamp.getBytes(), StandardOpenOption.APPEND);
        helperPush.addFilePathTest(readmePath);
        helperPush.commit("added a character");
        PushCommand command = helperPush.prepareToPushAll();
        helperPush.pushAll(command);

        // Now do the pull (well, a fetch)
        existingHelperPull.fetch(false);
        existingHelperPull.mergeFromFetch();
    }

    @Test
    public void cloneThenPushTest() throws Exception {
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider("agitter",
                                                                                                  "letmein");

        Path directoryPath = testingRemoteAndLocalRepos.getDirectoryPath();


        Path repoPathPush = directoryPath.resolve("clonepush");
        ClonedRepoHelper helperPush = new ClonedRepoHelper(repoPathPush, credentials);
        assertNotNull(helperPush);
        helperPush.obtainRepository(authURI.toString());

        // Update the file, then commit and push
        Path readmePath = repoPathPush.resolve("README.md");
        System.out.println(readmePath);
        String timestamp = (new Date()).toString() + "\n";
        Files.write(readmePath, timestamp.getBytes(), StandardOpenOption.APPEND);
        helperPush.addFilePathTest(readmePath);
        helperPush.commit("added a character");
        PushCommand push = helperPush.prepareToPushAll();
        helperPush.pushAll(push);

    }

    // Test to make sure creating a local branch lets us push and
    // that pushing will create the new branch.
    @Test
    public void testBranchPushAndDelete() throws Exception {
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider("agitter",
                                                                                                  "letmein");

        Path directoryPath = testingRemoteAndLocalRepos.getDirectoryPath();

        // Repo that will commit to master
        Path repoPathPush = directoryPath.resolve("pusher");
        ClonedRepoHelper helperPush = new ClonedRepoHelper(repoPathPush, credentials);
        assertNotNull(helperPush);
        helperPush.obtainRepository(authURI.toString());


        /* ********************* BRANCH AND PUSH SECTION ********************* */
        // Check that a previous test wasn't interrupted, if so make sure our branch is not there
        for (RemoteBranchHelper helper : helperPush.getBranchModel().getRemoteBranchesTyped()) {
            if (helper.getRefName().contains("new_branch")) {
                helperPush.getBranchModel().deleteRemoteBranch(helper);
            }
        }

        // Make a new branch and push it
        helperPush.getBranchModel().createNewLocalBranch("new_branch");
        helperPush.getBranchModel().getBranchByName(BranchModel.BranchType.LOCAL, "new_branch").checkoutBranch();
        helperPush.updateModel();
        // Check that we can push
        assertEquals(true, helperPush.canPush());

        PushCommand push = helperPush.prepareToPushCurrentBranch(true);
        helperPush.pushCurrentBranch(push);
        helperPush.updateModel();
        // Check that there is a remote branch now
        assertEquals(true, helperPush.getBranchModel().getRemoteBranchesTyped().toString().contains("new_branch"));



        // Test that we can delete the branch too
        helperPush.getBranchModel().updateRemoteBranches();
        BranchHelper remoteBranchHelper = helperPush.getBranchModel().getBranchByName(BranchModel.BranchType.REMOTE, "origin/new_branch");
        helperPush.getBranchModel().deleteRemoteBranch((RemoteBranchHelper) remoteBranchHelper);

        // Check that the branch was deleted
        assertEquals(true, helperPush.getBranchModel().getCurrentRemoteBranch()==null);
    }

    @Test
    public void testSwitchingRepoInvisCommit() throws Exception {
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider("agitter",
                                                                                                  "letmein");

        Path directoryPath = testingRemoteAndLocalRepos.getDirectoryPath();

        // First copy of the repo
        Path repoPath1 = directoryPath.resolve("repo1");
        ClonedRepoHelper repo1 = new ClonedRepoHelper(repoPath1, credentials);
        assertNotNull(repo1);
        repo1.obtainRepository(authURI.toString());

        CommitHelper repo1OldHead = repo1.getCommit("master");
        assertNotNull(repo1OldHead);

        // Make a change in repo1 and commit it
        File file = Paths.get(repoPath1.toString(), "modify.txt").toFile();
        assertTrue(file.exists());

        try(PrintWriter fileTextWriter = new PrintWriter( file )){
            fileTextWriter.println("Add a line");
        }

        repo1.addFilePathTest(file.toPath());
        repo1.commit("Modified modify.txt in a unit test!");

        CommitHelper repo1NewHead = repo1.getCommit("master");
        assertNotNull(repo1NewHead);

        // Second copy of the repo
        Path repoPath2 = directoryPath.resolve("repo2");
        ClonedRepoHelper repo2 = new ClonedRepoHelper(repoPath2, credentials);
        assertNotNull(repo2);
        repo2.obtainRepository(authURI.toString());

        CommitHelper repo2OldHead = repo2.getCommit("master");
        assertNotNull(repo2OldHead);
        assertEquals(repo1OldHead.getName(), repo2OldHead.getName());

        // Push the previous commit
        PushCommand push = repo1.prepareToPushAll();
        repo1.pushAll(push);

        // Fetch into the second repo
        repo2.fetch(false);
        repo2.mergeFromFetch();

        CommitHelper repo2NewHead = repo2.getCommit("master");
        assertNotNull(repo2NewHead);
        assertEquals(repo1NewHead.getName(), repo2NewHead.getName());

        repo1.updateModel();
        Set<CommitHelper> repo1LocalCommits = repo1.getLocalCommits();
        Set<CommitHelper> repo1RemoteCommits = repo1.getRemoteCommits();

        assertTrue(repo1LocalCommits.contains(repo1OldHead));
        assertTrue(repo1RemoteCommits.contains(repo1OldHead));
        assertTrue(repo1LocalCommits.contains(repo1NewHead));
        assertTrue(repo1RemoteCommits.contains(repo1NewHead));

        repo2.updateModel();
        Set<CommitHelper> repo2LocalCommits = repo2.getLocalCommits();
        Set<CommitHelper> repo2RemoteCommits = repo2.getRemoteCommits();

        assertTrue(repo2LocalCommits.contains(repo2OldHead));
        assertTrue(repo2RemoteCommits.contains(repo2OldHead));
        assertTrue(repo2LocalCommits.contains(repo2NewHead));
        assertTrue(repo2RemoteCommits.contains(repo2NewHead));

    }

    @Test
    public void testFastForwardMergeFromFetch() throws Exception {
        Path remoteFull = jGitTestRepo.getRepository().getDirectory().toPath();
        ExistingRepoHelper helperServer = new ExistingRepoHelper(remoteFull, null);
        helperServer.getBranchModel().createNewLocalBranch("new_branch");

        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider("agitter",
                                                                                                  "letmein");

        Path directoryPath = testingRemoteAndLocalRepos.getDirectoryPath();


        // Repo that will commit to new_branch
        Path repoPathPush = directoryPath.resolve("pusher");
        ClonedRepoHelper helperPush = new ClonedRepoHelper(repoPathPush, credentials);
        assertNotNull(helperPush);
        helperPush.obtainRepository(authURI.toString());

        // Repo that will fetch and mergefromfetch
        Path repoPathFetch = directoryPath.resolve("fetcher");
        ClonedRepoHelper helperFetch = new ClonedRepoHelper(repoPathFetch, credentials);
        assertNotNull(helperPush);
        helperFetch.obtainRepository(authURI.toString());


        /* ********************* EDIT AND PUSH SECTION ********************* */
        RemoteBranchHelper remote_helper_push, remote_helper_fetch;
        LocalBranchHelper new_branch_push_helper, master_push_helper, new_branch_fetch_helper;

        // Find the remote 'new_branch' for push repo
        remote_helper_push = (RemoteBranchHelper) helperPush
                .getBranchModel().getBranchByName(BranchModel.BranchType.REMOTE, "origin/new_branch");
        assertNotNull(remote_helper_push);
        // Track new_branch and check it out
        new_branch_push_helper = helperPush.getBranchModel().trackRemoteBranch(remote_helper_push);
        new_branch_push_helper.checkoutBranch();

        // Make some different changes in new_branch
        Path filePath = repoPathPush.resolve("README.md");
        String newBranchLine = "Line for new branch\n";
        Files.write(filePath, newBranchLine.getBytes(), StandardOpenOption.APPEND);
        helperPush.addFilePathTest(filePath);

        // Commit changes in new_branch
        helperPush.commit("added line in new_branch");

        // Make some changes in master
        master_push_helper = (LocalBranchHelper) helperPush.getBranchModel().getBranchByName(BranchModel.BranchType.LOCAL, "master");
        master_push_helper.checkoutBranch();
        filePath = repoPathPush.resolve("README.md");
        newBranchLine = "Line for master\n";
        Files.write(filePath, newBranchLine.getBytes(), StandardOpenOption.APPEND);
        helperPush.addFilePathTest(filePath);

        // Commit the changes in master and push
        helperPush.commit("added line in master");

        PushCommand push = helperPush.prepareToPushAll();
        helperPush.pushAll(push);


        /* ******************** FETCH AND MERGE SECTION ******************** */

        // Checkout new_branch
        remote_helper_fetch = (RemoteBranchHelper) helperFetch.getBranchModel().getBranchByName(BranchModel.BranchType.REMOTE, "origin/new_branch");
        assertNotNull(remote_helper_fetch);
        // Track new_branch and check it out
        new_branch_fetch_helper = helperFetch.getBranchModel().trackRemoteBranch(remote_helper_fetch);
        new_branch_fetch_helper.checkoutBranch();

        // Fetch changes
        helperFetch.fetch(false);

        // Merge from the fetch
        boolean is_fast_forward = true;
        try {
            is_fast_forward = helperFetch.mergeFromFetch() == MergeResult.MergeStatus.FAST_FORWARD;
        } catch (IOException | GitAPIException | MissingRepoException e) {
            throw new ExceptionAdapter(e);
        }
        catch (ConflictingFilesException e) {
            is_fast_forward = false;
        }

        // Check that new_branch was fast-forwarded instead of merged with master
        assert(is_fast_forward);
    }


    @Test
    public void testLocalBranchMerge() throws Exception {
        Path directoryPath = testingRemoteAndLocalRepos.getDirectoryPath();
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider("agitter",
                                                                                                  "letmein");

        // Repo that will commit to master
        Path repoPathPush = directoryPath.resolve("pusher");
        ClonedRepoHelper helperPush = new ClonedRepoHelper(repoPathPush, credentials);
        assertNotNull(helperPush);
        helperPush.obtainRepository(authURI.toString());

        // Repo that will fetch and mergefromfetch
        Path repoPathFetch = directoryPath.resolve("fetcher");
        ClonedRepoHelper helperFetch = new ClonedRepoHelper(repoPathFetch, credentials);
        assertNotNull(helperPush);
        helperFetch.obtainRepository(authURI.toString());


        /* ********************* EDIT AND PUSH SECTION ********************* */
        // Make some changes in master in pusher
        Path filePath = repoPathPush.resolve("README.md");
        String timestamp = "testLocalBranchPush " + (new Date()).toString() + "\n";
        Files.write(filePath, timestamp.getBytes(), StandardOpenOption.APPEND);
        helperPush.addFilePathTest(filePath);

        // Commit changes in master in pusher and push
        helperPush.commit("added line in master");
        PushCommand command = helperPush.prepareToPushAll();
        helperPush.pushAll(command);


        // Make a new branch in fetcher and check it out
        LocalBranchHelper new_branch_fetch_helper =
                helperFetch.getBranchModel().createNewLocalBranch("new_branch_name");
        new_branch_fetch_helper.checkoutBranch();

        // Make some changes in new_branch in pusher
        filePath = repoPathFetch.resolve("README.md");
        timestamp = "testLocalBranchFetch " + (new Date()).toString() + "\n";
        Files.write(filePath, timestamp.getBytes(), StandardOpenOption.APPEND);
        helperFetch.addFilePathTest(filePath);
        /* ******************** FETCH AND MERGE SECTION ******************** */

        // Fetch changes
        helperFetch.fetch(false);

        // Merge from the fetch
        boolean local_branch_is_tracked = false;
        try {
            helperFetch.mergeFromFetch();
        } catch (IOException | GitAPIException | MissingRepoException | ConflictingFilesException e) {
            throw new ExceptionAdapter(e);
        }
        catch (NoTrackingException e) {
            local_branch_is_tracked = true;
        } catch (Exception e) {
            throw new ExceptionAdapter(e);
        }

        // Check that new_branch was fast-forwarded instead of merged with master
        assert(local_branch_is_tracked);

    }

    @Test
    public void testClonedRepoHelper() throws Exception {
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider("", "");
        Path repoPath = testingRemoteAndLocalRepos.getLocalFull();
        ClonedRepoHelper helper = new ClonedRepoHelper(repoPath, credentials);
        helper.obtainRepository(remoteURI.toString());
        assertNotNull(helper);
        assertTrue(helper.exists());
        assertNotNull(helper.getRepo());
    }

    @Test
    public void testAddFileAndCommit() throws Exception {
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider("", "");
        Path repoPath = testingRemoteAndLocalRepos.getLocalFull();
        ClonedRepoHelper helper = new ClonedRepoHelper(repoPath, credentials);
        helper.obtainRepository(remoteURI.toString());

        assertFalse(helper.getAheadCount()>0);

        Path directoryPath = testingRemoteAndLocalRepos.getDirectoryPath();
        Path newPath = Paths.get(directoryPath.toString(), "new.txt");

        // Need to make the "newFile.txt" actually exist:
        Files.createFile(newPath);

        try(PrintWriter newPathTextWriter = new PrintWriter( newPath.toString() )){
            newPathTextWriter.println("Dummy text for the new file to commit");
        }

        helper.addFilePathTest(newPath);
        helper.commit("Added a new file in a unit test!");

        assertTrue(helper.getAheadCount()>0);

    }

    @Test
    public void testFastForward() throws Exception {
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider("agitter",
                                                                                                  "letmein");

        Path directoryPath = testingRemoteAndLocalRepos.getDirectoryPath();

        // Repo that will commit to make a fast forward commit
        Path repoPathFast = directoryPath.resolve("fastforward");
        ClonedRepoHelper helperFast = new ClonedRepoHelper(repoPathFast, credentials);
        assertNotNull(helperFast);
        helperFast.obtainRepository(authURI.toString());

        // Create the branch 'fast_branch'
        helperFast.getBranchModel().createNewLocalBranch("fast_branch");


        LocalBranchHelper fastBranch = (LocalBranchHelper) helperFast
                .getBranchModel().getBranchByName(BranchModel.BranchType.LOCAL, "fast_branch");
        Git git = new Git(helperFast.getRepo());
        // Just push all untracked local branches
        PushCommand command = helperFast.prepareToPushAll(untrackedLocalBranches -> untrackedLocalBranches);
        helperFast.pushAll(command);


        // Track fast_branch and check it out
        fastBranch.checkoutBranch();
        helperFast.updateModel();

        // Update the file in fast_branch
        Path filePath = repoPathFast.resolve("modify.txt");
        String timestamp = (new Date()).toString() + "\n";
        Files.write(filePath, timestamp.getBytes(), StandardOpenOption.APPEND);
        helperFast.addFilePathTest(filePath);

        // Commit changes in fast_branch and push
        helperFast.commit("added a character");
        command = helperFast.prepareToPushAll();
        helperFast.pushAll(command);

        //Checkout master
        LocalBranchHelper master_helper = (LocalBranchHelper) helperFast
                .getBranchModel().getBranchByName(BranchModel.BranchType.LOCAL, "master");
        master_helper.checkoutBranch();

        // Merge fast_forward into master
        helperFast.getBranchModel().mergeWithBranch(fastBranch);

        // Check that Elegit recognizes there are unpushed commits
        assertEquals(true, helperFast.getAheadCount()>0);

        // Push changes
        command = helperFast.prepareToPushAll();
        helperFast.pushAll(command);

    }

    @Test
    public void testFastForwardCommitCanPush() throws Exception {
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider("agitter",
                                                                                                  "letmein");

        Path directoryPath = testingRemoteAndLocalRepos.getDirectoryPath();
        // Repo that will commit to make a fast forward commit
        Path repoPathFast = directoryPath.resolve("fastforward");
        ClonedRepoHelper helperFast = new ClonedRepoHelper(repoPathFast, credentials);
        assertNotNull(helperFast);
        helperFast.obtainRepository(authURI.toString());


        // Create the branches
        helperFast.getBranchModel().createNewLocalBranch("can_push");
        LocalBranchHelper fastBranch = (LocalBranchHelper) helperFast
                .getBranchModel().getBranchByName(BranchModel.BranchType.LOCAL, "can_push");
        // Just push all untracked local branches
        PushCommand command = helperFast.prepareToPushAll(untrackedLocalBranches -> untrackedLocalBranches);
        helperFast.pushAll(command);

        helperFast.getBranchModel().createNewLocalBranch("can_pushb");
        LocalBranchHelper fastBranchb = (LocalBranchHelper) helperFast
                .getBranchModel().getBranchByName(BranchModel.BranchType.LOCAL, "can_pushb");
        // Just push all untracked local branches
        command = helperFast.prepareToPushAll(untrackedLocalBranches -> untrackedLocalBranches);
        helperFast.pushAll(command);


        // Track can_push and check it out
        fastBranch.checkoutBranch();
        helperFast.updateModel();

        // Update the file in can_push
        Path filePath = repoPathFast.resolve("modify.txt");
        String timestamp = (new Date()).toString() + "\n";
        Files.write(filePath, timestamp.getBytes(), StandardOpenOption.APPEND);
        helperFast.addFilePathTest(filePath);

        // Commit changes in can_push and push
        helperFast.commit("added a character");
        command = helperFast.prepareToPushAll();
        helperFast.pushAll(command);

        // Track can_pushb and check it out
        fastBranchb.checkoutBranch();

        // Merge can_push into can_pushb
        helperFast.getBranchModel().mergeWithBranch(fastBranch);

        // Check that Elegit recognizes there are unpushed commits
        assertEquals(true, helperFast.getAheadCount()>0);

        // Push changes
        command = helperFast.prepareToPushAll();
        helperFast.pushAll(command);

    }

}
