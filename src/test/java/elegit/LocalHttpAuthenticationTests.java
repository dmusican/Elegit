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
import elegit.models.ClonedRepoHelper;
import elegit.models.ExistingRepoHelper;
import elegit.models.RepoHelper;
import elegit.sshauthentication.ElegitUserInfoTest;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
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

    @Test
    /* This is a version of the test where the username password is entered incorrectly at first, which works for a
     * public repo, but later needs to be specified for a push.
     * and needs to be fixed later.
     */

    public void testHttpBadUsernamePassword() throws Exception {

        System.out.println("remote " + remoteURI);
        System.out.println("auth " + authURI);
        Path localFull = testingRemoteAndLocalRepos.getLocalFull();
        System.out.println("local " + localFull);
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider("", "");
        ClonedRepoHelper helper = new ClonedRepoHelper(localFull, remoteURI.toString(), credentials);
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
        helper.pushTags();
    }


}
