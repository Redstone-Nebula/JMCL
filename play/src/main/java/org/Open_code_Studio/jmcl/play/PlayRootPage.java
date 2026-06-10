/// Minimal one-screen launcher UI.
package org.Open_code_Studio.jmcl.play;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.Open_code_Studio.jmcl.game.DefaultGameRepository;
import org.Open_code_Studio.jmcl.game.GameRepository;
import org.Open_code_Studio.jmcl.game.Version;

import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * A minimal, LauncherX-style root page.
 *
 * <p>Layout (top to bottom):
 * <ol>
 *   <li>App name label</li>
 *   <li>Version selector combo box</li>
 *   <li>Large Play button</li>
 *   <li>Account / status bar at the bottom</li>
 * </ol>
 */
public final class PlayRootPage {

    private final Stage stage;
    private final GameRepository gameRepository;
    private final VBox root;

    /**
     * @param stage   the primary stage
     * @param workDir the JMCL working directory
     */
    public PlayRootPage(Stage stage, Path workDir) {
        this.stage = stage;
        this.gameRepository = new DefaultGameRepository(workDir.resolve(".minecraft"));

        root = new VBox(24);
        root.getStyleClass().add("play-root");
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(40, 32, 24, 32));

        // Refresh the version list
        gameRepository.refreshVersions();

        buildUI();
    }

    /** Returns the root layout. */
    public VBox getRoot() {
        return root;
    }

    private void buildUI() {
        // App name
        Text appName = new Text("Play");
        appName.getStyleClass().add("play-title");

        // Version selector
        ComboBox<String> versionCombo = new ComboBox<>();
        versionCombo.getStyleClass().add("play-combo-box");
        versionCombo.setPromptText("Select Version");
        versionCombo.setMaxWidth(Double.MAX_VALUE);

        // Load installed versions
        Collection<Version> versions = gameRepository.getVersions();
        versionCombo.getItems().addAll(
                versions.stream().map(Version::getId).collect(Collectors.toList())
        );

        if (!versionCombo.getItems().isEmpty()) {
            versionCombo.getSelectionModel().selectFirst();
        }

        // Account label
        Text accountLabel = new Text("Account: Offline Player");
        accountLabel.getStyleClass().add("play-section-title");
        accountLabel.setOnMouseClicked(e -> {
            // Placeholder for account switching
        });

        // Large Play button
        Button playButton = new Button("Play");
        playButton.getStyleClass().add("play-btn-primary");
        playButton.setMaxWidth(Double.MAX_VALUE);
        playButton.setPrefHeight(56);
        playButton.setOnAction(e -> launchGame(versionCombo.getValue()));

        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Status bar
        Label statusBar = new Label("Ready");
        statusBar.getStyleClass().add("play-status-bar");
        statusBar.setMaxWidth(Double.MAX_VALUE);
        statusBar.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        root.getChildren().addAll(
                appName,
                versionCombo,
                accountLabel,
                playButton,
                spacer,
                statusBar
        );
    }

    private void launchGame(String versionId) {
        if (versionId == null || versionId.isBlank()) {
            return;
        }
        // Placeholder — actual launch logic will use
        // DefaultLauncher from JVM-MCLCore.
        System.out.println("Launching " + versionId + "...");
    }
}