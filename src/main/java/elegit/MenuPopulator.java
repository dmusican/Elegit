package main.java.elegit;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;

public class MenuPopulator {

    private MenuBar menuBar;

    private static MenuPopulator instance;

    private MenuPopulator() {
        menuBar = new MenuBar();
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
}
