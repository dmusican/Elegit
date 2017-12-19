/*
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

import elegit.models.AuthMethod;
import elegit.models.ClonedRepoHelper;
import elegit.models.ExistingRepoHelper;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jgit.api.PushCommand;
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

@RunWith(Parameterized.class)
public class SmartClientSmartServerTest extends HttpTestCase {
	private static final String HDR_TRANSFER_ENCODING = "Transfer-Encoding";

	private Repository remoteRepository;

	private CredentialsProvider testCredentials = new UsernamePasswordCredentialsProvider(
            AppServer.username, AppServer.password);

	private URIish remoteURI;

	private URIish brokenURI;

	private URIish redirectURI;

	private URIish authURI;

	private URIish authOnPostURI;

	private RevBlob A_txt;

	private RevCommit A, B;

	@Parameters
	public static Collection<Object[]> data() {
		// run all tests with both connection factories we have
		return Arrays.asList(new Object[][] {
//				{ new JDKHttpConnectionFactory() },
				{ new HttpClientConnectionFactory() } });
	}

	public SmartClientSmartServerTest(HttpConnectionFactory cf) {
		HttpTransport.setConnectionFactory(cf);
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

		ServletContextHandler broken = addBrokenContext(gs, src, srcName);

		ServletContextHandler redirect = addRedirectContext(gs);

		ServletContextHandler auth = addAuthContext(gs, "auth");

		ServletContextHandler authOnPost = addAuthContext(gs, "pauth", "POST");

		server.setUp();

		remoteRepository = src.getRepository();
		remoteURI = toURIish(app, srcName);
		brokenURI = toURIish(broken, srcName);
		redirectURI = toURIish(redirect, srcName);
		authURI = toURIish(auth, srcName);
		authOnPostURI = toURIish(authOnPost, srcName);

		A_txt = src.blob("A");
		A = src.commit().add("A_txt", A_txt).create();
		B = src.commit().parent(A).add("A_txt", "C").add("B", "B").create();
		src.update(master, B);

		src.update("refs/garbage/a/very/long/ref/name/to/compress", B);
	}

	private ServletContextHandler addNormalContext(GitServlet gs, TestRepository<Repository> src, String srcName) {
		ServletContextHandler app = server.addContext("/git");
		app.addFilter(new FilterHolder(new Filter() {

			@Override
			public void init(FilterConfig filterConfig)
					throws ServletException {
				// empty
			}

			// Does an internal forward for GET requests containing "/post/",
			// and issues a 301 redirect on POST requests for such URLs. Used
			// in the POST redirect tests.
			@Override
			public void doFilter(ServletRequest request,
					ServletResponse response, FilterChain chain)
					throws IOException, ServletException {
				final HttpServletResponse httpServletResponse = (HttpServletResponse) response;
				final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
				final StringBuffer fullUrl = httpServletRequest.getRequestURL();
				if (httpServletRequest.getQueryString() != null) {
					fullUrl.append("?")
							.append(httpServletRequest.getQueryString());
				}
				String urlString = fullUrl.toString();
				if ("POST".equalsIgnoreCase(httpServletRequest.getMethod())) {
					httpServletResponse.setStatus(
							HttpServletResponse.SC_MOVED_PERMANENTLY);
					httpServletResponse.setHeader(HttpSupport.HDR_LOCATION,
                                                  urlString.replace("/post/", "/"));
				} else {
					String path = httpServletRequest.getPathInfo();
					path = path.replace("/post/", "/");
					if (httpServletRequest.getQueryString() != null) {
						path += '?' + httpServletRequest.getQueryString();
					}
					RequestDispatcher dispatcher = httpServletRequest
							.getRequestDispatcher(path);
					dispatcher.forward(httpServletRequest, httpServletResponse);
				}
			}

			@Override
			public void destroy() {
				// empty
			}
		}), "/post/*", EnumSet.of(DispatcherType.REQUEST));
		gs.setRepositoryResolver(new TestRepositoryResolver(src, srcName));
		app.addServlet(new ServletHolder(gs), "/*");
		return app;
	}

	@SuppressWarnings("unused")
	private ServletContextHandler addBrokenContext(GitServlet gs, TestRepository<Repository> src, String srcName) {
		ServletContextHandler broken = server.addContext("/bad");
		broken.addFilter(new FilterHolder(new Filter() {

			@Override
			public void doFilter(ServletRequest request,
					ServletResponse response, FilterChain chain)
					throws IOException, ServletException {
				final HttpServletResponse r = (HttpServletResponse) response;
				r.setContentType("text/plain");
				r.setCharacterEncoding("UTF-8");
				PrintWriter w = r.getWriter();
				w.print("OK");
				w.close();
			}

			@Override
			public void init(FilterConfig filterConfig)
					throws ServletException {
				// empty
			}

			@Override
			public void destroy() {
				// empty
			}
		}), "/" + srcName + "/git-upload-pack",
                         EnumSet.of(DispatcherType.REQUEST));
		broken.addServlet(new ServletHolder(gs), "/*");
		return broken;
	}

	private ServletContextHandler addAuthContext(GitServlet gs,
                                                 String contextPath, String... methods) {
		ServletContextHandler auth = server.addContext('/' + contextPath);
		auth.addServlet(new ServletHolder(gs), "/*");
		return server.authBasic(auth, methods);
	}

	private ServletContextHandler addRedirectContext(GitServlet gs) {
		ServletContextHandler redirect = server.addContext("/redirect");
		redirect.addFilter(new FilterHolder(new Filter() {

			// Enables tests for different codes, and for multiple redirects.
			// First parameter is the number of redirects, second one is the
			// redirect status code that should be used
			private Pattern responsePattern = Pattern
					.compile("/response/(\\d+)/(30[1237])/");

			// Enables tests to specify the context that the request should be
			// redirected to in the end. If not present, redirects got to the
			// normal /git context.
			private Pattern targetPattern = Pattern.compile("/target(/\\w+)/");

			@Override
			public void init(FilterConfig filterConfig)
					throws ServletException {
				// empty
			}

			@Override
			public void doFilter(ServletRequest request,
					ServletResponse response, FilterChain chain)
					throws IOException, ServletException {
				final HttpServletResponse httpServletResponse = (HttpServletResponse) response;
				final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
				final StringBuffer fullUrl = httpServletRequest.getRequestURL();
				if (httpServletRequest.getQueryString() != null) {
					fullUrl.append("?")
							.append(httpServletRequest.getQueryString());
				}
				String urlString = fullUrl.toString();
				if (urlString.contains("/loop/")) {
					urlString = urlString.replace("/loop/", "/loop/x/");
					if (urlString.contains("/loop/x/x/x/x/x/x/x/x/")) {
						// Go back to initial.
						urlString = urlString.replace("/loop/x/x/x/x/x/x/x/x/",
								"/loop/");
					}
					httpServletResponse.setStatus(
							HttpServletResponse.SC_MOVED_TEMPORARILY);
					httpServletResponse.setHeader(HttpSupport.HDR_LOCATION,
                                                  urlString);
					return;
				}
				int responseCode = HttpServletResponse.SC_MOVED_PERMANENTLY;
				int nofRedirects = 0;
				Matcher matcher = responsePattern.matcher(urlString);
				if (matcher.find()) {
					nofRedirects = Integer
							.parseUnsignedInt(matcher.group(1));
					responseCode = Integer.parseUnsignedInt(matcher.group(2));
					if (--nofRedirects <= 0) {
						urlString = urlString.substring(0, matcher.start())
								+ '/' + urlString.substring(matcher.end());
					} else {
						urlString = urlString.substring(0, matcher.start())
								+ "/response/" + nofRedirects + "/"
								+ responseCode + '/'
								+ urlString.substring(matcher.end());
					}
				}
				httpServletResponse.setStatus(responseCode);
				if (nofRedirects <= 0) {
					String targetContext = "/git";
					matcher = targetPattern.matcher(urlString);
					if (matcher.find()) {
						urlString = urlString.substring(0, matcher.start())
								+ '/' + urlString.substring(matcher.end());
						targetContext = matcher.group(1);
					}
					urlString = urlString.replace("/redirect", targetContext);
				}
				httpServletResponse.setHeader(HttpSupport.HDR_LOCATION,
                                              urlString);
			}

			@Override
			public void destroy() {
				// empty
			}
		}), "/*", EnumSet.of(DispatcherType.REQUEST));
		redirect.addServlet(new ServletHolder(gs), "/*");
		return redirect;
	}


	@Test
	public void testInitialClone_WithAuthentication() throws Exception {

        // Set up remote repo
        TestingRemoteAndLocalRepos testingRemoteAndLocalRepos =
                new TestingRemoteAndLocalRepos(false);
        testingRemoteAndLocalRepos.before();
        Path remoteFull = testingRemoteAndLocalRepos.getRemoteFull();
        Path localFull = testingRemoteAndLocalRepos.getLocalFull();
        System.out.println("remote full is " + remoteFull);

        Repository db = new FileRepository(remoteFull.toString());

        Path remoteFilePath = remoteFull.resolve("file.txt");
        Files.write(remoteFilePath, "hello".getBytes());
        ArrayList<Path> paths = new ArrayList<>();
        paths.add(remoteFilePath);
        ExistingRepoHelper helperServer = new ExistingRepoHelper(remoteFull, null);
        helperServer.addFilePathsTest(paths);
        helperServer.commit("Initial unit test commit");


        Repository dst = createBareRepository();

        System.out.println("Location is " + db.getDirectory());
        System.out.println("Other location is " + dst.getDirectory());
        System.out.println(dst.getDirectory());
        System.out.println(authURI);

        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider("agitter", "letmein");
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
        paths = new ArrayList<>();
        paths.add(fileLocation.getFileName());
        helper.addFilePaths(paths);
        helper.commit("Appended to file");
        PushCommand command = helper.prepareToPushAll();
        helper.pushAll(command);

	}


}
