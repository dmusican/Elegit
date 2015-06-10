package edugit;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import java.io.File;

/**
 * Abstract super class for controllers that contains some helpful methods and constants
 */
public abstract class Controller{

    // For testing purposes, a default path to a directory on the users desktop (Mac only)
    public final String defaultPath = System.getProperty("user.home")+File.separator+"Desktop"+File.separator+"TestClone";

    /**
    * Pops up a file or directory chooser for the user and returns the selected File object
    *
    * @param isDirectory whether to show the directory chooser (true) or the file chooser (false)
    * @param title the title of the chooser
    * @param parent the parent window of the chooser
    * @return the selected file or directory
    */
    public File getPathFromChooser(boolean isDirectory, String title, Window parent){
        File path = new File(defaultPath);

        File returnFile;
        if(isDirectory) {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle(title);
            chooser.setInitialDirectory(path.getParentFile());

            returnFile = chooser.showDialog(parent);
        }else{
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(title);
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("All Files", "*.*"),
                    new FileChooser.ExtensionFilter("Text Files", "*.txt"));

            returnFile = fileChooser.showOpenDialog(parent);
        }
        return returnFile;
    }

    /**
    * Returns the relative path between two files. If parent is not a parent directory
    * of targetFile, returns the full path to targetFile
    *
    * @param parent the parent directory to be used as a point of reference
    * @param targetFile the file whose relative path will be returned
    * @return the path to targetFile with the path to parent truncated from the beginning
    */
    public String getRelativePath(File parent, File targetFile){
        return parent.toURI().relativize(targetFile.toURI()).getPath();
    }
}
