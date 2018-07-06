package elegit.repofile;

import elegit.models.RepoHelper;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import net.jcip.annotations.ThreadSafe;
import org.eclipse.jgit.api.errors.GitAPIException;
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
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * A DiffHelper helps interact with the diff for a given file in a repository.
 * This class reads, formats, and returns the string in a ScrollPane.
 *
 *  ---
 *
 * Some code is from the JGit-Cookbook, with modification.
 *  (https://github.com/centic9/jgit-cookbook)
 *    - https://github.com/centic9/jgit-cookbook/blob/master/src/main/java/org/dstadler/jgit/porcelain/ShowChangedFilesBetweenCommits.java
 *    and
 *    - https://github.com/centic9/jgit-cookbook/blob/master/src/main/java/org/dstadler/jgit/porcelain/ShowFileDiff.java
 *
 * This is considered a derivative work under the Apache 2.0 license, under which jgit-cookbook is licensed.
 *
 */
@ThreadSafe
// ... because JGit is threadsafe so long as you aren't making changes to a working directory, and because
// the FX elements being instantiated aren't on the FX thread (yet)
public class DiffHelper {

    private final Repository repo;
    private final String pathFilter;

    public DiffHelper(Path relativeFilePath, RepoHelper repo) {
        this.repo = repo.getRepo();
        this.pathFilter = relativeFilePath.toString();
    }

    private String getDiffString() throws IOException {
        ObjectId head = this.repo.resolve("HEAD");
        if(head == null) return "";

        // The following code is largely written by Tk421 on StackOverflow
        //      (http://stackoverflow.com/q/23486483)
        // Thanks! NOTE: comments are mine.
        ByteArrayOutputStream diffOutputStream = new ByteArrayOutputStream();
        DiffFormatter formatter = new DiffFormatter(diffOutputStream);
        formatter.setRepository(this.repo);
        formatter.setPathFilter(PathFilter.create(this.pathFilter.replaceAll("\\\\","/")));

        AbstractTreeIterator commitTreeIterator = prepareTreeParser(this.repo, head.getName());
        FileTreeIterator workTreeIterator = new FileTreeIterator(this.repo);

        // Scan gets difference between the two iterators.
        formatter.format(commitTreeIterator, workTreeIterator);

        return diffOutputStream.toString();
    }

    private List<Text> getColoredDiffList() throws IOException {
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
                text.setText(" "+text.getText());
                text.setId("unchangedDiffText");
            }
            coloredDiffList.add(text);
        }

        return Collections.unmodifiableList(coloredDiffList);
    }

    public ScrollPane getDiffScrollPane() throws IOException {
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
        try (RevWalk walk = new RevWalk(repository)){
            RevCommit commit = walk.parseCommit(ObjectId.fromString(objectId));
            RevTree tree = walk.parseTree(commit.getTree().getId());

            CanonicalTreeParser oldTreeParser = new CanonicalTreeParser();
            try (ObjectReader oldReader = repository.newObjectReader()){
                oldTreeParser.reset(oldReader, tree.getId());
            }

            walk.dispose();
            return oldTreeParser;
        }
    }
}
