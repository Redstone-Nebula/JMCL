/*
 * JMCL
 * Copyright (C) 2026  OCS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.Open_code_Studio.jmcl.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.Open_code_Studio.jmcl.setting.ConfigHolder;
import org.Open_code_Studio.jmcl.setting.StyleSheets;
import org.Open_code_Studio.jmcl.util.i18n.I18n;
import org.Open_code_Studio.jmcl.util.i18n.SupportedLocale;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

import static org.Open_code_Studio.jmcl.util.i18n.I18n.i18n;

/// First-launch wizard shown once on initial startup.
/// Page 1: welcome title + language selector + cancel/continue.
/// Page 2: launcher-type selection (4 option cards).
///
/// The wizard is rendered as a modal undecorated stage centred on its owner.
public final class FirstLaunchWizard {

    private static final int WIZARD_WIDTH = 580;
    private static final int WIZARD_HEIGHT = 500;

    private final Stage owner;
    private final Stage stage;

    private final StackPane root;
    private VBox page1Root;
    private VBox page2Root;

    private SupportedLocale selectedLocale;

    /// @param owner the main application stage (blocked while the wizard is open).
    public FirstLaunchWizard(Stage owner) {
        this.owner = owner;
        this.stage = new Stage();
        this.root = new StackPane();
        this.root.getStyleClass().add("first-launch-wizard");

        initStage();
        buildPage1();
    }

    // ---- public API ---------------------------------------------------------

    /// Shows the wizard modally. Returns immediately (non-blocking).
    /// When the wizard completes it calls [#onComplete()] internally.
    public void show() {
        if (stage.isShowing()) return;
        stage.show();
    }

    // ---- stage initialisation -----------------------------------------------

    private void initStage() {
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setResizable(false);

        Scene scene = new Scene(root, WIZARD_WIDTH, WIZARD_HEIGHT);
        scene.setFill(null);
        StyleSheets.init(scene);
        stage.setScene(scene);

        // centre on owner
        stage.setOnShown(e -> {
            if (owner.isShowing()) {
                stage.setX(owner.getX() + (owner.getWidth() - WIZARD_WIDTH) / 2);
                stage.setY(owner.getY() + (owner.getHeight() - WIZARD_HEIGHT) / 2);
            }
        });

        // ESC closes the wizard (same as Cancel)
        root.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) cancel();
        });
    }

    // =========================================================================
    // Page 1  –  Welcome + language selection
    // =========================================================================

    private void buildPage1() {
        page1Root = new VBox(24);
        page1Root.getStyleClass().add("wizard-page");
        page1Root.setAlignment(Pos.TOP_CENTER);
        page1Root.setPadding(new Insets(56, 48, 32, 48));

        // Large title
        Label title = new Label(i18n("firstlaunch.welcome.title"));
        title.getStyleClass().addAll("wizard-title", "md3-headline-small");

        // Subtitle
        Label subtitle = new Label(i18n("firstlaunch.welcome.subtitle"));
        subtitle.getStyleClass().addAll("wizard-subtitle", "md3-body-large");

        // Language selector
        VBox langBox = new VBox(8);
        langBox.setAlignment(Pos.CENTER);
        langBox.setPadding(new Insets(16, 0, 0, 0));

        ComboBox<SupportedLocale> langCombo = createLanguageCombo();
        langCombo.setMaxWidth(320);
        langBox.getChildren().add(langCombo);

        // Buttons
        HBox buttonBar = new HBox(12);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(32, 0, 0, 0));

        Button cancelBtn = new Button(i18n("firstlaunch.welcome.cancel"));
        cancelBtn.getStyleClass().addAll("md3-text-button", "wizard-cancel");

        Button continueBtn = new Button(i18n("firstlaunch.welcome.continue"));
        continueBtn.getStyleClass().addAll("md3-tonal-button", "wizard-continue");

        cancelBtn.setOnAction(e -> cancel());
        continueBtn.setOnAction(e -> {
            selectedLocale = langCombo.getValue();
            if (selectedLocale != null && selectedLocale != I18n.getLocale()) {
                ConfigHolder.config().setLocalization(selectedLocale);
                Locale.setDefault(selectedLocale.getLocale());
                I18n.setLocale(selectedLocale);
            }
            buildPage2();
            switchToPage(page2Root);
        });

        buttonBar.getChildren().addAll(cancelBtn, continueBtn);

        // VFill spacer before buttons
        VBox spacer = new VBox();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        page1Root.getChildren().addAll(title, subtitle, langBox, spacer, buttonBar);

        root.getChildren().setAll(page1Root);
    }

    private ComboBox<SupportedLocale> createLanguageCombo() {
        List<SupportedLocale> locales = SupportedLocale.getSupportedLocales();
        ComboBox<SupportedLocale> combo = new ComboBox<>();
        combo.getItems().setAll(locales);

        // Select current locale (or first match)
        SupportedLocale current = I18n.getLocale();
        combo.setValue(current);

        // Custom cell rendering: show display name in the locale itself
        combo.setButtonCell(new LocaleCell());
        combo.setCellFactory(v -> new LocaleCell());

        return combo;
    }

    private static final class LocaleCell extends ListCell<SupportedLocale> {
        @Override
        protected void updateItem(SupportedLocale locale, boolean empty) {
            super.updateItem(locale, empty);
            if (empty || locale == null) {
                setText(null);
            } else {
                setText(locale.getDisplayName(locale));
            }
        }
    }

    // =========================================================================
    // Page 2  –  Launcher-type selection
    // =========================================================================

    private void buildPage2() {
        page2Root = new VBox(24);
        page2Root.getStyleClass().add("wizard-page");
        page2Root.setAlignment(Pos.TOP_CENTER);
        page2Root.setPadding(new Insets(40, 40, 32, 40));

        // Title
        Label title = new Label(i18n("firstlaunch.type.title"));
        title.getStyleClass().addAll("wizard-title", "md3-headline-small");

        // 2x2 grid of option cards
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(16, 0, 0, 0));

        // Row 0
        grid.add(createTypeCard("firstlaunch.type.full", "firstlaunch.type.full.desc", "FULL"), 0, 0);
        grid.add(createTypeCard("firstlaunch.type.classic", "firstlaunch.type.classic.desc", "CLASSIC"), 1, 0);
        // Row 1
        grid.add(createTypeCard("firstlaunch.type.creator", "firstlaunch.type.creator.desc", "CREATOR"), 0, 1);
        grid.add(createTypeCard("firstlaunch.type.player", "firstlaunch.type.player.desc", "PLAYER"), 1, 1);

        // Back button
        HBox buttonBar = new HBox(12);
        buttonBar.setAlignment(Pos.CENTER_LEFT);
        buttonBar.setPadding(new Insets(8, 0, 0, 0));
        Button backBtn = new Button(i18n("wizard.prev"));
        backBtn.getStyleClass().addAll("md3-text-button", "wizard-back");
        backBtn.setOnAction(e -> switchToPage(page1Root));

        VBox spacer = new VBox();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        page2Root.getChildren().addAll(title, grid, spacer, buttonBar);
    }

    /// Creates a single type-selection card (VBox with title + description).
    private VBox createTypeCard(String titleKey, String descKey, String typeValue) {
        VBox card = new VBox(8);
        card.getStyleClass().add("wizard-type-card");
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20, 16, 20, 16));
        card.setPrefSize(220, 130);
        card.setMaxSize(220, 130);

        Label cardTitle = new Label(i18n(titleKey));
        cardTitle.getStyleClass().addAll("wizard-type-card-title", "md3-title-medium");

        Label cardDesc = new Label(i18n(descKey));
        cardDesc.getStyleClass().addAll("wizard-type-card-desc", "md3-body-small");
        cardDesc.setWrapText(true);
        cardDesc.setAlignment(Pos.CENTER);
        cardDesc.setMaxWidth(200);

        card.getChildren().addAll(cardTitle, cardDesc);

        card.setOnMouseClicked(e -> onTypeSelected(typeValue));
        return card;
    }

    // =========================================================================
    // Actions
    // =========================================================================

    private void switchToPage(VBox page) {
        root.getChildren().setAll(page);
    }

    private void cancel() {
        Platform.exit();
    }

    private void onTypeSelected(String type) {
        ConfigHolder.config().setLauncherType(type);
        ConfigHolder.config().setFirstLaunchWizardShown(true);

        // "Classic" is the only fully supported mode — close wizard and proceed.
        // Other modes are saved for future use but behave like Classic for now.
        stage.close();
    }

    /// Hide the wizard and return control to the caller.
    @Nullable
    public SupportedLocale getSelectedLocale() {
        return selectedLocale;
    }
}