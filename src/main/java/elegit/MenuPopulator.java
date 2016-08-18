package elegit;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;

import java.util.concurrent.atomic.AtomicBoolean;

public class MenuPopulator {

    private MenuBar menuBar;
    private AtomicBoolean populated;
    private SessionController sessionController;

    private static MenuPopulator instance;

    private MenuPopulator() {
        menuBar = new MenuBar();
        populated = new AtomicBoolean(false);
    }

    /**
     * Either returns the current MenuPopulator or creates a new one
     * @return MenuPopulator
     */
    public static MenuPopulator getInstance() {
        if (instance == null) {
            instance = new MenuPopulator();
        }
        return instance;
    }

    /**
     * Sets the SessionController that has all the git commands in it
     * @param sc SessionController
     */
    public void setSessionController(SessionController sc) {
        this.sessionController = sc;
    }

    /**
     * Returns a populated MenuBar
     * @return MenuBar
     * @throws IllegalStateException if the MenuBar doesn't exist
     */
    public MenuBar populate() throws IllegalStateException {
        if (menuBar == null) {
            throw new IllegalStateException();
        }
        if (isPopulated()) return menuBar;
        populated.set(true);

        Menu menuFile = initFileMenu();
        Menu menuEdit = initEditMenu();
        Menu menuRepository = initRepositoryMenu();

        menuBar.getMenus().addAll(menuFile, menuEdit, menuRepository);

        menuBar.setUseSystemMenuBar(true);


        return menuBar;
    }

    /**
     * Initializes the "File" menu
     * @return Menu
     */
    private Menu initFileMenu() {
        return new Menu("File");
    }

    /**
     * Initliazes the "Edit" menu
     * @return Menu
     */
    private Menu initEditMenu() {
        Menu menu = new Menu("Edit");

        MenuItem openGitIgnoreItem = new MenuItem(".gitignore...");
        openGitIgnoreItem.setOnAction(event ->
                GitIgnoreEditor.show(
                        SessionModel.getSessionModel().getCurrentRepoHelper(),
                        null));

        menu.getItems().add(openGitIgnoreItem);

        return menu;
    }

    /**
     * Initializes the "Repository" menu
     * @return Menu
     */
    private Menu initRepositoryMenu() {
        Menu menu = new Menu("Repository");

        menu.getItems().addAll(initBranchMenu(),
                initCheckoutMenu(),
                initCloneMenu(),
                initCommitMenu(),
                initFetchMenu(),
                initMergeMenu(),
                initPullMenu(),
                initPushMenu());

        return menu;
    }

    /**
     * Helper method that initializes the "Branch" sub-menu
     * @return Menu
     */
    private Menu initBranchMenu() {
        Menu branchMenu = new Menu("Branch");

        MenuItem branch = new MenuItem("create branch");
        branch.setOnAction(event -> sessionController.handleNewBranchButton());

        MenuItem branch_d = new MenuItem("delete branch");
        branch_d.setOnAction(event -> sessionController.handleCreateOrDeleteBranchButton("local"));

        MenuItem branch_r_d = new MenuItem("delete remote branch");
        branch_r_d.setOnAction(event -> sessionController.handleCreateOrDeleteBranchButton("remote"));

        branchMenu.getItems().addAll(branch, branch_d, branch_r_d);

        return branchMenu;
    }

    /**
     * Helper method that initializes the "Checkout" sub-menu
     * @return Menu
     */
    private Menu initCheckoutMenu() {
        Menu checkoutMenu = new Menu("Checkout");

        MenuItem checkout = new MenuItem("checkout branch");
        checkout.setOnAction(event -> sessionController.showBranchCheckout());

        checkoutMenu.getItems().addAll(checkout);

        return checkoutMenu;
    }

    /**
     * Helper method that initializes the "Clone" sub-menu
     * @return Menu
     */
    private Menu initCloneMenu() {
        Menu cloneMenu = new Menu("Clone");

        MenuItem clone = new MenuItem("clone");
        clone.setOnAction(event -> sessionController.handleCloneNewRepoOption());

        cloneMenu.getItems().addAll(clone);

        return cloneMenu;
    }

    /**
     * Helper method that initializes the "Commit" sub-menu
     * @return Menu
     */
    private Menu initCommitMenu() {
        Menu commitMenu = new Menu("Commit");

        MenuItem commit = new MenuItem("commit");
        commit.setOnAction(event -> sessionController.handleCommitNormal());

        MenuItem commit_a = new MenuItem("commit -a");
        commit_a.setOnAction(event -> sessionController.handleCommitAll());

        commitMenu.getItems().addAll(commit, commit_a);

        return commitMenu;
    }

    /**
     * Helper method that initializes the "Fetch" sub-menu
     * @return Menu
     */
    private Menu initFetchMenu() {
        Menu fetchMenu = new Menu("Fetch");

        MenuItem fetch = new MenuItem("fetch");
        fetch.setOnAction(event -> sessionController.handleNormalFetchButton());

        MenuItem fetch_p = new MenuItem("fetch -p");
        fetch_p.setOnAction(event -> sessionController.handlePruneFetchButton());

        fetchMenu.getItems().addAll(fetch, fetch_p);

        return fetchMenu;
    }

    /**
     * Helper method that initializes the "Merge" sub-menu
     * @return Menu
     */
    private Menu initMergeMenu() {
        Menu mergeMenu = new Menu("Merge");

        MenuItem merge = new MenuItem("merge from fetch");
        merge.setOnAction(event -> sessionController.mergeFromFetch());

        MenuItem merge_branch = new MenuItem("merge local branches");
        merge_branch.setOnAction(event -> sessionController.handleBranchMergeButton());

        mergeMenu.getItems().addAll(merge, merge_branch);

        return mergeMenu;
    }

    /**
     * Helper method that initializes the "Pull" sub-menu
     * @return Menu
     */
    private Menu initPullMenu() {
        Menu pullMenu = new Menu("Pull");

        MenuItem pull = new MenuItem("pull");
        pull.setOnAction(event -> sessionController.handlePullButton());

        pullMenu.getItems().addAll(pull);

        return pullMenu;
    }

    /**
     * Helper method that initializes the "Push" sub-menu
     * @return Menu
     */
    private Menu initPushMenu() {
        Menu pushMenu = new Menu("Push");

        MenuItem push = new MenuItem("push current branch");
        push.setOnAction(event -> sessionController.handlePushButton());

        MenuItem push_all = new MenuItem("push all");
        push_all.setOnAction(event -> sessionController.handlePushButton());

        MenuItem push_tags = new MenuItem("push --tags");
        push_tags.setOnAction(event -> sessionController.handlePushTagsButton());

        pushMenu.getItems().addAll(push, push_all, push_tags);

        return pushMenu;
    }

    private static boolean isPopulated() {
        return getInstance().populated.get();
    }

    public static synchronized void menuConfigNoRepo() {
        if (!isPopulated()) {
            getInstance().populate();
        }

        // disables .gitignore option in the "Edit" menu
        getInstance().menuBar.getMenus().get(1).getItems().get(0).setDisable(true);
        // disables the "Repository" menu
        getInstance().menuBar.getMenus().get(2).setDisable(true);
    }

    public static synchronized void menuConfigNormal() {
        if (!isPopulated()) {
            getInstance().populate();
        }

        // enables .gitignore option in the "Edit" menu
        getInstance().menuBar.getMenus().get(1).getItems().get(0).setDisable(false);
        // enables the "Repository" menu
        getInstance().menuBar.getMenus().get(2).setDisable(false);
    }
}
