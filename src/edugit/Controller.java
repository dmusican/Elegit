package edugit;


import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;

public class Controller {

/*
Resources I've been using:
    * My test GitHub repo: https://github.com/grahamearley/jgit-test (private)
    * JGit cookbook: https://github.com/centic9/jgit-cookbook
    * This stackoverflow: http://stackoverflow.com/questions/6861881/jgit-cannot-find-a-tutorial-or-simple-example
    * On Authentication: http://www.codeaffine.com/2014/12/09/jgit-authentication/
            ( still need to read this more thoroughly )

 */
    public static void main(String args[]) throws GitAPIException, IOException {
        RepoModel repo = new RepoModel(SECRET_CONSTANTS.TEST_GITHUB_TOKEN);
//        repo.cloneRepo();
        repo.findRepo();
        repo.pushNewFile("another.txt", "great commit messages");
        repo.closeRepo();
    }
}
