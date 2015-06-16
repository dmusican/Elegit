package edugit;

/**
 * Created by grahamearley on 6/16/15.
 */
public class RepoOwner {

    private String username;
    private String password;

    public RepoOwner(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

}
