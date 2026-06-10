/// Root page of Craft Launcher with a dark sidebar and content area.
package org.Open_code_Studio.jmcl.craft;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.Open_code_Studio.jmcl.game.DefaultGameRepository;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Main UI container with a dark sidebar on the left and a content area on the right.
 */
public final class CraftRootPage {

    private final Stage stage;
    private final DefaultGameRepository gameRepository;
    private final BorderPane root;
    private final StackPane contentArea;
    private final Map<String, Node> pages = new HashMap<>();
    private final ToggleGroup navGroup = new ToggleGroup();

    /// Key used to identify which nav item is active.
    private String currentPage = "dashboard";

    /**
     * @param stage      the primary stage
     * @param workDir    the JMCL working directory (contains .minecraft, config, etc.)
     */
    public CraftRootPage(Stage stage, Path workDir) {
        this.stage = stage;
        this.gameRepository = new DefaultGameRepository(workDir.resolve(".minecraft"));

        root = new BorderPane();
        root.getStyleClass().add("craft-root");

        // Refresh the version list so getVersions() returns data
        gameRepository.refreshVersions();

        // Sidebar
        VBox sidebar = createSidebar();
        root.setLeft(sidebar);

        // Content area
        contentArea = new StackPane();
        contentArea.getStyleClass().add("craft-content");
        root.setCenter(contentArea);

        // Show default page
        navigateTo("dashboard");
    }

    /** Returns the root layout node. */
    public BorderPane getRoot() {
        return root;
    }

    // ---- Sidebar ----

    private VBox createSidebar() {
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("craft-sidebar");

        // Logo / brand
        HBox logo = new HBox();
        logo.getStyleClass().add("craft-sidebar-logo");
        Text logoText = new Text("Craft");
        logoText.getStyleClass().add("craft-sidebar-logo-text");
        logo.getChildren().add(logoText);
        sidebar.getChildren().add(logo);

        // Navigation items
        sidebar.getChildren().add(createNavButton("dashboard", "🏠 Dashboard", true));
        sidebar.getChildren().add(createNavButton("games",     "🎮 Games",      false));
        sidebar.getChildren().add(createNavButton("mods",      "🔧 Mods",       false));
        sidebar.getChildren().add(createNavButton("downloads", "📥 Downloads",  false));
        sidebar.getChildren().add(createNavButton("settings",  "⚙ Settings",    false));

        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().add(spacer);

        // Account area at bottom
        HBox account = new HBox();
        account.getStyleClass().add("craft-sidebar-account");
        Text accountText = new Text("Offline Player");
        account.getChildren().add(accountText);
        sidebar.getChildren().add(account);
        account.setOnMouseClicked(e -> {
            // Placeholder for account management
        });

        return sidebar;
    }

    private ToggleButton createNavButton(String id, String label, boolean selected) {
        ToggleButton btn = new ToggleButton(label);
        btn.getStyleClass().add("craft-sidebar-btn");
        btn.setToggleGroup(navGroup);
        btn.setUserData(id);
        btn.setMaxWidth(Double.MAX_VALUE);
        if (selected) {
            btn.setSelected(true);
            btn.getStyleClass().add("active");
        }
        btn.selectedProperty().addListener((obs, was, now) -> {
            if (now) {
                btn.getStyleClass().add("active");
                navigateTo((String) btn.getUserData());
            } else {
                btn.getStyleClass().remove("active");
            }
        });
        return btn;
    }

    // ---- Navigation ----

    private void navigateTo(String pageId) {
        currentPage = pageId;
        Node page = pages.get(pageId);
        if (page == null) {
            page = createPage(pageId);
            pages.put(pageId, page);
        }
        contentArea.getChildren().clear();
        contentArea.getChildren().add(page);
    }

    private Node createPage(String pageId) {
        return switch (pageId) {
            case "dashboard" -> createDashboardPage();
            case "games"     -> createGamesPage();
            case "mods"      -> createModsPage();
            case "downloads" -> createDownloadsPage();
            case "settings"  -> createSettingsPage();
            default -> new Label("Unknown page: " + pageId);
        };
    }

    // ---- Pages ----

    private Node createDashboardPage() {
        VBox page = new VBox(20);
        page.setPadding(new Insets(24));

        Text title = new Text("Dashboard");
        title.getStyleClass().add("craft-section-title");

        // Stats cards
        HBox stats = new HBox(16);
        stats.getStyleClass().add("craft-stats-grid");
        stats.getChildren().addAll(
                createStatCard("Installed Games", String.valueOf(gameRepository.getVersions().size())),
                createStatCard("Installed Mods", "0"),
                createStatCard("Last Played", "—")
        );

        page.getChildren().addAll(title, stats);
        return new ScrollPane(page) {{
            getStyleClass().add("craft-scroll-pane");
            setFitToWidth(true);
        }};
    }

    private VBox createStatCard(String label, String value) {
        VBox card = new VBox(8);
        card.getStyleClass().add("craft-card");
        card.setPrefWidth(260);
        card.setPadding(new Insets(20));

        Text title = new Text(label);
        title.getStyleClass().add("craft-card-title");
        Text val = new Text(value);
        val.getStyleClass().add("craft-card-value");

        card.getChildren().addAll(title, val);
        return card;
    }

    private Node createGamesPage() {
        VBox page = new VBox(16);
        page.setPadding(new Insets(24));

        Text title = new Text("Game Versions");
        title.getStyleClass().add("craft-section-title");

        ListView<String> gameList = new ListView<>();
        gameList.getStyleClass().add("craft-list");
        gameRepository.getVersions().forEach(v -> gameList.getItems().add(v.getId()));

        Button launchBtn = new Button("Launch");
        launchBtn.getStyleClass().add("craft-btn-primary");

        page.getChildren().addAll(title, gameList, launchBtn);
        return new ScrollPane(page) {{
            getStyleClass().add("craft-scroll-pane");
            setFitToWidth(true);
        }};
    }

    private Node createModsPage() {
        VBox page = new VBox(16);
        page.setPadding(new Insets(24));

        Text title = new Text("Mods");
        title.getStyleClass().add("craft-section-title");
        Text placeholder = new Text("Mod management coming soon.");
        placeholder.getStyleClass().add("craft-label-muted");

        page.getChildren().addAll(title, placeholder);
        return new ScrollPane(page) {{
            getStyleClass().add("craft-scroll-pane");
            setFitToWidth(true);
        }};
    }

    private Node createDownloadsPage() {
        VBox page = new VBox(16);
        page.setPadding(new Insets(24));

        Text title = new Text("Download Center");
        title.getStyleClass().add("craft-section-title");
        Text placeholder = new Text("Download management coming soon.");
        placeholder.getStyleClass().add("craft-label-muted");

        page.getChildren().addAll(title, placeholder);
        return new ScrollPane(page) {{
            getStyleClass().add("craft-scroll-pane");
            setFitToWidth(true);
        }};
    }

    private Node createSettingsPage() {
        VBox page = new VBox(20);
        page.setPadding(new Insets(24));

        Text title = new Text("Settings");
        title.getStyleClass().add("craft-section-title");

        VBox group = new VBox(16);
        group.getStyleClass().add("craft-settings-group");

        group.getChildren().addAll(
                createSettingRow("Game Directory", gameRepository.getBaseDirectory().toString()),
                createSettingRow("Language", "English"),
                createSettingRow("Memory (MB)", "2048")
        );

        page.getChildren().addAll(title, group);
        return new ScrollPane(page) {{
            getStyleClass().add("craft-scroll-pane");
            setFitToWidth(true);
        }};
    }

    private HBox createSettingRow(String label, String value) {
        HBox row = new HBox(16);
        row.getStyleClass().add("craft-settings-row");
        Text lbl = new Text(label);
        lbl.getStyleClass().add("craft-settings-label");
        Text val = new Text(value);
        val.getStyleClass().add("craft-label");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().addAll(lbl, spacer, val);
        return row;
    }
}