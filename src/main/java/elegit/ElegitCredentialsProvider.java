package elegit;

import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * Created by dmusican on 5/25/16.
 */
public class ElegitCredentialsProvider extends CredentialsProvider {

    // Used for unit testing
    private File credentialsFile;

    public ElegitCredentialsProvider() {
        super();
        this.credentialsFile = null;
    }

    public ElegitCredentialsProvider(File credentialsFile) {
        super();
        this.credentialsFile = credentialsFile;
    }

    @Override
    public boolean isInteractive() {
        return true;
    }

    @Override
    public boolean supports(CredentialItem... credentialItems) {
        for(CredentialItem item : credentialItems) {
            System.out.println("Supports: " + item);
        }
        return true;
    }

    @Override
    public boolean get(URIish urIish, CredentialItem... credentialItems) throws UnsupportedCredentialItem {
        Scanner scanner = null;
        if (credentialsFile != null) {
            try {
                scanner = new Scanner(credentialsFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        for(CredentialItem item : credentialItems) {
            String nextValue = scanner.next();
            if (item instanceof CredentialItem.Username) {
                ((CredentialItem.Username)item).setValue(nextValue);
            } else if (item instanceof CredentialItem.Password) {
                ((CredentialItem.Password)item).setValue(nextValue.toCharArray());
            } else if (item instanceof CredentialItem.StringType) {
                ((CredentialItem.StringType)item).setValue(nextValue);
            } else {
                System.out.println(item);
                System.out.println(item.getPromptText());
                throw new UnsupportedCredentialItem(urIish, "Case not covered in ElegitCredentialsProvider");
            }

        }
        return true;
    }
}
