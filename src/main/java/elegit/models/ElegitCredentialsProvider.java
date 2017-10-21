package elegit.models;

import org.apache.http.annotation.ThreadSafe;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@ThreadSafe
public class ElegitCredentialsProvider extends CredentialsProvider {

    // Used for unit testing
    private final List<String> userCredentials;

    public ElegitCredentialsProvider(List<String> userCredentials) {
        super();
        if (userCredentials != null) {
            this.userCredentials = Collections.unmodifiableList(new ArrayList<>(userCredentials));
        } else {
            this.userCredentials = userCredentials;
        }
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
        if (userCredentials != null) {
            Iterator<String> userCredentialsIterator = userCredentials.iterator();
            for(CredentialItem item : credentialItems) {
                String nextValue = userCredentialsIterator.next();
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
        }
        return true;
    }
}