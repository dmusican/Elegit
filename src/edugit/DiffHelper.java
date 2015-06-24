package edugit;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * Code is from the JGit-Cookbook.
 *  (https://github.com/centic9/jgit-cookbook)
 *
 *       This is a mix of
 *          - https://github.com/centic9/jgit-cookbook/blob/master/src/main/java/org/dstadler/jgit/porcelain/ShowChangedFilesBetweenCommits.java
 *          and
 *          - https://github.com/centic9/jgit-cookbook/blob/master/src/main/java/org/dstadler/jgit/porcelain/ShowFileDiff.java
 *
 * Thanks Dominik Stadler!
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

        // The {tree} will return the underlying tree-id instead of the commit-id itself!
        // For a description of what the carets do see e.g. http://www.paulboxley.com/blog/2011/06/git-caret-and-tilde
        // This means we are selecting the parent of the parent of current HEAD and
        // take the tree-ish of it. TODO: these aren't always giving the right diffs...
        ObjectId oldHead = this.repo.resolve("HEAD^^{tree}");
        ObjectId head = this.repo.resolve("HEAD^{tree}");

        // prepare the two iterators to compute the diff between
        ObjectReader reader = this.repo.newObjectReader();
        CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
        oldTreeIter.reset(reader, oldHead);
        CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
        newTreeIter.reset(reader, head);

        List<DiffEntry> diffs= new Git(this.repo).diff()
                .setNewTree(newTreeIter)
                .setOldTree(oldTreeIter)
                .setPathFilter(PathFilter.create(this.pathFilter))
                .call();

        for (DiffEntry entry : diffs) {
            DiffFormatter formatter = new DiffFormatter(diffOutputStream);
            formatter.setRepository(this.repo);
            formatter.format(entry);
        }

        return diffOutputStream.toString();
    }

    private ArrayList<Text> getColoredDiffList() throws GitAPIException, IOException {
        String diffText = this.getDiffString();

        ArrayList<Text> coloredDiffList = new ArrayList<>();

        String[] lines = diffText.split("\n");
        for (String line : lines) {
            Text text = new Text(line);
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

        scrollPane.setPadding(new Insets(10));

        return scrollPane;
    }
}
