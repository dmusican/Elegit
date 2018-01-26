package sharedrules;

import elegit.exceptions.CancelledAuthorizationException;
import elegit.exceptions.MissingRepoException;
import elegit.models.ExistingRepoHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.common.keyprovider.MappedKeyPairProvider;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.git.pack.GitPackCommandFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.pubkey.KeySetPublickeyAuthenticator;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;

import static org.junit.Assert.fail;

public class TestUtilities {

    private static final Logger console = LogManager.getLogger("briefconsolelogger");
    private static final String testPassword = "a_test_password";

    public static String setUpTestSshServer(SshServer sshd,
                                            Path serverDirectory,
                                            Path remoteRepoDirectoryFull,
                                            Path remoteRepoDirectoryBrief)
            throws IOException, GitAPIException,
            CancelledAuthorizationException,
            MissingRepoException, GeneralSecurityException {

        // Set up remote repo
        Path remoteFilePath = remoteRepoDirectoryFull.resolve("file.txt");
        Files.write(remoteFilePath, "testSshPassword".getBytes());
        ArrayList<Path> paths = new ArrayList<>();
        paths.add(remoteFilePath);
        ExistingRepoHelper helperServer = new ExistingRepoHelper(remoteRepoDirectoryFull, null);
        helperServer.addFilePathsTest(paths);
        helperServer.commit("Initial unit test commit");

        // All of this key set up is gratuitous, but it's the only way that I was able to get sshd to start up.
        // In the end, it is ignore, and the password is used.

        // Provide SSH server with public and private key info that client will be connecting with
        InputStream passwordFileStream = TestUtilities.class.getResourceAsStream("/rsa_key1_passphrase.txt");
        Scanner scanner = new Scanner(passwordFileStream);
        String passphrase = scanner.next();
        console.info("phrase is " + passphrase);

        String privateKeyFileLocation = "/rsa_key1";
        InputStream privateKeyStream = TestUtilities.class.getResourceAsStream(privateKeyFileLocation);
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
        sshd.setCommandFactory(new GitPackCommandFactory(serverDirectory.toString()));

        // Start the SSH test server.
        sshd.start();

        // Create temporary known_hosts file.
        Path knownHostsFileLocation = serverDirectory.resolve("testing_known_hosts");
        Files.createFile(knownHostsFileLocation);

        // Clone the bare repo, using the SSH connection, to the local.
        return "ssh://localhost:2222/"+remoteRepoDirectoryBrief;
    }

}
