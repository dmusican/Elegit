package main.java.edugit;

import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.FileTreeIterator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * A DiffHelper helps interact with the diff for a given file in a repository.
 * This class reads, formats, and returns the string in a ScrollPane.
 *
 *  ---
 *
 * Some code is from the JGit-Cookbook.
 *  (https://github.com/centic9/jgit-cookbook)
 *    - https://github.com/centic9/jgit-cookbook/blob/master/src/main/java/org/dstadler/jgit/porcelain/ShowChangedFilesBetweenCommits.java
 *    and
 *    - https://github.com/centic9/jgit-cookbook/blob/master/src/main/java/org/dstadler/jgit/porcelain/ShowFileDiff.java
 *
 * TODO: Licensing?!
 *
 */
public class DiffHelper {

    Repository repo;
    String pathFilter;

    public DiffHelper(Path relativeFilePath, Repository repo) throws IOException {
        this.repo = repo;
        this.pathFilter = relativeFilePath.toString();
    }

    private String getDiffString() throws GitAPIException, IOException {
        ByteArrayOutputStream diffOutputStream = new ByteArrayOutputStream();

        ObjectId head = this.repo.resolve("HEAD");

        // The following code is largely written by Tk421 on StackOverflow
        //      (http://stackoverflow.com/q/23486483)
        // Thanks! NOTE: comments are mine.
        DiffFormatter formatter = new DiffFormatter(diffOutputStream);
        formatter.setRepository(this.repo);

        AbstractTreeIterator commitTreeIterator = prepareTreeParser(this.repo, head.getName());
        FileTreeIterator workTreeIterator = new FileTreeIterator(this.repo);

        // Scan gets difference between the two iterators.
        List<DiffEntry> diffs = formatter.scan(commitTreeIterator, workTreeIterator);

        for (DiffEntry entry : diffs) {
            if (entry.getNewPath().equals(this.pathFilter)) {
                // Only output the diff if it's for the file in question
                formatter.format(entry);
            }
        }

        return diffOutputStream.toString();
    }

    private ArrayList<Text> getColoredDiffList() throws GitAPIException, IOException {
        String diffText = this.getDiffString();

        ArrayList<Text> coloredDiffList = new ArrayList<>();

        String[] lines = diffText.split("\n");
        for (String line : lines) {
            Text text = new Text(line);
            text.getStyleClass().add("diffText");

            if (line.length() > 0 && line.charAt(0) == '+') {
                text.setId("addedDiffText");
            } else if (line.length() > 0 && line.charAt(0) == '-') {
                text.setId("deletedDiffText");
            } else if (line.length() > 1 && line.charAt(0) == '@' && line.charAt(1) == '@') {
                text.setId("gitAnnotationDiffText");
            } else {
                text.setId("unchangedDiffText");
            }
            coloredDiffList.add(text);
        }

        return coloredDiffList;
    }

    public ScrollPane getDiffScrollPane() throws GitAPIException, IOException {
        ScrollPane scrollPane = new ScrollPane();

        VBox verticalListOfColoredDiffs = new VBox();
        verticalListOfColoredDiffs.getChildren().setAll(this.getColoredDiffList());
        
        scrollPane.setContent(verticalListOfColoredDiffs);

        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);

        scrollPane.setMaxWidth(800);
        scrollPane.setMaxHeight(400);

        scrollPane.setPadding(new Insets(0, 0, 0, 10));

        return scrollPane;
    }

    private static AbstractTreeIterator prepareTreeParser(Repository repository, String objectId) throws IOException {
        // from the commit we can build the tree which allows us to construct the TreeParser
        RevWalk walk = new RevWalk(repository);
        RevCommit commit = walk.parseCommit(ObjectId.fromString(objectId));
        RevTree tree = walk.parseTree(commit.getTree().getId());

        CanonicalTreeParser oldTreeParser = new CanonicalTreeParser();
        ObjectReader oldReader = repository.newObjectReader();
        try {
            oldTreeParser.reset(oldReader, tree.getId());
        } finally {
            oldReader.release();
        }

        walk.dispose();

        return oldTreeParser;
    }
}
