/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2025  huangyuhui <huanghongxun2008@126.com> and contributors
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
import javafx.beans.WeakInvalidationListener;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.Open_code_Studio.jmcl.game.JMCLGameRepository;
import org.Open_code_Studio.jmcl.setting.Profile;
import org.Open_code_Studio.jmcl.setting.StyleSheets;
import org.Open_code_Studio.jmcl.ui.construct.MessageDialogPane;
import org.Open_code_Studio.jmcl.util.TaskCancellationAction;
import org.Open_code_Studio.jmcl.util.platform.ManagedProcess;
import org.Open_code_Studio.jmcl.util.platform.OperatingSystem;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static org.Open_code_Studio.jmcl.util.i18n.I18n.i18n;

/// A small undecorated window that manages a running Minecraft game instance.
///
/// It appears next to the main launcher window when a game is launched,
/// providing quick access to stop the game, open its folder, and view mods.
public final class InstanceManagerWindow extends Stage {

    /// The managed game process.
    private final ManagedProcess process;

    /// The profile associated with this instance.
    private final Profile profile;

    /// The version ID of this instance.
    private final String versionId;

    /// The game repository for accessing game directories.
    private final JMCLGameRepository repository;

    /// Thread monitoring the game process for exit detection.
    private @Nullable Thread monitorThread;

    /// Creates an instance manager window for a running game.
    ///
    /// @param process    the managed game process.
    /// @param profile    the launcher profile.
    /// @param versionId  the Minecraft version ID.
    /// @param repository the game repository.
    public InstanceManagerWindow(ManagedProcess process, Profile profile, String versionId, JMCLGameRepository repository) {
        this.process = Objects.requireNonNull(process);
        this.profile = Objects.requireNonNull(profile);
        this.versionId = Objects.requireNonNull(versionId);
        this.repository = Objects.requireNonNull(repository);

        initStyle(StageStyle.UNDECORATED);
        initStyle(StageStyle.TRANSPARENT);
        setResizable(false);
        setAlwaysOnTop(true);

        VBox root = new VBox(4);
        root.getStyleClass().add("instance-manager");
        root.setPadding(new Insets(6));
        root.setAlignment(Pos.TOP_CENTER);

        IconButton btnClose = new IconButton(SVG.CLOSE, this::close);
        FXUtils.installFastTooltip(btnClose, i18n("instance.manager.close"));

        IconButton btnExit = new IconButton(SVG.CANCEL, () -> {
            process.stop();
            close();
        });
        FXUtils.installFastTooltip(btnExit, i18n("instance.manager.exit"));

        IconButton btnFolder = new IconButton(SVG.FOLDER_OPEN, () ->
                FXUtils.openFolder(repository.getRunDirectory(versionId)));
        FXUtils.installFastTooltip(btnFolder, i18n("instance.manager.folder"));

        IconButton btnMods = new IconButton(SVG.EXTENSION, this::openModPage);
        FXUtils.installFastTooltip(btnMods, i18n("instance.manager.mods"));

        root.getChildren().setAll(btnClose, btnExit, btnFolder, btnMods);

        Scene scene = new Scene(root);
        scene.setFill(null);
        StyleSheets.init(scene);
        setScene(scene);

        positionRelativeToMainStage();
        startProcessMonitor();
    }

    /// Positions this window to the right of the main launcher stage.
    private void positionRelativeToMainStage() {
        Stage mainStage = Controllers.getStage();
        if (mainStage == null) return;

        double offsetX = mainStage.getX() + mainStage.getWidth() + 4;
        double offsetY = mainStage.getY() + 40;
        setX(offsetX);
        setY(offsetY);

        mainStage.xProperty().addListener(new WeakInvalidationListener(
                obs -> setX(mainStage.getX() + mainStage.getWidth() + 4)));
        mainStage.yProperty().addListener(new WeakInvalidationListener(
                obs -> setY(mainStage.getY() + 40)));
        mainStage.widthProperty().addListener(new WeakInvalidationListener(
                obs -> setX(mainStage.getX() + mainStage.getWidth() + 4)));
    }

    /// Starts a background thread that monitors the game process.
    /// When the process exits, the manager window closes automatically.
    private void startProcessMonitor() {
        monitorThread = new Thread(() -> {
            while (process.isRunning()) {
                try {
                    //noinspection BusyWait
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            Platform.runLater(this::close);
        }, "InstanceManagerMonitor-" + versionId);
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    /// Opens the mod list page for this instance in the launcher.
    private void openModPage() {
        Controllers.getVersionPage().setVersion(versionId, profile);
        Controllers.getVersionPage().showInstanceSettings();
        Controllers.navigate(Controllers.getVersionPage());
    }

    /// A small icon-only button used in the instance manager toolbar.
    private static final class IconButton extends com.jfoenix.controls.JFXButton {
        IconButton(SVG icon, Runnable action) {
            setGraphic(icon.createIcon(16));
            setFocusTraversable(false);
            getStyleClass().add("instance-manager-button");
            setOnAction(e -> action.run());
        }
    }
}