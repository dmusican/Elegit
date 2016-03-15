package main.java.elegit;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by dmusican on 2/13/16.
 */
public class ClonedRepoHelperBuilderTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testGetPrevRepoName() throws Exception {
        assertTrue(4 == 2+2);
    }

    @Test
    public void testLsRemoteOnHTTP() throws Exception {
        LsRemoteCommand command = Git.lsRemoteRepository();
        command.setRemote("https://github.com/TheElegitTeam/TestRepository.git");
        command.call();

    }

}