/// Play-style minimal UI for JMCL.
///
/// When {@code config().getLauncherType()} is {@code "PLAYER"},
/// this pane replaces the normal sidebar-based RootPage
/// with a simplified LauncherX-style interface.
///
/// Layout:
/// <ul>
///   <li>Center: content area switching by bottom tabs</li>
///   <li>Bottom bar: player info (left), launch button (right), tab buttons</li>
/// </ul>
package org.Open_code_Studio.jmcl.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import org.Open_code_Studio.jmcl.auth.Account;
import org.Open_code_Studio.jmcl.game.JMCLGameRepository;
import org.Open_code_Studio.jmcl.game.Version;
import org.Open_code_Studio.jmcl.setting.Accounts;
import org.Open_code_Studio.jmcl.setting.Profile;
import org.Open_code_Studio.jmcl.setting.Profiles;
import org.Open_code_Studio.jmcl.ui.versions.Versions;
import org.Open_code_Studio.jmcl.util.Lang;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.Open_code_Studio.jmcl.util.i18n.I18n.i18n;

/**
 * A LauncherX-style root pane with bottom navigation.
 */
public final class PlayModePane extends StackPane {

    // ---- tabs ----
    private static final int TAB_HOME = 0;
    private static final int TAB_ACCOUNT = 1;
    private static final int TAB_DOWNLOAD = 2;
    private static final int TAB_SETTINGS = 3;

    private final StackPane contentArea = new StackPane();
    private final ComboBox<String> versionCombo = new ComboBox<>();
    private final VBox homePage;
    private final VBox accountPage;
    private final VBox downloadPage;
    private final VBox settingsPage;

    @Nullable
    private Profile profile;

    /**
     * @param repository the game repository to load versions from
     */
    public PlayModePane(JMCLGameRepository repository) {
        getStyleClass().add("play-mode-pane");

        // Build tab pages
        homePage = buildHomePage(repository);
        accountPage = buildAccountPage();
        downloadPage = buildDownloadPage();
        settingsPage = buildSettingsPage();

        // Default to home
        contentArea.getChildren().add(homePage);

        // Bottom bar
        VBox bottomBar = buildBottomBar();

        // ---- root layout: content fills, bottom bar pinned ----
        BorderPane root = new BorderPane();
        root.setCenter(contentArea);
        root.setBottom(bottomBar);
        getChildren().add(root);
    }

    // ========================================================================
    //  Pages
    // ========================================================================

    private VBox buildHomePage(JMCLGameRepository repository) {
        VBox page = new VBox(16);
        page.getStyleClass().add("play-page");
        page.setAlignment(Pos.CENTER);
        page.setPadding(new Insets(48, 32, 16, 32));

        Text title = new Text(i18n("firstlaunch.welcome.title"));
        title.getStyleClass().add("play-page-title");

        // Version selector
        versionCombo.getStyleClass().add("play-version-combo");
        versionCombo.setPromptText(i18n("version"));
        versionCombo.setMaxWidth(320);
        loadVersions(repository);

        Label subtitle = new Label(i18n("launch.select_version"));
        subtitle.getStyleClass().add("play-page-subtitle");

        page.getChildren().addAll(title, subtitle, versionCombo);
        return page;
    }

    private VBox buildAccountPage() {
        VBox page = new VBox(16);
        page.getStyleClass().add("play-page");
        page.setAlignment(Pos.CENTER);
        page.setPadding(new Insets(48, 32, 16, 32));

        Text title = new Text(i18n("account"));
        title.getStyleClass().add("play-page-title");

        Account selected = Accounts.getSelectedAccount();
        String name = selected != null ? selected.getCharacter() : i18n("account.not_logged_in");
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("play-account-name");

        Button manageBtn = new Button(i18n("account.login"));
        manageBtn.getStyleClass().addAll("play-account-btn");
        manageBtn.setOnAction(e -> Controllers.navigate(Controllers.getAccountListPage()));

        page.getChildren().addAll(title, nameLabel, manageBtn);
        return page;
    }

    private VBox buildDownloadPage() {
        VBox page = new VBox(16);
        page.getStyleClass().add("play-page");
        page.setAlignment(Pos.CENTER);
        page.setPadding(new Insets(48, 32, 16, 32));

        Text title = new Text(i18n("download"));
        title.getStyleClass().add("play-page-title");

        Button goBtn = new Button(i18n("download"));
        goBtn.getStyleClass().addAll("play-account-btn");
        goBtn.setOnAction(e -> Controllers.navigate(Controllers.getDownloadPage()));

        page.getChildren().addAll(title, goBtn);
        return page;
    }

    private VBox buildSettingsPage() {
        VBox page = new VBox(16);
        page.getStyleClass().add("play-page");
        page.setAlignment(Pos.CENTER);
        page.setPadding(new Insets(48, 32, 16, 32));

        Text title = new Text(i18n("settings"));
        title.getStyleClass().add("play-page-title");

        Button goBtn = new Button(i18n("settings"));
        goBtn.getStyleClass().addAll("play-account-btn");
        goBtn.setOnAction(e -> Controllers.navigate(Controllers.getSettingsPage()));

        page.getChildren().addAll(title, goBtn);
        return page;
    }

    // ========================================================================
    //  Bottom bar
    // ========================================================================

    private VBox buildBottomBar() {
        // --- Player row ---
        HBox playerRow = new HBox(8);
        playerRow.getStyleClass().add("play-player-row");
        playerRow.setAlignment(Pos.CENTER_LEFT);
        playerRow.setPadding(new Insets(8, 16, 4, 16));

        // Avatar circle
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("play-avatar");
        Account selected = Accounts.getSelectedAccount();
        String name = selected != null ? selected.getCharacter() : "?";
        Label avatarLetter = new Label(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());
        avatarLetter.getStyleClass().add("play-avatar-letter");
        avatar.getChildren().add(avatarLetter);
        avatar.setClip(new Circle(16, 16, 16));

        // Player name
        Label playerName = new Label(name);
        playerName.getStyleClass().add("play-player-name");

        HBox playerInfo = new HBox(8, avatar, playerName);
        playerInfo.setAlignment(Pos.CENTER_LEFT);
        playerInfo.setOnMouseClicked(e -> switchTab(TAB_ACCOUNT));

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Launch button
        Button launchBtn = new Button();
        launchBtn.getStyleClass().add("play-launch-btn");
        SVG graphic = SVG.GAMEPAD;
        launchBtn.setGraphic(FXUtils.svgShape(24, 24, graphic));
        launchBtn.setOnAction(e -> launchGame());

        playerRow.getChildren().addAll(playerInfo, spacer, launchBtn);

        // --- Tab bar ---
        HBox tabBar = new HBox();
        tabBar.getStyleClass().add("play-tab-bar");

        Button homeTab = createTabButton(i18n("play.tab.home"), SVG.HOME, TAB_HOME);
        Button accountTab = createTabButton(i18n("play.tab.account"), SVG.PERSON, TAB_ACCOUNT);
        Button downloadTab = createTabButton(i18n("play.tab.download"), SVG.DOWNLOAD, TAB_DOWNLOAD);
        Button settingsTab = createTabButton(i18n("play.tab.settings"), SVG.SETTINGS, TAB_SETTINGS);

        tabBar.getChildren().addAll(homeTab, accountTab, downloadTab, settingsTab);
        // Default selection
        homeTab.getStyleClass().add("play-tab-selected");

        VBox bottom = new VBox(playerRow, tabBar);
        bottom.getStyleClass().add("play-bottom-bar");
        return bottom;
    }

    private Button createTabButton(String text, SVG icon, int tabIndex) {
        Button btn = new Button(text);
        btn.getStyleClass().add("play-tab-btn");
        btn.setGraphic(FXUtils.svgShape(20, 20, icon));
        btn.setContentDisplay(ContentDisplay.TOP);
        btn.setOnAction(e -> switchTab(tabIndex));
        return btn;
    }

    private void switchTab(int index) {
        // Update content
        contentArea.getChildren().clear();
        switch (index) {
            case TAB_HOME -> contentArea.getChildren().add(homePage);
            case TAB_ACCOUNT -> contentArea.getChildren().add(accountPage);
            case TAB_DOWNLOAD -> contentArea.getChildren().add(downloadPage);
            case TAB_SETTINGS -> contentArea.getChildren().add(settingsPage);
            default -> contentArea.getChildren().add(homePage);
        }
        // Update tab selection via CSS class toggling
        VBox bottom = (VBox) ((BorderPane) getChildren().get(0)).getBottom();
        HBox tabBar = (HBox) bottom.getChildren().get(1);
        for (int i = 0; i < tabBar.getChildren().size(); i++) {
            tabBar.getChildren().get(i).getStyleClass().remove("play-tab-selected");
        }
        if (index >= 0 && index < tabBar.getChildren().size()) {
            tabBar.getChildren().get(index).getStyleClass().add("play-tab-selected");
        }
    }

    // ========================================================================
    //  Version loading & launch
    // ========================================================================

    private void loadVersions(JMCLGameRepository repository) {
        Profile profile = Profiles.getSelectedProfile();
        this.profile = profile;
        if (profile != null && profile.getRepository().isLoaded()) {
            List<Version> children = repository.getVersions().parallelStream()
                    .filter(version -> !version.isHidden())
                    .sorted(Comparator
                            .comparing((Version version) ->
                                    Lang.requireNonNullElse(version.getReleaseTime(), Instant.EPOCH))
                            .thenComparing(version -> version.getId()))
                    .collect(Collectors.toList());

            versionCombo.getItems().clear();
            for (Version v : children) {
                versionCombo.getItems().add(v.getId());
            }
            if (!versionCombo.getItems().isEmpty()) {
                String selected = Profiles.getSelectedVersion();
                if (selected != null && versionCombo.getItems().contains(selected)) {
                    versionCombo.getSelectionModel().select(selected);
                } else {
                    versionCombo.getSelectionModel().selectFirst();
                }
            }
        }
    }

    private void launchGame() {
        String selected = versionCombo.getSelectionModel().getSelectedItem();
        String accountName = Accounts.getSelectedAccount() != null
                ? Accounts.getSelectedAccount().getCharacter()
                : null;
        if (selected == null || accountName == null) return;

        Profile profile = Profiles.getSelectedProfile();
        if (profile == null) return;

        Versions.launch(profile, selected, false);
    }

    /** Returns the currently selected version ID, or {@code null}. */
    @Nullable
    public String getSelectedVersion() {
        return versionCombo.getSelectionModel().getSelectedItem();
    }
}