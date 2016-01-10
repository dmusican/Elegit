package main.java.elegit.tests;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.junit.Test;
import org.loadui.testfx.GuiTest;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by grahamearley on 1/10/16.
 */
public class BranchManagerTest extends GuiTest {

    @Override
    protected Parent getRootNode() {
        Parent root = null;

        try {
            root = FXMLLoader.load(getClass().getResource("/elegit/fxml/MainView.fxml"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return root;
    }

    // The window size is too big for the Manage Branches button to be clicked!
    public void setCustomStage() {
        Platform.runLater(() -> {
            stage.close();

            Parent root = getRootNode();
            stage.setTitle("Elegit");

            Scene scene = new Scene(root, 1200, 650); // width, height
            stage.setScene(scene);
            stage.show();
            sleep(3, TimeUnit.SECONDS);
        });
    }

    @Test
    public void testBranchManagerButton() {
        Button managerButton = find("Manage branches");
        click(managerButton);

        sleep(5, TimeUnit.SECONDS);

        click(managerButton);

        Stage branchManagerWindow = findStageByTitle("Branch Manager");
        assert branchManagerWindow.isShowing();

    }
}
