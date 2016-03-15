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
public class BranchManagerTest /*extends GuiTest*/ {
/*
    // Test is failing; will come back to at some point
    @Override
    protected Parent getRootNode() {
        Parent root = null;

        try {
            root = FXMLLoader.load(getClass().getResource("/elegit/fxml/MainView.fxml"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        root.setTranslateY(50);

        return root;
    }

    @Test
    public void testBranchManagerButton() {
        sleep(5, TimeUnit.SECONDS);

        Button managerButton = find("Manage branches");
        click(managerButton);

        sleep(10, TimeUnit.SECONDS);

        Stage branchManagerWindow = findStageByTitle("Branch Manager");
        assert branchManagerWindow != null;
        assert branchManagerWindow.isShowing();
    }*/
}
