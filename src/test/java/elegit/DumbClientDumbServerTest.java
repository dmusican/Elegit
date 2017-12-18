/*
 * Copyright (C) 2010, Google Inc.
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

import elegit.models.AuthMethod;
import elegit.models.ClonedRepoHelper;
import elegit.models.ExistingRepoHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.errors.NotSupportedException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.junit.http.AccessEvent;
import org.eclipse.jgit.junit.http.HttpTestCase;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevBlob;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.FetchConnection;
import org.eclipse.jgit.transport.HttpTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.TransportHttp;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.transport.http.HttpConnectionFactory;
import org.eclipse.jgit.transport.http.JDKHttpConnectionFactory;
import org.eclipse.jgit.transport.http.apache.HttpClientConnectionFactory;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.eclipse.jgit.util.HttpSupport.HDR_ACCEPT;
import static org.eclipse.jgit.util.HttpSupport.HDR_PRAGMA;
import static org.eclipse.jgit.util.HttpSupport.HDR_USER_AGENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class DumbClientDumbServerTest extends HttpTestCase {
	private Repository remoteRepository;

	private URIish remoteURI;

	private RevBlob A_txt;

	private RevCommit A, B;

	@ClassRule
	public static final TestingLogPath testingLogPath = new TestingLogPath();

	@Rule
	public final TestingRemoteAndLocalRepos testingRemoteAndLocalRepos =
			new TestingRemoteAndLocalRepos(false);
	private Path directoryPath;

	private static final Logger console = LogManager.getLogger("briefconsolelogger");

	private static final String testPassword = "a_test_password";



	@Parameters
	public static Collection<Object[]> data() {
		// run all tests with both connection factories we have
		return Arrays.asList(new Object[][] {
				//{ new JDKHttpConnectionFactory() },
				{ new HttpClientConnectionFactory() } });
	}

	public DumbClientDumbServerTest(HttpConnectionFactory cf) {
		HttpTransport.setConnectionFactory(cf);
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();

		directoryPath = testingRemoteAndLocalRepos.getDirectoryPath();

		final TestRepository<Repository> src = createTestRepository();
		final File srcGit = src.getRepository().getDirectory();
		final URI base = srcGit.getParentFile().toURI();

		ServletContextHandler app = server.addContext("/git");
		app.setResourceBase(base.toString());
		ServletHolder holder = app.addServlet(DefaultServlet.class, "/");
		// The tmp directory is symlinked on OS X
		holder.setInitParameter("aliases", "true");
		server.setUp();

		remoteRepository = src.getRepository();
		remoteURI = toURIish(app, srcGit.getName());

		A_txt = src.blob("A");
		A = src.commit().add("A_txt", A_txt).create();
		B = src.commit().parent(A).add("A_txt", "C").add("B", "B").create();
		src.update(master, B);
	}



	@Test
	public void myTest() throws Exception {
		Path remoteFull = testingRemoteAndLocalRepos.getRemoteFull();
		Path localFull = testingRemoteAndLocalRepos.getLocalFull();
		System.out.println("Remote full is " + remoteFull);
		Repository db = new FileRepository(new File(remoteFull.toString()));
		console.info("repo " + db.toString());

		Transport t = Transport.open(db, remoteURI);
		((TransportHttp) t).setUseSmartHttp(false);

		// Set up remote repo
		Path remoteFilePath = remoteFull.resolve("file.txt");
		Files.write(remoteFilePath, "hello".getBytes());
		ArrayList<Path> paths = new ArrayList<>();
		paths.add(remoteFilePath);
		ExistingRepoHelper helperServer = new ExistingRepoHelper(remoteFull, null);
		helperServer.addFilePathsTest(paths);
		helperServer.commit("Initial unit test commit");

		UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider("agitter",
																								  "letmein");
		console.info("Remote is " + db.getBranch() + " " + db.getRemoteName("hey"));
		System.out.println("Remote URI is " + remoteURI);
		System.out.println(localFull);
		ClonedRepoHelper helper = new ClonedRepoHelper(localFull, remoteURI.toString(), credentials);
//        assertNotNull(helper);
        helper.obtainRepository(remoteURI.toString());

		        assertEquals(helper.getCompatibleAuthentication(), AuthMethod.HTTP);
        helper.fetch(false);
        Path fileLocation = localFull.resolve("file.txt");
        console.info("File location is " + fileLocation);
        FileWriter fw = new FileWriter(fileLocation.toString(), true);
        fw.write("1");
        fw.close();
        paths = new ArrayList<>();
        paths.add(fileLocation.getFileName());
        helper.addFilePaths(paths);
        helper.commit("Appended to file");
        PushCommand command = helper.prepareToPushAll();
        helper.pushAll(command);

	}

	@Test
	public void testListRemote() throws IOException {
		Repository dst = createBareRepository();

		assertEquals("http", remoteURI.getScheme());

		Map<String, Ref> map;
		try (Transport t = Transport.open(dst, remoteURI)) {
			// I didn't make up these public interface names, I just
			// approved them for inclusion into the code base. Sorry.
			// --spearce
			//
			assertTrue("isa TransportHttp", t instanceof TransportHttp);
			assertTrue("isa HttpTransport", t instanceof HttpTransport);

			try (FetchConnection c = t.openFetch()) {
				map = c.getRefsMap();
			}
		}

		assertNotNull("have map of refs", map);
		assertEquals(2, map.size());

		assertNotNull("has " + master, map.get(master));
		assertEquals(B, map.get(master).getObjectId());

		assertNotNull("has " + Constants.HEAD, map.get(Constants.HEAD));
		assertEquals(B, map.get(Constants.HEAD).getObjectId());

		List<AccessEvent> requests = getRequests();
		assertEquals(2, requests.size());
		assertEquals(0, getRequests(remoteURI, "git-upload-pack").size());

		AccessEvent info = requests.get(0);
		assertEquals("GET", info.getMethod());
		assertEquals(join(remoteURI, "info/refs"), info.getPath());
		assertEquals(1, info.getParameters().size());
		assertEquals("git-upload-pack", info.getParameter("service"));
		assertEquals("no-cache", info.getRequestHeader(HDR_PRAGMA));
		assertNotNull("has user-agent", info.getRequestHeader(HDR_USER_AGENT));
		assertTrue("is jgit agent", info.getRequestHeader(HDR_USER_AGENT)
				.startsWith("JGit/"));
		assertEquals("application/x-git-upload-pack-advertisement, */*", info
				.getRequestHeader(HDR_ACCEPT));
		assertEquals(200, info.getStatus());

		AccessEvent head = requests.get(1);
		assertEquals("GET", head.getMethod());
		assertEquals(join(remoteURI, "HEAD"), head.getPath());
		assertEquals(0, head.getParameters().size());
		assertEquals("no-cache", head.getRequestHeader(HDR_PRAGMA));
		assertNotNull("has user-agent", head.getRequestHeader(HDR_USER_AGENT));
		assertTrue("is jgit agent", head.getRequestHeader(HDR_USER_AGENT)
				.startsWith("JGit/"));
		assertEquals(200, head.getStatus());
	}

	@Test
	public void testInitialClone_Loose() throws Exception {
		Repository dst = createBareRepository();
		assertFalse(dst.hasObject(A_txt));

		try (Transport t = Transport.open(dst, remoteURI)) {
			t.fetch(NullProgressMonitor.INSTANCE, mirror(master));
		}

		assertTrue(dst.hasObject(A_txt));
		assertEquals(B, dst.exactRef(master).getObjectId());
		fsck(dst, B);

		List<AccessEvent> loose = getRequests(loose(remoteURI, A_txt));
		assertEquals(1, loose.size());
		assertEquals("GET", loose.get(0).getMethod());
		assertEquals(0, loose.get(0).getParameters().size());
		assertEquals(200, loose.get(0).getStatus());
	}

	@Test
	public void testInitialClone_Packed() throws Exception {
		new TestRepository<>(remoteRepository).packAndPrune();

		Repository dst = createBareRepository();
		assertFalse(dst.hasObject(A_txt));

		try (Transport t = Transport.open(dst, remoteURI)) {
			t.fetch(NullProgressMonitor.INSTANCE, mirror(master));
		}

		assertTrue(dst.hasObject(A_txt));
		assertEquals(B, dst.exactRef(master).getObjectId());
		fsck(dst, B);

		List<AccessEvent> req;
		AccessEvent event;

		req = getRequests(loose(remoteURI, B));
		assertEquals(1, req.size());
		event = req.get(0);
		assertEquals("GET", event.getMethod());
		assertEquals(0, event.getParameters().size());
		assertEquals(404, event.getStatus());

		req = getRequests(join(remoteURI, "objects/info/packs"));
		assertEquals(1, req.size());
		event = req.get(0);
		assertEquals("GET", event.getMethod());
		assertEquals(0, event.getParameters().size());
		assertEquals("no-cache", event.getRequestHeader(HDR_PRAGMA));
		assertNotNull("has user-agent", event.getRequestHeader(HDR_USER_AGENT));
		assertTrue("is jgit agent", event.getRequestHeader(HDR_USER_AGENT)
				.startsWith("JGit/"));
		assertEquals(200, event.getStatus());
	}

	@Test
	public void testPushNotSupported() throws Exception {
		final TestRepository src = createTestRepository();
		final RevCommit Q = src.commit().create();
		final Repository db = src.getRepository();

		try (Transport t = Transport.open(db, remoteURI)) {
			try {
				t.push(NullProgressMonitor.INSTANCE, push(src, Q));
				fail("push incorrectly completed against a dumb server");
			} catch (NotSupportedException nse) {
				String exp = "remote does not support smart HTTP push";
				assertEquals(exp, nse.getMessage());
			}
		}
	}
}
