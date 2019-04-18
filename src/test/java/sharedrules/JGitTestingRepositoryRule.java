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

package sharedrules;

import elegit.TestRepositoryResolver;
import elegit.models.SessionModel;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jgit.http.server.GitServlet;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.junit.http.HttpTestCase;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevBlob;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.URIish;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class JGitTestingRepositoryRule extends HttpTestCase implements TestRule {

    private TestRepository<Repository> jgitTestRepo;
    private URIish remoteURI;
    private URIish authURI;

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {

                JGitTestingRepositoryRule.super.setUp();

                // Set up primary remote repo
                jgitTestRepo = createTestRepository();
                final String srcName = jgitTestRepo.getRepository().getDirectory().getName();
                jgitTestRepo.getRepository()
                        .getConfig()
                        .setBoolean(ConfigConstants.CONFIG_CORE_SECTION, null,
                                    ConfigConstants.CONFIG_KEY_LOGALLREFUPDATES, true);

                GitServlet gs = new GitServlet();
                ServletContextHandler app = addNormalContext(gs, jgitTestRepo, srcName);
                ServletContextHandler auth = addAuthContext(gs, "auth");
                server.setUp();
                remoteURI = toURIish(app, srcName);
                authURI = toURIish(auth, srcName);
                RevBlob A_txt = jgitTestRepo.blob("A");
                RevCommit A = jgitTestRepo.commit().add("A_txt", A_txt).create();
                RevCommit B = jgitTestRepo.commit().parent(A).add("A_txt", "C").add("B", "B").create();
                jgitTestRepo.update(master, B);

                RevCommit C = jgitTestRepo.commit().parent(B)
                        .add("modify.txt", "A file to be modified, then reset\n").create();
                RevCommit D = jgitTestRepo.commit().parent(C)
                        .add("README.md", "# Reset testing\nA test repo to pull\n").create();
                jgitTestRepo.update(master, D);

                base.evaluate();
            }
        };
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


    public URIish getAuthURI() {
        return authURI;
    }

    public URIish getRemoteURI() {
        return remoteURI;
    }

    public TestRepository<Repository> getJgitTestRepo() {
        return jgitTestRepo;
    }
}
