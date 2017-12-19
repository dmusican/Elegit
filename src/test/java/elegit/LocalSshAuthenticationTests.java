package elegit;

import elegit.exceptions.CancelledAuthorizationException;
import elegit.exceptions.MissingRepoException;
import elegit.models.AuthMethod;
import elegit.models.ClonedRepoHelper;
import elegit.models.ExistingRepoHelper;
import elegit.models.RepoHelper;
import elegit.sshauthentication.ElegitUserInfoTest;
import junit.framework.TestCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.common.keyprovider.MappedKeyPairProvider;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.git.pack.GitPackCommandFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.pubkey.KeySetPublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.http.server.GitServlet;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.junit.http.AppServer;
import org.eclipse.jgit.junit.http.HttpTestCase;
import org.eclipse.jgit.junit.http.RecordingLogger;
import org.eclipse.jgit.junit.http.SimpleHttpServer;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.ServiceMayNotContinueException;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.TransportGitSsh;
import org.eclipse.jgit.transport.TransportProtocol;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.transport.resolver.RepositoryResolver;
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException;
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException;
import org.eclipse.jgit.util.HttpSupport;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

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
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class LocalSshAuthenticationTests {

    @ClassRule
    public static final TestingLogPath testingLogPath = new TestingLogPath();

    @Rule
    public final TestingRemoteAndLocalRepos testingRemoteAndLocalRepos =
            new TestingRemoteAndLocalRepos(false);
    private Path directoryPath;

    private static final Logger console = LogManager.getLogger("briefconsolelogger");

    private static final String testPassword = "a_test_password";

    private URIish authURI;

    @Before
    public void setUp() throws Exception {
        console.info("Unit test started");
        directoryPath = testingRemoteAndLocalRepos.getDirectoryPath();


    }

    // http://www.jcraft.com/jsch/examples/Logger.java.html
    public static class MyLogger implements com.jcraft.jsch.Logger {
        static java.util.Hashtable<Integer,String> name=new java.util.Hashtable<>();
        static{
            name.put(DEBUG, "DEBUG: ");
            name.put(INFO, "INFO: ");
            name.put(WARN, "WARN: ");
            name.put(ERROR, "ERROR: ");
            name.put(FATAL, "FATAL: ");
        }
        public boolean isEnabled(int level){
            return true;
        }
        public void log(int level, String message){
            System.err.print(name.get(level));
            System.err.println(message);
        }
    }


    @Test
    public void testSshPrivateKey() throws Exception {
        // Uncomment this to get detail SSH logging info, for debugging
        //JSch.setLogger(new AuthenticatedCloneTest.MyLogger());

        // Set up test SSH server.
        try (SshServer sshd = SshServer.setUpDefaultServer()) {

            // Provide SSH server with public and private key info that client will be connecting with
            InputStream passwordFileStream = getClass().getResourceAsStream("/rsa_key1_passphrase.txt");
            Scanner scanner = new Scanner(passwordFileStream);
            String passphrase = scanner.next();
            console.info("phrase is " + passphrase);

            String privateKeyFileLocation = "/rsa_key1";
            InputStream privateKeyStream = getClass().getResourceAsStream(privateKeyFileLocation);
            FilePasswordProvider filePasswordProvider = FilePasswordProvider.of(passphrase);
            KeyPair kp = SecurityUtils.loadKeyPairIdentity("testkey", privateKeyStream, filePasswordProvider);
            ArrayList<KeyPair> pairs = new ArrayList<>();
            pairs.add(kp);
            KeyPairProvider hostKeyProvider = new MappedKeyPairProvider(pairs);
            sshd.setKeyPairProvider(hostKeyProvider);

            // Need to use a non-standard port, as there may be an ssh server already running on this machine
            sshd.setPort(2222);

            // Set up a fall-back password authenticator to help in diagnosing failed test
            sshd.setPasswordAuthenticator(new PasswordAuthenticator() {
                public boolean authenticate(String username, String password, ServerSession session) {
                    fail("Tried to use password instead of public key authentication");
                    return false;
                }
            });

            // This replaces the role of authorized_keys.
            Collection<PublicKey> allowedKeys = new ArrayList<>();
            allowedKeys.add(kp.getPublic());
            sshd.setPublickeyAuthenticator(new KeySetPublickeyAuthenticator(allowedKeys));

            // Amazingly useful Git command setup provided by Mina.
            sshd.setCommandFactory(new GitPackCommandFactory(directoryPath.toString()));

            // Start the SSH test server.
            sshd.start();

            // Create temporary known_hosts file.
            Path knownHostsFileLocation = directoryPath.resolve("testing_known_hosts");
            Files.createFile(knownHostsFileLocation);

            // Clone the bare repo, using the SSH connection, to the local.
            String remoteURL = "ssh://localhost:2222/" + testingRemoteAndLocalRepos.getRemoteBrief();
            console.info("Connecting to " + remoteURL);
            Path local = testingRemoteAndLocalRepos.getLocalFull();
            ClonedRepoHelper helper =
                    new ClonedRepoHelper(local, remoteURL, passphrase,
                                         new ElegitUserInfoTest(null, passphrase),
                                         getClass().getResource(privateKeyFileLocation).getFile(),
                                         directoryPath.resolve("testing_known_hosts").toString());
            helper.obtainRepository(remoteURL);

            // Verify that it is an SSH connection, then try a fetch
            assertEquals(helper.getCompatibleAuthentication(), AuthMethod.SSH);
            helper.fetch(false);

            // Create a new test file at the local repo
            Path fileLocation = local.resolve("README.md");
            FileWriter fw = new FileWriter(fileLocation.toString(), true);
            fw.write("start");
            fw.close();

            // Commit, and push to remote
            helper.addFilePathTest(fileLocation);
            helper.commit("Appended to file");
            PushCommand command = helper.prepareToPushAll();
            helper.pushAll(command);

            // Other methods to test if authentication is succeeding
            helper.getRefsFromRemote(false);

            // Shut down test SSH server
            sshd.stop();
        }
    }


    @Test
    public void testSshPassword() throws Exception {

        try (SshServer sshd = SshServer.setUpDefaultServer()) {
            String remoteURL = setUpTestSshServer(sshd);

            console.info("Connecting to " + remoteURL);
            Path local = testingRemoteAndLocalRepos.getLocalFull();
            ClonedRepoHelper helper =
                    new ClonedRepoHelper(local, remoteURL, testPassword,
                                         new ElegitUserInfoTest(null, null));
            helper.obtainRepository(remoteURL);

            assertEquals(helper.getCompatibleAuthentication(), AuthMethod.SSH);
            helper.fetch(false);
            PushCommand command = helper.prepareToPushAll();
            helper.pushAll(command);
        }
    }

    @Test
    public void testLsSshPassword() throws Exception {
        try (SshServer sshd = SshServer.setUpDefaultServer()) {
            String remoteURL = setUpTestSshServer(sshd);
            TransportCommand command = Git.lsRemoteRepository().setRemote(remoteURL);
            RepoHelper helper = new RepoHelper(new ElegitUserInfoTest(testPassword, null));
            helper.wrapAuthentication(command);
            command.call();
        }
    }


    private String setUpTestSshServer(SshServer sshd) throws IOException, GitAPIException,
            CancelledAuthorizationException,
            MissingRepoException, GeneralSecurityException {
        Path local = testingRemoteAndLocalRepos.getLocalFull();
        Path remote = testingRemoteAndLocalRepos.getRemoteFull();

        // Set up remote repo
        Path remoteFilePath = remote.resolve("file.txt");
        Files.write(remoteFilePath, "testSshPassword".getBytes());
        ArrayList<Path> paths = new ArrayList<>();
        paths.add(remoteFilePath);
        ExistingRepoHelper helperServer = new ExistingRepoHelper(remote, null);
        helperServer.addFilePathsTest(paths);
        helperServer.commit("Initial unit test commit");

        // All of this key set up is gratuitous, but it's the only way that I was able to get sshd to start up.
        // In the end, it is ignore, and the password is used.

        // Provide SSH server with public and private key info that client will be connecting with
        InputStream passwordFileStream = getClass().getResourceAsStream("/rsa_key1_passphrase.txt");
        Scanner scanner = new Scanner(passwordFileStream);
        String passphrase = scanner.next();
        console.info("phrase is " + passphrase);

        String privateKeyFileLocation = "/rsa_key1";
        InputStream privateKeyStream = getClass().getResourceAsStream(privateKeyFileLocation);
        FilePasswordProvider filePasswordProvider = FilePasswordProvider.of(passphrase);
        KeyPair kp = SecurityUtils.loadKeyPairIdentity("testkey", privateKeyStream, filePasswordProvider);
        ArrayList<KeyPair> pairs = new ArrayList<>();
        pairs.add(kp);
        KeyPairProvider hostKeyProvider = new MappedKeyPairProvider(pairs);
        sshd.setKeyPairProvider(hostKeyProvider);

        // Need to use a non-standard port, as there may be an ssh server already running on this machine
        sshd.setPort(2222);

        // Set up a fall-back password authenticator to help in diagnosing failed test
        sshd.setPasswordAuthenticator(
                (username, password, session) -> {
                    if (password.equals(testPassword)) {
                        return true;
                    } else {
                        fail("Tried to use password instead of public key authentication");
                        return false;
                    }
                });


        // This replaces the role of authorized_keys.
        Collection<PublicKey> allowedKeys = new ArrayList<>();
        allowedKeys.add(kp.getPublic());
        sshd.setPublickeyAuthenticator(new KeySetPublickeyAuthenticator(allowedKeys));

        // Amazingly useful Git command setup provided by Mina.
        sshd.setCommandFactory(new GitPackCommandFactory(directoryPath.toString()));

        // Start the SSH test server.
        sshd.start();

        // Create temporary known_hosts file.
        Path knownHostsFileLocation = directoryPath.resolve("testing_known_hosts");
        Files.createFile(knownHostsFileLocation);

        // Clone the bare repo, using the SSH connection, to the local.
        return "ssh://localhost:2222/"+testingRemoteAndLocalRepos.getRemoteBrief();
    }

    @Test
    public void testTransportProtocols() throws Exception {
        List<TransportProtocol> protocols = TransportGitSsh.getTransportProtocols();
        for (TransportProtocol protocol : protocols) {
            System.out.println(protocol + " " + protocol.getName());
            for (String scheme : protocol.getSchemes()) {
                System.out.println("\t" + scheme);
            }
        }
        for (TransportProtocol protocol : protocols) {
            if (protocol.canHandle(new URIish("https://anything.com/repo.git"))) {
                assertEquals(protocol.getName(), "HTTP");
                assertNotEquals(protocol.getName(), "SSH");
            }

            if (protocol.canHandle(new URIish("ssh://anything.com/repo.git"))) {
                assertEquals(protocol.getName(), "SSH");
                assertNotEquals(protocol.getName(), "HTTP");
            }
        }
    }

    protected static URIish extendPath(URIish uri, String pathComponents)
            throws URISyntaxException {
        String raw = uri.toString();
        String newComponents = pathComponents;
        if (!newComponents.startsWith("/")) {
            newComponents = '/' + newComponents;
        }
        if (!newComponents.endsWith("/")) {
            newComponents += '/';
        }
        int i = raw.lastIndexOf('/');
        raw = raw.substring(0, i) + newComponents + raw.substring(i + 1);
        return new URIish(raw);
    }

    protected static Set<RefSpec> mirror(String... refs) {
        HashSet<RefSpec> r = new HashSet<>();
        for (String name : refs) {
            RefSpec rs = new RefSpec(name);
            rs = rs.setDestination(name);
            rs = rs.setForceUpdate(true);
            r.add(rs);
        }
        return r;
    }



    @Test
    public void testCloneHttpPassword() throws Exception  {

//        Path remoteFull = testingRemoteAndLocalRepos.getRemoteFull();
//        Path localFull = testingRemoteAndLocalRepos.getLocalFull();
//        System.out.println("remote full is " + remoteFull);
//        Repository db = new FileRepository(remoteFull.toString());
////        Repository db = new FileRepository("/tmp/sbasic");

        // Set up remote repo
//        Path remoteFilePath = remoteFull.resolve("file.txt");
//        Files.write(remoteFilePath, "hello".getBytes());
//        ArrayList<Path> paths = new ArrayList<>();
//        paths.add(remoteFilePath);
//        ExistingRepoHelper helperServer = new ExistingRepoHelper(remoteFull, null);
//        helperServer.addFilePathsTest(paths);
//        helperServer.commit("Initial unit test commit");



        //Repository db = new FileRepository("/sbasic/remote");
        Repository db = new FileRepository("remote");
        SimpleHttpServer server = new SimpleHttpServer(db, false);
        server.start();
        System.out.println(server.getUri().toString());
        while(true);

//
//        UsernamePasswordCredentialsProvider testCredentials = new UsernamePasswordCredentialsProvider("agitter",
//                                                                                                  "letmein");
//
////        CredentialsProvider uriSpecificCredentialsProvider = new UsernamePasswordCredentialsProvider(
////                "unknown", "none") {
////            @Override
////            public boolean get(URIish uri, CredentialItem... items)
////                    throws UnsupportedCredentialItem {
////                // Only return the true credentials if the uri path starts with
////                // /auth. This ensures that we do provide the correct
////                // credentials only for the URi after the redirect, making the
////                // test fail if we should be asked for the credentials for the
////                // original URI.
////                if (uri.getPath().startsWith("/auth")) {
////                    return testCredentials.get(uri, items);
////                }
////                return super.get(uri, items);
////            }
////        };
//        Repository outdb = new FileRepository("/tmp/dave2");
//        outdb.create(true);
//        System.out.println(server.getURI());
//        try (Transport t = Transport.open(outdb, authURI)) {
//            t.setCredentialsProvider(testCredentials);
//            t.fetch(NullProgressMonitor.INSTANCE, mirror(Constants.R_HEADS + Constants.MASTER));
//        }
//
//
////
////        while(true) {
////            if (RecordingLogger.getWarnings().size() > 0) {
////                System.out.println(RecordingLogger.getWarnings());
////            }
////        }

//        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider("agitter",
//                                                                                                  "letmein");
//        ClonedRepoHelper helper = new ClonedRepoHelper(localFull, server.getUri().toString(), credentials);
//        assertNotNull(helper);
//        helper.obtainRepository(server.getUri().toString());

        ////        assertEquals(helper.getCompatibleAuthentication(), AuthMethod.HTTP);
////        helper.fetch(false);
////        Path fileLocation = localFull.resolve("file.txt");
////        console.info("File location is " + fileLocation);
////        FileWriter fw = new FileWriter(fileLocation.toString(), true);
////        fw.write("1");
////        fw.close();
////        while (true);
////        paths = new ArrayList<>();
////        paths.add(fileLocation.getFileName());
////        helper.addFilePaths(paths);
////        helper.commit("Appended to file");
////        PushCommand command = helper.prepareToPushAll();
////        helper.pushAll(command);


    }

}
