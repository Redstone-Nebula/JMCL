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
package org.Open_code_Studio.jmcl.upgrade;

import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.*;
import javafx.beans.value.ObservableBooleanValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import org.Open_code_Studio.jmcl.Metadata;
import org.Open_code_Studio.jmcl.ui.Controllers;
import org.Open_code_Studio.jmcl.ui.FXUtils;
import org.Open_code_Studio.jmcl.ui.SVG;
import org.Open_code_Studio.jmcl.ui.SVGContainer;
import org.Open_code_Studio.jmcl.ui.construct.DialogCloseEvent;
import org.Open_code_Studio.jmcl.util.versioning.VersionNumber;

import java.io.IOException;

import static org.Open_code_Studio.jmcl.setting.ConfigHolder.config;
import static org.Open_code_Studio.jmcl.util.Lang.*;
import static org.Open_code_Studio.jmcl.util.logging.Logger.LOG;

public final class UpdateChecker {
    private UpdateChecker() {
    }

    private static final ObjectProperty<RemoteVersion> latestVersion = new SimpleObjectProperty<>();
    private static final BooleanBinding outdated = Bindings.createBooleanBinding(
            () -> {
                RemoteVersion latest = latestVersion.get();
                if (latest == null) {
                    return false;
                }
                if (isDevelopmentVersion(Metadata.VERSION) && !config().isAcceptPreviewUpdate()) {
                    return false;
                }
                if (latest.force()
                        || Metadata.isNightly()
                        || latest.channel() == UpdateChannel.NIGHTLY
                        || latest.channel() != UpdateChannel.getChannel()) {
                    // Cross-channel comparison: strip "DEV" prefix for fair version comparison
                    // This prevents showing a DOWNGRADE (e.g. DEV2026.2.1 → stable 2026.2.0) as an update
                    String currentVer = Metadata.VERSION.startsWith("DEV") ? Metadata.VERSION.substring(3) : Metadata.VERSION;
                    String latestVer = latest.version().startsWith("DEV") ? latest.version().substring(3) : latest.version();
                    return VersionNumber.compare(currentVer, latestVer) < 0;
                } else {
                    return VersionNumber.compare(Metadata.VERSION, latest.version()) < 0;
                }
            },
            latestVersion, config().acceptPreviewUpdateProperty());
    private static final ReadOnlyBooleanWrapper checkingUpdate = new ReadOnlyBooleanWrapper(false);

    public static void init() {
        requestCheckUpdate(UpdateChannel.getChannel(), config().isAcceptPreviewUpdate());
    }

    public static RemoteVersion getLatestVersion() {
        return latestVersion.get();
    }

    public static ReadOnlyObjectProperty<RemoteVersion> latestVersionProperty() {
        return latestVersion;
    }

    public static boolean isOutdated() {
        return outdated.get();
    }

    public static ObservableBooleanValue outdatedProperty() {
        return outdated;
    }

    public static boolean isCheckingUpdate() {
        return checkingUpdate.get();
    }

    public static ReadOnlyBooleanProperty checkingUpdateProperty() {
        return checkingUpdate.getReadOnlyProperty();
    }

    private static RemoteVersion checkUpdate(UpdateChannel channel, boolean preview) throws IOException {
        return RemoteVersion.fetchFromGitHub(channel, preview);
    }

    private static boolean isDevelopmentVersion(String version) {
        return version.startsWith("DEV"); // eg. DEV2026.1.0
    }

    public static void requestCheckUpdate(UpdateChannel channel, boolean preview) {
        Platform.runLater(() -> {
            if (isCheckingUpdate())
                return;
            checkingUpdate.set(true);

            thread(() -> {
                RemoteVersion result = null;
                try {
                    result = checkUpdate(channel, preview);
                    LOG.info("Latest version (" + channel + ", preview=" + preview + ") is " + result);

                    // If we're on a DEV version that is newer than anything on GitHub,
                    // show the "not released" dialog to inform the user
                    if (result != null && preview && isDevelopmentVersion(Metadata.VERSION)) {
                        String currentVer = Metadata.VERSION.startsWith("DEV") ? Metadata.VERSION.substring(3) : Metadata.VERSION;
                        String latestVer = result.version().startsWith("DEV") ? result.version().substring(3) : result.version();
                        if (VersionNumber.compare(currentVer, latestVer) > 0) {
                            LOG.info("Current version " + Metadata.VERSION + " is newer than GitHub release " + result.version());
                            showNotReleasedDialog();
                        }
                    }
                } catch (Throwable e) {
                    LOG.warning("Failed to check for update", e);
                    String errorMsg = e.getMessage();
                    if (errorMsg != null && (errorMsg.contains("rate limit") || errorMsg.contains("Repository not found"))) {
                        LOG.info("Update check skipped: " + errorMsg);
                    } else if (preview && isDevelopmentVersion(Metadata.VERSION)) {
                        showNotReleasedDialog();
                    }
                }

                RemoteVersion finalResult = result;
                Platform.runLater(() -> {
                    checkingUpdate.set(false);
                    if (finalResult != null) {
                        latestVersion.set(finalResult);
                    }
                });
            }, "Update Checker", true);
        });
    }

    private static void showNotReleasedDialog() {
        Platform.runLater(() -> {
            SVGContainer rocketIcon = SVG.ROCKET_LAUNCH.createIcon(48);
            rocketIcon.setStyle("-fx-fill: -monet-primary;");

            Label titleLabel = new Label("E...不对，你是不是改了这个代码了？");
            titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -monet-primary;");

            Label subtitleLabel = new Label("怎么查不到这个版本呢...");
            subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: -monet-on-surface-variant;");

            Label hintLabel = new Label("当前版本 " + Metadata.VERSION + " 好像没有在 GitHub 上找到呢。\n去瞧瞧正式版有什么新东西吧！");
            hintLabel.setWrapText(true);
            hintLabel.setTextAlignment(TextAlignment.CENTER);
            hintLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -monet-on-surface;");

            VBox content = new VBox(12);
            content.setAlignment(Pos.CENTER);
            content.setPadding(new Insets(24));
            content.getStyleClass().add("jfx-dialog-layout");

            MFXButton goButton = new MFXButton("去 GitHub 看看");
            goButton.getStyleClass().addAll("dialog-accept", "md3-contained-button");
            goButton.setOnAction(e -> {
                FXUtils.openLink(Metadata.DOWNLOAD_URL);
            });

            MFXButton closeButton = new MFXButton("知道了");
            closeButton.getStyleClass().addAll("md3-text-button");
            closeButton.setOnAction(e -> {
                content.fireEvent(new DialogCloseEvent());
            });

            rocketIcon.getStyleClass().add("hbox");
            titleLabel.getStyleClass().add("hbox");
            subtitleLabel.getStyleClass().add("hbox");
            hintLabel.getStyleClass().add("hbox");

            HBox buttonBar = new HBox(8, goButton, closeButton);
            buttonBar.setAlignment(Pos.CENTER);
            buttonBar.getStyleClass().add("hbox");

            content.getChildren().setAll(rocketIcon, titleLabel, subtitleLabel, hintLabel, buttonBar);

            Controllers.dialog(content);
        });
    }
}