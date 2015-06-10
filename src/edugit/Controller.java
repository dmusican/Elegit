package edugit;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;

public abstract class Controller{

    public final String defaultPath = System.getProperty("user.home")+File.separator+"Desktop"+File.separator+"TestClone";

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

    public String getRelativePath(File parent, File file){
        return parent.toURI().relativize(file.toURI()).getPath();
    }
}
