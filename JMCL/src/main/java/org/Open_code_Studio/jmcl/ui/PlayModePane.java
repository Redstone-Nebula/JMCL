/// Play-style minimal UI for JMCL.
///
/// When {@code config().getLauncherType()} is {@code "PLAY"},
/// this pane replaces the normal sidebar-based RootPage
/// with a simplified LauncherX-style interface.
package org.Open_code_Studio.jmcl.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import org.Open_code_Studio.jmcl.auth.Account;
import org.Open_code_Studio.jmcl.game.JMCLGameRepository;
import org.Open_code_Studio.jmcl.game.Version;
import org.Open_code_Studio.jmcl.setting.Accounts;
import org.Open_code_Studio.jmcl.setting.Profile;
import org.Open_code_Studio.jmcl.setting.Profiles;
import org.Open_code_Studio.jmcl.ui.construct.ComponentList;
import org.Open_code_Studio.jmcl.util.Lang;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.Open_code_Studio.jmcl.util.i18n.I18n.i18n;

/**
 * A minimal, LauncherX-style root pane.
 *
 * <p>Layout (top to bottom):
 * <ol>
 *   <li>App name / logo</li>
 *   <li>Account label (clickable)</li>
 *   <li>Version selector</li>
 *   <li>Large Play button</li>
 *   <li>Settings button at the bottom</li>
 * </ol>
 */
public final class PlayModePane extends VBox {

    private final ComboBox<String> versionCombo = new ComboBox<>();

    @Nullable
    private Profile profile;

    /**
     * @param repository the game repository to load versions from
     */
    public PlayModePane(JMCLGameRepository repository) {
        super(24);
        getStyleClass().add("play-mode-pane");
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(56, 32, 32, 32));
        setMaxWidth(480);

        buildUI(repository);
    }

    private void buildUI(JMCLGameRepository repository) {
        // App name
        Text appName = new Text("Play");
        appName.getStyleClass().add("play-mode-title");
        VBox.setMargin(appName, new Insets(0, 0, 8, 0));

        // Account label
        Label accountLabel = new Label(i18n("account") + ": " + Accounts.getCurrentAccountName());
        accountLabel.getStyleClass().add("play-mode-section-title");
        accountLabel.setOnMouseClicked(e -> Controllers.navigate(Controllers.getAccountListPage()));
        accountLabel.setMaxWidth(Double.MAX_VALUE);
        accountLabel.setAlignment(Pos.CENTER);

        // Version combo
        versionCombo.getStyleClass().add("play-mode-combo");
        versionCombo.setPromptText(i18n("version"));
        versionCombo.setMaxWidth(Double.MAX_VALUE);

        // Load versions from the repository
        loadVersions(repository);

        // Large Play button
        Button playButton = new Button(i18n("launch"));
        playButton.getStyleClass().add("play-mode-play-btn");
        playButton.setMaxWidth(Double.MAX_VALUE);
        playButton.setPrefHeight(56);

        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Bottom row: settings button
        Button settingsButton = new Button(i18n("settings"));
        settingsButton.getStyleClass().add("play-mode-settings-btn");
        settingsButton.setMaxWidth(Double.MAX_VALUE);
        settingsButton.setOnAction(e -> Controllers.navigate(Controllers.getSettingsPage()));

        getChildren().addAll(
                appName,
                accountLabel,
                versionCombo,
                playButton,
                spacer,
                settingsButton
        );
    }

    private void loadVersions(JMCLGameRepository repository) {
        // Use the same version-filtering logic as RootPage
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

    /** Returns the currently selected version ID, or {@code null}. */
    @Nullable
    public String getSelectedVersion() {
        return versionCombo.getSelectionModel().getSelectedItem();
    }
}