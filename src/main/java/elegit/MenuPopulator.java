package main.java.elegit;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;

import java.util.concurrent.atomic.AtomicBoolean;

public class MenuPopulator {

    private MenuBar menuBar;
    private AtomicBoolean populated;

    private static MenuPopulator instance;

    private MenuPopulator() {
        menuBar = new MenuBar();
        populated = new AtomicBoolean(false);
    }

    public static MenuPopulator getInstance() {
        if (instance == null) {
            instance = new MenuPopulator();
        }
        return instance;
    }

    public MenuBar populate() throws IllegalStateException {
        if (menuBar == null) {
            throw new IllegalStateException();
        }
        if (isPopulated()) return menuBar;
        populated.set(true);

        Menu menuFile = new Menu("File");

        Menu menuEdit = new Menu("Edit");
        MenuItem openGitIgnoreItem = new MenuItem(".gitignore...");
        openGitIgnoreItem.setOnAction(event ->
                GitIgnoreEditor.show(
                        SessionModel.getSessionModel().getCurrentRepoHelper(),
                        null));
        menuEdit.getItems().add(openGitIgnoreItem);

        menuBar.getMenus().addAll(menuFile, menuEdit);
        return menuBar;
    }

    private static boolean isPopulated() {
        return getInstance().populated.get();
    }

    public static synchronized void menuConfigNoRepo() {
        if (!isPopulated()) {
            getInstance().populate();
        }

        getInstance().menuBar.getMenus().get(1).getItems().get(0).setDisable
                (true);
    }

    public static synchronized void menuConfigNormal() {
        if (!isPopulated()) {
            getInstance().populate();
        }

        getInstance().menuBar.getMenus().get(1).getItems().get(0).setDisable
                (false);
    }
}
