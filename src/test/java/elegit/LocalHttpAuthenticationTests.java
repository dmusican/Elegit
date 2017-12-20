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

import elegit.gui.ClonedRepoHelperBuilder;
import elegit.gui.RepoHelperBuilder;
import elegit.models.AuthMethod;
import elegit.models.BranchHelper;
import elegit.models.BranchModel;
import elegit.models.ClonedRepoHelper;
import elegit.models.CommitHelper;
import elegit.models.ExistingRepoHelper;
import elegit.models.RemoteBranchHelper;
import elegit.models.RepoHelper;
import elegit.sshauthentication.ElegitUserInfoTest;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.errors.RemoteRepositoryException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.http.server.GitServlet;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.junit.TestRng;
import org.eclipse.jgit.junit.http.AccessEvent;
import org.eclipse.jgit.junit.http.AppServer;
import org.eclipse.jgit.junit.http.HttpTestCase;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectIdRef;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.ReflogEntry;
import org.eclipse.jgit.lib.ReflogReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevBlob;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.FetchConnection;
import org.eclipse.jgit.transport.HttpTransport;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.TransportHttp;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.transport.http.HttpConnectionFactory;
import org.eclipse.jgit.transport.http.JDKHttpConnectionFactory;
import org.eclipse.jgit.transport.http.apache.HttpClientConnectionFactory;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.HttpSupport;
import org.eclipse.jgit.util.SystemReader;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.eclipse.jgit.util.HttpSupport.HDR_CONTENT_ENCODING;
import static org.eclipse.jgit.util.HttpSupport.HDR_CONTENT_LENGTH;
import static org.eclipse.jgit.util.HttpSupport.HDR_CONTENT_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LocalHttpAuthenticationTests extends HttpTestCase {


    @ClassRule
    public static final TestingLogPath testingLogPath = new TestingLogPath();

    @Rule
    public final TestingRemoteAndLocalRepos testingRemoteAndLocalRepos =
            new TestingRemoteAndLocalRepos(false);

    private URIish remoteURI;
    private URIish authURI;

    private static final String EDIT_STRING = "Lorem Ipsum";

    public LocalHttpAuthenticationTests() {
		HttpTransport.setConnectionFactory(new HttpClientConnectionFactory());
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();

		final TestRepository<Repository> src = createTestRepository();
		final String srcName = src.getRepository().getDirectory().getName();
		src.getRepository()
				.getConfig()
				.setBoolean(ConfigConstants.CONFIG_CORE_SECTION, null,
                            ConfigConstants.CONFIG_KEY_LOGALLREFUPDATES, true);

		GitServlet gs = new GitServlet();
		ServletContextHandler app = addNormalContext(gs, src, srcName);
		ServletContextHandler auth = addAuthContext(gs, "auth");
		server.setUp();
        remoteURI = toURIish(app, srcName);
        authURI = toURIish(auth, srcName);
        RevBlob A_txt = src.blob("A");
		RevCommit A = src.commit().add("A_txt", A_txt).create();
		RevCommit B = src.commit().parent(A).add("A_txt", "C").add("B", "B").create();
		src.update(master, B);

		RevCommit C = src.commit().parent(B)
                .add("modify.txt", "A file to be modified, then reset\n").create();
		RevCommit D = src.commit().parent(C)
                .add("README.md", "# Reset testing\nA test repo to pull\n").create();
        src.update(master, D);

        // Set up remote repo
        Path remoteFull = testingRemoteAndLocalRepos.getRemoteFull();
        System.out.println("remote full is " + remoteFull);

        Repository db = new FileRepository(remoteFull.toString());

        Path remoteFilePath = remoteFull.resolve("file.txt");
        Files.write(remoteFilePath, "hello".getBytes());
        ArrayList<Path> paths = new ArrayList<>();
        paths.add(remoteFilePath);
        ExistingRepoHelper helperServer = new ExistingRepoHelper(remoteFull, null);
        helperServer.addFilePathsTest(paths);
        helperServer.commit("Initial unit test commit");

        System.out.println("Location is " + db.getDirectory());
    }

	private ServletContextHandler addNormalContext(GitServlet gs, TestRepository<Repository> src, String srcName) {
		ServletContextHandler app = server.addContext("/git");
		gs.setRepositoryResolver(new TestRepositoryResolver(src, srcName));
		app.addServlet(new ServletHolder(gs), "/*");
		return app;
	}


	private ServletContextHandler addAuthContext(GitServlet gs,
                                                 String contextPath, String... methods) {
		ServletContextHandler auth = server.addContext('/' + contextPath);
		auth.addServlet(new ServletHolder(gs), "/*");
		return server.authBasic(auth, methods);
	}

    @Test
    public void testCloneHttpNoPassword() throws Exception {

        Path localFull = testingRemoteAndLocalRepos.getLocalFull();

        System.out.println(remoteURI);

        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider("", "");
        ClonedRepoHelper helper = new ClonedRepoHelper(localFull, remoteURI.toString(), credentials);
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
        ClonedRepoHelper helper = new ClonedRepoHelper(localFull, authURI.toString(), credentials);
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
        RepoHelper helper = new RepoHelper("");
        helper.wrapAuthentication(command, credentials);
        command.call();
    }

    @Test
    public void testLsHttpPublicUsernamePasswordEmpty() throws Exception {

        UsernamePasswordCredentialsProvider credentials =
                new UsernamePasswordCredentialsProvider("a", "asdas");

        TransportCommand command = Git.lsRemoteRepository().setRemote(remoteURI.toString());
        RepoHelper helper = new RepoHelper("");
        helper.wrapAuthentication(command, credentials);
        command.call();
    }

    @Test
    public void testLsHttpPrivateUsernamePassword() throws Exception {

        System.out.println(authURI);
        UsernamePasswordCredentialsProvider credentials =
                new UsernamePasswordCredentialsProvider("agitter", "letmein");

        TransportCommand command = Git.lsRemoteRepository().setRemote(authURI.toString());
        RepoHelper helper = new RepoHelper("");
        helper.wrapAuthentication(command, credentials);
        command.call();
    }

    @Test
    public void testCloneRepositoryPublicWithChecksHttpUsernamePassword() throws Exception {

        RepoHelperBuilder.AuthDialogResponse response =
                new RepoHelperBuilder.AuthDialogResponse(null, "", "", false);

        Path localFull = testingRemoteAndLocalRepos.getLocalFull();
        ClonedRepoHelperBuilder.cloneRepositoryWithChecks(remoteURI.toString(), localFull, response,
                                                          new ElegitUserInfoTest());

    }

    @Test
    public void testCloneRepositoryPrivateWithChecksHttpUsernamePassword() throws Exception {

        RepoHelperBuilder.AuthDialogResponse response =
                new RepoHelperBuilder.AuthDialogResponse(null, "agitter", "letmein", false);

        Path localFull = testingRemoteAndLocalRepos.getLocalFull();
        ClonedRepoHelperBuilder.cloneRepositoryWithChecks(authURI.toString(), localFull, response,
                                                          new ElegitUserInfoTest());

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
        ClonedRepoHelper helper = new ClonedRepoHelper(localFull, "", credentials);
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
        helper.pushTags();
    }

    // Test to make sure creating a local branch lets us push and
    // that pushing will create the new branch.
    @Test
    public void testResetFile() throws Exception {

        // Repo that will commit to master
        Path repoPath = testingRemoteAndLocalRepos.getLocalFull();
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider("agitter",
                                                                                                  "letmein");
        ClonedRepoHelper helper = new ClonedRepoHelper(repoPath, "", credentials);
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
        helper.addFilePathsTest(paths);
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
        ClonedRepoHelper helper = new ClonedRepoHelper(repoPath, "", credentials);
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
        ClonedRepoHelper helper = new ClonedRepoHelper(repoPath, "", credentials);
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
        ClonedRepoHelper helperPush = new ClonedRepoHelper(repoPathPush, "", credentials);
        assertNotNull(helperPush);
        helperPush.obtainRepository(authURI.toString());

        // Repo that will pull
        Path repoPathPull = directoryPath.resolve("pushpull2");
        ClonedRepoHelper clonedHelperPull = new ClonedRepoHelper(repoPathPull, "", credentials);
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
        ClonedRepoHelper helperPush = new ClonedRepoHelper(repoPathPush, "", credentials);
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
        ClonedRepoHelper helperPush = new ClonedRepoHelper(repoPathPush, "", credentials);
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
        ClonedRepoHelper repo1 = new ClonedRepoHelper(repoPath1, "", credentials);
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
        ClonedRepoHelper repo2 = new ClonedRepoHelper(repoPath2, "", credentials);
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

}
