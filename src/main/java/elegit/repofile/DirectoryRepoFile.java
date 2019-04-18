package elegit.repofile;

import elegit.Main;
import elegit.models.RepoHelper;
import javafx.scene.control.Button;
import net.jcip.annotations.ThreadSafe;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A subclass of RepoFile that contains a directory within the repository.
 * This subclass is different from its parent in that it can hold children.
 * This class is a view, controller, and model all mixed in one. That said. the model aspects are minimal, and the
 * view is mostly just a button and a context menu. Most notably, because most of the code is view oriented, ALL OF IT
 * should only be run from the JavaFX thread. In principle, a handful of method could be run elsewhere, but they're
 * fast anyway and not much would be gained; and most of this is FX work that should be done on the thread.
 *
 */
@ThreadSafe
// but only threadsafe because of the asserts on the FX thread nearly everywhere. No guarantees if any of those go;
// this needs to be thought through
public class DirectoryRepoFile extends RepoFile {

    private final AtomicBoolean showFullPath;

    public DirectoryRepoFile(Path filePath, RepoHelper repo) {
        super(filePath, repo);
        Main.assertFxThread();
        this.showFullPath = new AtomicBoolean(false);
    }

    public DirectoryRepoFile(String filePathString, RepoHelper repo) {
        this(Paths.get(filePathString), repo);
        Main.assertFxThread();
    }

    @Override
    protected Button initialDiffButton() {
        return null;
    }

    @Override
    public String toString() {
        Main.assertFxThread();
        return this.showFullPath.get() ? this.getFilePath().toString() : this.getFilePath().getFileName().toString();
    }

    @Override public boolean canAdd() {
        return true;
    }

    @Override public boolean canRemove() {
        return true;
    }

    public void setShowFullPath(Boolean b) {
        Main.assertFxThread();
        this.showFullPath.set(b);
    }
}
