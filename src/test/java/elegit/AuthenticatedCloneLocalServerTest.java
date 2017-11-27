package elegit;

import com.jcraft.jsch.JSch;
import elegit.exceptions.ExceptionAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.keyprovider.MappedKeyPairProvider;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.server.SshServer;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.openssl.PKCS8Generator;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.pkcs.jcajce.JcePKCSPBEOutputEncryptorBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;


public class AuthenticatedCloneLocalServerTest {

    private static Path logPath;

    static {
        try {
            logPath = Files.createTempDirectory("elegitLogs");
        } catch (IOException e) {
            throw new ExceptionAdapter(e);
        }

        logPath.toFile().deleteOnExit();

        System.setProperty("logFolder", logPath.toString());
    }

    private static final Logger console = LogManager.getLogger("briefconsolelogger");

    private Path directoryPath;

    @Before
    public void setUp() throws Exception {
        console.info("Unit test started");

        directoryPath = Files.createTempDirectory("unitTestRepos");
        directoryPath.toFile().deleteOnExit();
        console.info("Setting server root to " + directoryPath);
    }

    @After
    public void tearDown() throws Exception {
//        removeAllFilesFromDirectory(this.logPath.toFile());
//        removeAllFilesFromDirectory(directoryPath.toFile());
    }

    // Helper tear-down method:
    void removeAllFilesFromDirectory(File dir) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) removeAllFilesFromDirectory(file);
            file.delete();
        }
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
        JSch.setLogger(new AuthenticatedCloneTest.MyLogger());

        // Set up test SSH server.
        SshServer sshd = SshServer.setUpDefaultServer();

        // Pay close attention; we're using two different classes called KeyPair throughout the code.

        // Generate public and private key pair for testing, and write to key files.
//        com.jcraft.jsch.KeyPair jschKeyPair = com.jcraft.jsch.KeyPair.genKeyPair(new JSch(),
//                                                                           com.jcraft.jsch.KeyPair.RSA,
//                                                                                 1024);
        Path keyPath = directoryPath.resolve("keys");
        Files.createDirectory(keyPath);
        String privateKeyFileName = keyPath.resolve("generated_key").toString();
        String publicKeyFileName = keyPath.resolve("generated_key.pub").toString();
//        jschKeyPair.writePublicKey(publicKeyFileName,"a test key");
//        jschKeyPair.writePrivateKey(privateKeyFileName);//, passphrase.getBytes());


        // https://stackoverflow.com/questions/5127379/how-to-generate-a-rsa-keypair-with-a-privatekey-encrypted-with-password
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(1024);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        // extract the encoded private key, this is an unencrypted PKCS#8 private key
        byte[] encodedprivkey = keyPair.getPrivate().getEncoded();

        // We must use a PasswordBasedEncryption algorithm in order to encrypt the private key, you may use any common algorithm supported by openssl, you can check them in the openssl documentation http://www.openssl.org/docs/apps/pkcs8.html
        String MYPBEALG = "PBEWithSHA1AndDESede";
        String passphrase = "pleaseChangeit!";

        int count = 20;// hash iteration count
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[8];
        random.nextBytes(salt);

        // Create PBE parameter set
        PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, count);
        PBEKeySpec pbeKeySpec = new PBEKeySpec(passphrase.toCharArray());
        SecretKeyFactory keyFac = SecretKeyFactory.getInstance(MYPBEALG);
        SecretKey pbeKey = keyFac.generateSecret(pbeKeySpec);

        Cipher pbeCipher = Cipher.getInstance(MYPBEALG);

        // Initialize PBE Cipher with key and parameters
        pbeCipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec);

        // Encrypt the encoded Private Key with the PBE key
        byte[] ciphertext = pbeCipher.doFinal(encodedprivkey);

        // Now construct  PKCS #8 EncryptedPrivateKeyInfo object
        AlgorithmParameters algparms = AlgorithmParameters.getInstance(MYPBEALG);
        algparms.init(pbeParamSpec);
        EncryptedPrivateKeyInfo encinfo = new EncryptedPrivateKeyInfo(algparms, ciphertext);

        // and here we have it! a DER encoded PKCS#8 encrypted key!
        byte[] encryptedPkcs8 = encinfo.getEncoded();


        // https://stackoverflow.com/questions/24506246/java-how-to-save-a-private-key-in-a-pem-file-with-password-protection
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey encPrivateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(encryptedPkcs8));
        System.out.println(encPrivateKey);
        JcaPEMWriter writer = new JcaPEMWriter(new PrintWriter(new File(privateKeyFileName)));
        writer.writeObject(encPrivateKey);
        writer.close();


        // Read back from key files, and insert into test server.
        InputStream privateKeyInputStream = Files.newInputStream(Paths.get(privateKeyFileName));
        FilePasswordProvider passphraseProvider = FilePasswordProvider.of(passphrase);
        java.security.KeyPair javasecKeyPair =
                SecurityUtils.loadKeyPairIdentity("generated_key",
                                                  privateKeyInputStream,
                                                  passphraseProvider);
        sshd.setKeyPairProvider(new MappedKeyPairProvider(javasecKeyPair));
//
//        // Need to use a non-standard port, as there may be an ssh server already running on this machine
//        sshd.setPort(2222);
//
//        // Set up a fall-back password authenticator to help in diagnosing failed test
//        sshd.setPasswordAuthenticator(new PasswordAuthenticator() {
//            public boolean authenticate(String username, String password, ServerSession session) {
//                fail("Tried to use password instead of public key authentication");
//                return false;
//            }
//        });
//
//        // This replaces the role of authorized_keys, indicating that this key is allowed to be used.
//        // Note that this is not actually determining that a private/public key match has happened; merely that
//        // this key is allowed.
//        ArrayList<PublicKey> publicKeys = new ArrayList<>();
//        publicKeys.add(javasecKeyPair.getPublic());
//        sshd.setPublickeyAuthenticator(new KeySetPublickeyAuthenticator(publicKeys));
//
//
//        // Locations of simulated remote and local repos.
//        Path remoteFull = directoryPath.resolve("remote");
//        Path remoteBrief = Paths.get("remote");
//        Path local = directoryPath.resolve("local");
//        console.info("Remote path full = " + remoteFull);
//        console.info("Remote path brief = " + remoteBrief);
//        console.info("Local path = " + local);
//
//        // Amazingly useful Git command setup provided by Mina.
//        sshd.setCommandFactory(new GitPackCommandFactory(directoryPath.toString()));
//
//        // Start the SSH test server.
//        sshd.start();
//
//        // Create a bare repo on the remote to be cloned.
//        Git remoteHandle = Git.init().setDirectory(remoteFull.toFile()).setBare(true).call();
//
//        // Clone the bare repo, using the SSH connection, to the local.
//        String remoteURL = "ssh://localhost:2222/"+remoteBrief;
//        console.info("Connecting to " + remoteURL);
//        ClonedRepoHelper helper = new ClonedRepoHelper(local, remoteURL, passphrase,
//                                                       new ElegitUserInfoTest(null, passphrase));
//        helper.obtainRepository(remoteURL);
//
//        // Verify that it is an SSH connection, then try a getch
//        assertEquals(helper.getCompatibleAuthentication(), AuthMethod.SSH);
//        helper.fetch(false);
//
//        // Create a new test file at the local repo
//        Path fileLocation = local.resolve("README.md");
//        FileWriter fw = new FileWriter(fileLocation.toString(), true);
//        fw.write("start");
//        fw.close();
//
//        // Commit, and push to remote
//        helper.addFilePathTest(fileLocation);
//        helper.commit("Appended to file");
//        PushCommand command = helper.prepareToPushAll();
//        helper.pushAll(command);
//
//        // Shut down test SSH server
//        sshd.stop();


    }
}
