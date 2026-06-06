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
package org.Open_code_Studio.jmcl.ui.main;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.concurrent.Worker;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.Open_code_Studio.jmcl.auth.Account;
import org.Open_code_Studio.jmcl.auth.yggdrasil.TextureModel;
import org.Open_code_Studio.jmcl.game.TexturesLoader;
import org.Open_code_Studio.jmcl.setting.Accounts;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.Base64;

import static org.Open_code_Studio.jmcl.util.logging.Logger.LOG;

public class SkinViewPane extends StackPane {
    private final WebView webView;
    private final WebEngine engine;
    private String pendingDataUrl;
    private boolean pendingSlim;

    public SkinViewPane() {
        getStyleClass().add("skin-view-pane");

        webView = new WebView();
        webView.getStyleClass().add("skin-view-webview");

        engine = webView.getEngine();

        URL url = getClass().getResource("/assets/skinview3d.html");
        if (url != null) {
            engine.load(url.toExternalForm());
        }

        getChildren().add(webView);

        // Bind WebView size to StackPane size
        webView.prefWidthProperty().bind(widthProperty());
        webView.prefHeightProperty().bind(heightProperty());

        // Initialize skinview3d when the page is loaded
        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                engine.executeScript(String.format(
                        "initViewer(%d, %d)", (int) getWidth(), (int) getHeight()
                ));
                // Flush any pending skin update
                if (pendingDataUrl != null) {
                    pushSkin(pendingDataUrl, pendingSlim);
                    pendingDataUrl = null;
                }
            }
        });

        // React to account selection changes
        InvalidationListener updateSkinListener = obs -> {
            Account selectedAccount = Accounts.selectedAccountProperty().get();
            if (selectedAccount != null) {
                updateSkinForAccount(selectedAccount);
            } else {
                updateSkinFromImage(TexturesLoader.getDefaultSkinImage(), false);
            }
        };

        Accounts.selectedAccountProperty().addListener(updateSkinListener);
        updateSkinListener.invalidated(null);
    }

    private void updateSkinForAccount(Account account) {
        TexturesLoader.skinBinding(account).addListener((obs, old, val) -> {
            if (val != null) {
                Image skin = val.getImage();
                boolean isSlim = TextureModel.SLIM.modelName.equals(val.getMetadata().get("model"));
                updateSkinFromImage(skin, isSlim);
            }
        });
    }

    private void updateSkinFromImage(Image fxImage, boolean isSlim) {
        String dataUrl = imageToDataUrl(fxImage);
        if (dataUrl == null) return;

        Platform.runLater(() -> {
            if (engine.getLoadWorker().getState() == Worker.State.SUCCEEDED) {
                pushSkin(dataUrl, isSlim);
            } else {
                // Page not loaded yet — queue for later
                pendingDataUrl = dataUrl;
                pendingSlim = isSlim;
            }
        });
    }

    private void pushSkin(String dataUrl, boolean isSlim) {
        // Escape single quotes in base64 data URL (safe for this format)
        engine.executeScript("updateSkin('" + dataUrl + "', " + isSlim + ")");
    }

    private static String imageToDataUrl(Image fxImage) {
        try {
            int w = (int) fxImage.getWidth();
            int h = (int) fxImage.getHeight();
            BufferedImage bufferedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            PixelReader reader = fxImage.getPixelReader();
            if (reader != null) {
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        bufferedImage.setRGB(x, y, reader.getArgb(x, y));
                    }
                }
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", baos);
            byte[] bytes = baos.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            LOG.warning("Failed to convert skin image to data URL", e);
            return null;
        }
    }
}