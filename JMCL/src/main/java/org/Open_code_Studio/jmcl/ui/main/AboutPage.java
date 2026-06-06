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

import com.google.gson.*;
import com.jfoenix.controls.JFXButton;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.Open_code_Studio.jmcl.Metadata;
import org.Open_code_Studio.jmcl.theme.Themes;
import org.Open_code_Studio.jmcl.ui.Controllers;
import org.Open_code_Studio.jmcl.ui.FXUtils;
import org.Open_code_Studio.jmcl.ui.SVG;
import org.Open_code_Studio.jmcl.ui.WeakListenerHolder;
import org.Open_code_Studio.jmcl.ui.construct.ComponentList;
import org.Open_code_Studio.jmcl.ui.construct.LineButton;
import org.Open_code_Studio.jmcl.ui.construct.DialogCloseEvent;
import org.Open_code_Studio.jmcl.ui.construct.SpinnerPane;
import org.Open_code_Studio.jmcl.util.gson.JsonUtils;

import javax.imageio.ImageIO;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import static org.Open_code_Studio.jmcl.util.i18n.I18n.i18n;
import static org.Open_code_Studio.jmcl.util.logging.Logger.LOG;

public final class AboutPage extends SpinnerPane {

    private final WeakListenerHolder holder = new WeakListenerHolder();
    private int developerClickCount = 0;

    public AboutPage() {
        VBox content = new VBox();
        content.getStyleClass().add("spinner-pane-content");
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        FXUtils.smoothScrolling(scrollPane);
        setContent(scrollPane);

        ComponentList about = new ComponentList();
        {
            var launcher = LineButton.createExternalLinkButton(Metadata.PUBLISH_URL);
            launcher.setLargeTitle(true);
            launcher.setLeading(FXUtils.newBuiltinImage("/assets/img/jvm-mcl.png"), 36);
            launcher.setTitle("JVM Minecraft! Launcher");
            launcher.setSubtitle(Metadata.VERSION);
            launcher.setOnAction(e -> {
                developerClickCount++;
                if (developerClickCount >= 6) {
                    developerClickCount = 0;
                    Controllers.getSettingsPage().unlockDeveloperOptions();
                    Controllers.navigate(Controllers.getSettingsPage());
                }
            });

            var author = LineButton.createExternalLinkButton("https://github.com/Open-code-Studio");
            author.setLargeTitle(true);
            author.setLeading(FXUtils.newBuiltinImage("/assets/img/cangjie.jpeg"), 48);
            author.setTitle("Open Code Studio");
            author.setSubtitle(i18n("about.author.statement"));

            about.getContent().setAll(launcher, author);
        }

        ComponentList thanks = loadIconedTwoLineList("/assets/about/thanks.json");

        ComponentList deps = loadIconedTwoLineList("/assets/about/deps.json");

        ComponentList legal = new ComponentList();
        {
            var copyright = new LineButton();
            copyright.setLargeTitle(true);
            copyright.setTitle(i18n("about.copyright"));
            copyright.setSubtitle(i18n("about.copyright.statement"));

            var claim = new LineButton();
            claim.setLargeTitle(true);
            claim.setTitle(i18n("about.claim"));
            claim.setSubtitle(i18n("about.claim.statement"));
            claim.setTrailingIcon(SVG.OPEN_IN_NEW);
            claim.setOnAction(e -> showLicenseDialog());

            var newRepo = LineButton.createExternalLinkButton(Metadata.NEW_REPO_URL);
            newRepo.setLargeTitle(true);
            newRepo.setTitle(i18n("about.open_source"));
            newRepo.setSubtitle(i18n("about.open_source.statement"));

            legal.getContent().setAll(copyright, claim, newRepo);
        }

        content.getChildren().setAll(
                ComponentList.createComponentListTitle(i18n("about")),
                about,
                ComponentList.createComponentListTitle(i18n("about.thanks_to")),
                thanks,
                ComponentList.createComponentListTitle(i18n("about.dependency")),
                deps,
                ComponentList.createComponentListTitle(i18n("about.legal")),
                legal
        );
    }

    private void showLicenseDialog() {
        try (InputStream input = FXUtils.class.getResourceAsStream("/assets/about/eula.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8))) {
            String text = reader.lines().collect(Collectors.joining("\n"));

            TextArea textArea = new TextArea(text);
            textArea.setEditable(false);
            textArea.setWrapText(false);
            textArea.setPrefWidth(600);
            textArea.setPrefHeight(500);
            textArea.getStyleClass().add("license-text-area");

            ScrollPane scrollPane = new ScrollPane(textArea);
            scrollPane.setFitToWidth(true);
            scrollPane.setPrefSize(620, 520);

            JFXButton closeButton = FXUtils.newToggleButton4(SVG.CLOSE);
            closeButton.getStyleClass().add("license-close-button");

            HBox header = new HBox(closeButton);
            header.getStyleClass().add("license-header");
            header.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

            VBox dialogContent = new VBox(header, scrollPane);
            VBox.setVgrow(scrollPane, Priority.ALWAYS);

            closeButton.setOnAction(e -> dialogContent.fireEvent(new DialogCloseEvent()));

            Controllers.dialog(dialogContent);
        } catch (IOException e) {
            LOG.warning("Failed to load license text", e);
        }
    }

    private static Image loadImage(String url) {
        return url.startsWith("/")
                ? FXUtils.newBuiltinImage(url)
                : new Image(url);
    }

    private ComponentList loadIconedTwoLineList(String path) {
        ComponentList componentList = new ComponentList();

        InputStream input = FXUtils.class.getResourceAsStream(path);
        if (input == null) {
            LOG.warning("Resources not found: " + path);
            return componentList;
        }

        try {
            JsonArray array = JsonUtils.fromJsonFully(input, JsonArray.class);

            for (JsonElement element : array) {
                JsonObject obj = element.getAsJsonObject();

                var button = new LineButton();
                button.setLargeTitle(true);

                if (obj.get("externalLink") instanceof JsonPrimitive externalLink) {
                    button.setTrailingIcon(SVG.OPEN_IN_NEW);

                    String link = externalLink.getAsString();
                    button.setOnAction(event -> FXUtils.openLink(link));
                }

                if (obj.has("image")) {
                    JsonElement image = obj.get("image");
                    if (image.isJsonPrimitive()) {
                        button.setLeading(loadImage(image.getAsString()));
                    } else if (image.isJsonObject()) {
                        holder.add(FXUtils.onWeakChangeAndOperate(Themes.darkModeProperty(), darkMode -> {
                            button.setLeading(darkMode
                                    ? loadImage(image.getAsJsonObject().get("dark").getAsString())
                                    : loadImage(image.getAsJsonObject().get("light").getAsString())
                            );
                        }));
                    }
                }

                if (obj.get("title") instanceof JsonPrimitive title)
                    button.setTitle(title.getAsString());
                else if (obj.get("titleLocalized") instanceof JsonPrimitive titleLocalized)
                    button.setTitle(i18n(titleLocalized.getAsString()));

                if (obj.get("subtitle") instanceof JsonPrimitive subtitle)
                    button.setSubtitle(subtitle.getAsString());
                else if (obj.get("subtitleLocalized") instanceof JsonPrimitive subtitleLocalized)
                    button.setSubtitle(i18n(subtitleLocalized.getAsString()));

                componentList.getContent().add(button);
            }
        } catch (IOException | JsonParseException e) {
            LOG.warning("Failed to load list: " + path, e);
        }

        return componentList;
    }
}
