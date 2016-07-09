package elegit;

import com.jcraft.jsch.*;
import javafx.application.Application;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.transport.*;
import org.junit.*;
import org.junit.rules.ExpectedException;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

import static org.junit.Assert.*;

public class AuthenticatedCloneTest {

    private Path directoryPath;
    private String testFileLocation;
    Path logPath;

    // Used to indicate that if password files are missing, then tests should just pass
    private boolean looseTesting;

    private static final String GITHUB_REMOTE_URL = "https://github.com/TheElegitTeam/testrepo.git";
    private static final String BITBUCKET_REMOTE_URL = "https://musicant@bitbucket.org/musicant/bbtestrepo.git";

    @Before
    public void setUp() throws Exception {
        initializeLogger();
        this.directoryPath = Files.createTempDirectory("unitTestRepos");
        directoryPath.toFile().deleteOnExit();
        testFileLocation = System.getProperty("user.home") + File.separator +
                           "elegitTests" + File.separator;
        File strictTestingFile = new File(testFileLocation + "strictAuthenticationTesting.txt");
        looseTesting = !strictTestingFile.exists();
    }

    @After
    public void tearDown() throws Exception {
        removeAllFilesFromDirectory(this.logPath.toFile());
    }

    // Helper method to avoid annoying traces from logger
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

    // Helper tear-down method:
    void removeAllFilesFromDirectory(File dir) {
        for (File file: dir.listFiles()) {
            if (file.isDirectory()) removeAllFilesFromDirectory(file);
            file.delete();
        }
    }

    @Test
    public void     testCloneHttpNoPassword() throws Exception {
        Path repoPath = directoryPath.resolve("testrepo");
        // Clone from dummy repo:
        String remoteURL = "https://github.com/TheElegitTeam/TestRepository.git";

        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider("", "");
        ClonedRepoHelper helper = new ClonedRepoHelper(repoPath, remoteURL, credentials);
        assertNotNull(helper);

    }

    @Test
    public void testLsHttpNoPassword() throws Exception {
        testLsHttpUsernamePassword("httpNoUsernamePassword.txt");
    }

    @Test
    public void testHttpUsernamePasswordPublic() throws Exception {
        testHttpUsernamePassword("httpUsernamePassword.txt", GITHUB_REMOTE_URL);
    }

    @Test
    public void testHttpUsernamePasswordPrivate() throws Exception {
        testHttpUsernamePassword("httpUsernamePasswordPrivate.txt", BITBUCKET_REMOTE_URL);
    }


    /* The httpUsernamePassword should contain three lines, containing:
        repo http(s) address
        username
        password
     */
    public void testHttpUsernamePassword(String filename, String remoteURL) throws Exception {
        Path repoPath = directoryPath.resolve("testrepo");
        File authData = new File(testFileLocation + filename);

        // If a developer does not have this file present, test should just pass.
        if (!authData.exists() && looseTesting)
            return;

        Scanner scanner = new Scanner(authData);
        String ignoreURL = scanner.next();
        String username = scanner.next();
        String password = scanner.next();
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(username, password);
        try {
            ClonedRepoHelper helper = new ClonedRepoHelper(repoPath, remoteURL, credentials);
            assertEquals(helper.getCompatibleAuthentication(),AuthMethod.HTTP);
            helper.fetch();
            Path fileLocation = repoPath.resolve("README.md");
            System.out.println(fileLocation);
            FileWriter fw = new FileWriter(fileLocation.toString(), true);
            fw.write("1");
            fw.close();
            helper.addFilePath(fileLocation);
            helper.commit("Appended to file");
            helper.pushAll();
            helper.pushTags();
        } catch (TransportException e) {
            e.printStackTrace();
            fail("Test failed; it is likely that you have not name/password correctly in the" +
                 "or you do not have access to the Bitbucket repo. Note that httpUsernamePassword.txt " +
                 "should have GitHub authentication info; httpUsernamePasswordPrivate.txt should have" +
                 "Bitbucket authentication info.");
        }
    }

    @Test
    public void testLshHttpUsernamePasswordPublic() throws Exception {
        testLsHttpUsernamePassword("httpUsernamePassword.txt");
    }

    @Test
    public void testLshHttpUsernamePasswordPrivate() throws Exception {
        testLsHttpUsernamePassword("httpUsernamePasswordPrivate.txt");
    }

    public void testLsHttpUsernamePassword(String filename) throws Exception {

        File authData = new File(testFileLocation + filename);

        // If a developer does not have this file present, test should just pass.
        if (!authData.exists())
            return;

        Scanner scanner = new Scanner(authData);
        String ignoreURL = scanner.next();
        String username = scanner.next();
        String password = scanner.next();
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(username, password);

        TransportCommand command = Git.lsRemoteRepository().setRemote(GITHUB_REMOTE_URL);
        RepoHelper helper = new RepoHelper();
        helper.wrapAuthentication(command, credentials);
        command.call();
    }

    @Test
    // Test Https access, with empty string credentials, to see if it works for a repo that is public
    // ... and verify it fails with a bad username or password
    public void testLsHttpUsernamePasswordEmpty() throws Exception {

        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider("a", "asdas");

        TransportCommand command =
                Git.lsRemoteRepository().setRemote("https://github.com/TheElegitTeam/TestRepository.git");
        //RepoHelper.wrapAuthentication(command, credentials);
        command.call();
    }


    /* The sshPassword should contain two lines:
        repo ssh address
        password
     */
    @Test
    public void testLsSshPassword() throws Exception {

        File urlFile = new File(testFileLocation + "sshPasswordURL.txt");
        Path passwordFile = Paths.get(testFileLocation,"sshPasswordPassword.txt");

        // If a developer does not have this file present, test should just pass.
        if ((!urlFile.exists() || !Files.exists(passwordFile) && looseTesting))
            return;

        Scanner scanner = new Scanner(urlFile);
        String remoteURL = scanner.next();

        List<String> userCredentials = Files.readAllLines(passwordFile);
        TransportCommand command = Git.lsRemoteRepository().setRemote(remoteURL);
        RepoHelper helper = new RepoHelper();
        helper.wrapAuthentication(command, new ElegitUserInfoTest(userCredentials.get(0), null));
        command.call();
    }

    @Rule
    public ExpectedException exception = ExpectedException.none();


    @Test
    public void testSshPassword() throws Exception {
        Path repoPath = directoryPath.resolve("testrepo");
        File urlFile = new File(testFileLocation + "sshPasswordURL.txt");
        Path passwordFile = Paths.get(testFileLocation,"sshPasswordPassword.txt");

        // If a developer does not have this file present, test should just pass.
        if ((!urlFile.exists() || !Files.exists(passwordFile) && looseTesting))
            return;

        Scanner scanner = new Scanner(urlFile);
        String remoteURL = scanner.next();
        scanner.close();
        scanner = new Scanner(passwordFile);
        String password = scanner.next();

        //ClonedRepoHelper helper = new ClonedRepoHelper(repoPath, remoteURL, new ElegitUserInfoTest(password, null));
        ClonedRepoHelper helper = new ClonedRepoHelper(repoPath, remoteURL, password,
                                                       new ElegitUserInfoTest(password,null));
        assertEquals(helper.getCompatibleAuthentication(),AuthMethod.SSH);
        helper.fetch();
        helper.pushAll();
        helper.pushTags();
    }

    @Test
    public void testSshPrivateKey() throws Exception {

        Path repoPath = directoryPath.resolve("testrepo");
        File urlFile = new File(testFileLocation + "sshPrivateKeyURL.txt");
        File passwordFile = new File(testFileLocation + "sshPrivateKeyPassword.txt");

        // If a developer does not have this file present, test should just pass.
        if ((!urlFile.exists() || !passwordFile.exists()) && looseTesting)
            return;

        Scanner scanner = new Scanner(urlFile);
        String remoteURL = scanner.next();
        scanner.close();
        scanner = new Scanner(passwordFile);
        String passphrase = scanner.next();

        ClonedRepoHelper helper = new ClonedRepoHelper(repoPath, remoteURL, passphrase,
                                                       new ElegitUserInfoTest(null, passphrase));
        assertEquals(helper.getCompatibleAuthentication(),AuthMethod.SSH);
        helper.fetch();
        helper.pushAll();
        helper.pushTags();
        scanner.close();
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
        System.out.println();
        for (TransportProtocol protocol : protocols) {
            if (protocol.canHandle(new URIish("https://github.com/TheElegitTeam/TestRepository.git"))) {
                assertEquals(protocol.getName(), "HTTP");
                assertNotEquals(protocol.getName(), "SSH");
            }

            if (protocol.canHandle(new URIish("git@github.com:TheElegitTeam/TestRepository.git"))) {
                assertEquals(protocol.getName(), "SSH");
                assertNotEquals(protocol.getName(), "HTTP");
            }
        }
    }

    @Test
    public void testSshCallback() throws Exception {

        File urlFile = new File(testFileLocation + "keypairTesting.txt");

        // If a developer does not have this file present, test should just pass.
        if (!urlFile.exists() && looseTesting) {
            System.out.println("Ignoring keypair testing. Create a keypairTesting.txt file if you wish.");
            return;

        }

        LsRemoteCommand command = Git.lsRemoteRepository();
        //command.setRemote("https://github.com/TheElegitTeam/TestRepository.git");
        command.setRemote("git@github.com:TheElegitTeam/TestRepository.git");

        SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
            @Override
            protected void configure(OpenSshConfig.Host host, Session session ) {
                // do nothing
            }
        };

        command.setTransportConfigCallback( new TransportConfigCallback() {
            @Override
            public void configure( Transport transport ) {
                System.out.println(transport.getClass());
                // This cast will fail if SSH is not the protocol used
                SshTransport sshTransport = ( SshTransport )transport;
                sshTransport.setSshSessionFactory( sshSessionFactory );

        }
        } );
        // Command will fail if config not set up correctly; uses public/private key

        try {
            command.call();
        } catch (TransportException e) {
            fail("Public/private key authentication failed. You should set this up in your ssh/.config.");

        }

    }

    // sample code. Used for reference, can delete once I finally have all this working.
    // From https://gist.githubusercontent.com/ymnk/2318108/raw/82819389a225265c2aa4ca11afc0b35e938607fe/UserAuthPubKey.java
    public void UserAuthPubKey() {
        try{
            JSch jsch=new JSch();

            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Choose your privatekey(ex. ~/.ssh/id_dsa)");
            chooser.setFileHidingEnabled(false);
            int returnVal = chooser.showOpenDialog(null);
            if(returnVal == JFileChooser.APPROVE_OPTION) {
                System.out.println("You chose "+
                        chooser.getSelectedFile().getAbsolutePath()+".");
                jsch.addIdentity(chooser.getSelectedFile().getAbsolutePath()
//			 , "passphrase"
                );
            }

            String host=null;
            host=JOptionPane.showInputDialog("Enter username@hostname",
                    System.getProperty("user.name")+
                            "@localhost");
            String user=host.substring(0, host.indexOf('@'));
            host=host.substring(host.indexOf('@')+1);

            Session session=jsch.getSession(user, host, 22);

            // username and passphrase will be given via UserInfo interface.
            UserInfo ui=new MyUserInfo();
            session.setUserInfo(ui);
            session.connect();

            Channel channel=session.openChannel("shell");

            channel.setInputStream(System.in);
            channel.setOutputStream(System.out);

            channel.connect();
        }
        catch(Exception e){
            System.out.println(e);
        }
    }


    public static class MyUserInfo implements UserInfo, UIKeyboardInteractive {
        public String getPassword(){ return null; }
        public boolean promptYesNo(String str){
            Object[] options={ "yes", "no" };
            int foo=JOptionPane.showOptionDialog(null,
                    str,
                    "Warning",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null, options, options[0]);
            return foo==0;
        }

        String passphrase;
        JTextField passphraseField=(JTextField)new JPasswordField(20);

        public String getPassphrase(){ return passphrase; }
        public boolean promptPassphrase(String message){
            Object[] ob={passphraseField};
            int result=
                    JOptionPane.showConfirmDialog(null, ob, message,
                            JOptionPane.OK_CANCEL_OPTION);
            if(result==JOptionPane.OK_OPTION){
                passphrase=passphraseField.getText();
                return true;
            }
            else{ return false; }
        }
        public boolean promptPassword(String message){ return true; }
        public void showMessage(String message){
            JOptionPane.showMessageDialog(null, message);
        }
        final GridBagConstraints gbc =
                new GridBagConstraints(0,0,1,1,1,1,
                        GridBagConstraints.NORTHWEST,
                        GridBagConstraints.NONE,
                        new Insets(0,0,0,0),0,0);
        private Container panel;
        public String[] promptKeyboardInteractive(String destination,
                                                  String name,
                                                  String instruction,
                                                  String[] prompt,
                                                  boolean[] echo){
            panel = new JPanel();
            panel.setLayout(new GridBagLayout());

            gbc.weightx = 1.0;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.gridx = 0;
            panel.add(new JLabel(instruction), gbc);
            gbc.gridy++;

            gbc.gridwidth = GridBagConstraints.RELATIVE;

            JTextField[] texts=new JTextField[prompt.length];
            for(int i=0; i<prompt.length; i++){
                gbc.fill = GridBagConstraints.NONE;
                gbc.gridx = 0;
                gbc.weightx = 1;
                panel.add(new JLabel(prompt[i]),gbc);

                gbc.gridx = 1;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.weighty = 1;
                if(echo[i]){
                    texts[i]=new JTextField(20);
                }
                else{
                    texts[i]=new JPasswordField(20);
                }
                panel.add(texts[i], gbc);
                gbc.gridy++;
            }

            if(JOptionPane.showConfirmDialog(null, panel,
                    destination+": "+name,
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE)
                    == JOptionPane.OK_OPTION){
                String[] response=new String[prompt.length];
                for(int i=0; i<prompt.length; i++){
                    response[i]=texts[i].getText();
                }
                return response;
            }
            else{
                return null;  // cancel
            }
        }
    }

}


