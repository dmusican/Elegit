package edugit;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Created by makik on 6/10/15.
 */
public class InitRepoView extends Stage{

    public InitRepoView() throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("resources/fxml/InitRepoView.fxml"));
        this.setTitle("EduGit");

        Scene scene = new Scene(root, 400, 300);
        this.setScene(scene);
        this.show();
    }

}
