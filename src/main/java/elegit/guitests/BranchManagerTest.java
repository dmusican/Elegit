package main.java.elegit.guitests;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.junit.BeforeClass;
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

    @Test
    public void testBranchManagerButton() {
        sleep(5, TimeUnit.SECONDS);

        Button managerButton = find("Manage Branches");
        click(managerButton);

        Stage branchManagerWindow = findStageByTitle("Branch Manager");
        assert branchManagerWindow.isShowing();
    }
}
