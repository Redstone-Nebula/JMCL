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

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialogLayout;
import com.jfoenix.controls.JFXSpinner;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.Open_code_Studio.jmcl.Metadata;
import org.Open_code_Studio.jmcl.task.Schedulers;
import org.Open_code_Studio.jmcl.task.Task;
import org.Open_code_Studio.jmcl.ui.construct.DialogCloseEvent;
import org.Open_code_Studio.jmcl.ui.construct.JFXHyperlink;
import org.Open_code_Studio.jmcl.upgrade.RemoteVersion;
import org.Open_code_Studio.jmcl.util.gson.JsonUtils;
import org.Open_code_Studio.jmcl.util.io.NetworkUtils;

import java.net.URI;
import java.net.URLConnection;

import static org.Open_code_Studio.jmcl.Metadata.CHANGELOG_URL;
import static org.Open_code_Studio.jmcl.ui.FXUtils.onEscPressed;
import static org.Open_code_Studio.jmcl.util.i18n.I18n.i18n;
import static org.Open_code_Studio.jmcl.util.logging.Logger.LOG;

public final class UpgradeDialog extends JFXDialogLayout {

    private static final String GITHUB_API_RELEASE_BY_TAG = "https://api.github.com/repos/Open-code-Studio/JMCL/releases/tags/";

    public UpgradeDialog(RemoteVersion remoteVersion, Runnable updateRunnable) {
        maxWidthProperty().bind(Controllers.getScene().widthProperty().multiply(0.7));
        maxHeightProperty().bind(Controllers.getScene().heightProperty().multiply(0.7));

        setHeading(new Label(i18n("update.changelog")));
        setBody(new JFXSpinner());

        // Use the original GitHub release tag as-is (e.g. "v2026.1.0" or "DEV2026.2.0")
        String versionTag = remoteVersion.tag();
        String releaseUrl = CHANGELOG_URL + "/tag/" + versionTag;
        String apiUrl = GITHUB_API_RELEASE_BY_TAG + versionTag;

        Task.supplyAsync(Schedulers.io(), () -> {
            try {
                URLConnection connection = NetworkUtils.createConnection(URI.create(apiUrl));
                String token = System.getenv("GITHUB_TOKEN");
                if (token != null && !token.isEmpty()) {
                    connection.setRequestProperty("Authorization", "Bearer " + token);
                }
                String responseText = NetworkUtils.readFullyAsString(connection);
                JsonObject release = JsonUtils.fromNonNullJson(responseText, JsonObject.class);
                if (release == null) return null;

                // Get the release body (GitHub Markdown content)
                String body = release.get("body") != null ? release.get("body").getAsString() : null;
                if (body == null || body.isBlank()) return null;

                return renderReleaseBody(body);
            } catch (JsonParseException | java.io.IOException e) {
                LOG.warning("Failed to fetch release notes from GitHub: " + e.getMessage());
                return null;
            }
        }).whenComplete(Schedulers.javafx(), (result, exception) -> {
            if (exception == null && result != null) {
                ScrollPane scrollPane = new ScrollPane(result);
                scrollPane.setFitToWidth(true);
                FXUtils.smoothScrolling(scrollPane);
                setBody(scrollPane);
            } else {
                // Fallback: show a simple message with link to GitHub
                VBox fallbackBox = new VBox(12);
                Label infoLabel = new Label(i18n("update.changelog_unavailable", remoteVersion.version()));
                infoLabel.setWrapText(true);
                infoLabel.getStyleClass().add("md3-body-medium");
                fallbackBox.getChildren().add(infoLabel);
                setBody(fallbackBox);
            }
        }).start();

        JFXHyperlink openInBrowser = new JFXHyperlink(i18n("web.view_in_browser"));
        openInBrowser.setExternalLink(releaseUrl);

        JFXButton updateButton = new JFXButton(i18n("update.accept"));
        updateButton.getStyleClass().add("dialog-accept");
        updateButton.setOnAction(e -> updateRunnable.run());

        JFXButton cancelButton = new JFXButton(i18n("button.cancel"));
        cancelButton.getStyleClass().add("dialog-cancel");
        cancelButton.setOnAction(e -> fireEvent(new DialogCloseEvent()));

        setActions(openInBrowser, updateButton, cancelButton);
        onEscPressed(this, cancelButton::fire);
    }

    /**
     * Render the GitHub release body (Markdown-like text) into JavaFX nodes.
     * Handles basic formatting: headings, lists, code blocks, links, bold, italic.
     */
    private static VBox renderReleaseBody(String body) {
        VBox container = new VBox(6);
        container.getStyleClass().add("changelog-container");

        String[] lines = body.split("\n");
        boolean inCodeBlock = false;
        StringBuilder codeBlockBuffer = new StringBuilder();

        for (String line : lines) {
            // Handle code blocks
            if (line.trim().startsWith("```")) {
                if (inCodeBlock) {
                    // End of code block
                    Text codeText = new Text(codeBlockBuffer.toString());
                    codeText.getStyleClass().add("changelog-code");
                    TextFlow codeFlow = new TextFlow(codeText);
                    codeFlow.getStyleClass().add("changelog-code-block");
                    container.getChildren().add(codeFlow);
                    codeBlockBuffer = new StringBuilder();
                    inCodeBlock = false;
                } else {
                    inCodeBlock = true;
                }
                continue;
            }

            if (inCodeBlock) {
                codeBlockBuffer.append(line).append("\n");
                continue;
            }

            // Skip empty lines
            if (line.trim().isEmpty()) {
                continue;
            }

            // Headings (## or ###)
            if (line.trim().startsWith("## ")) {
                Label heading = new Label(line.trim().substring(3).trim());
                heading.getStyleClass().add("changelog-heading");
                container.getChildren().add(heading);
                continue;
            }
            if (line.trim().startsWith("### ")) {
                Label subheading = new Label(line.trim().substring(4).trim());
                subheading.getStyleClass().add("changelog-subheading");
                container.getChildren().add(subheading);
                continue;
            }

            // Unordered list items
            if (line.trim().startsWith("- ") || line.trim().startsWith("* ")) {
                String text = line.trim().substring(2).trim();
                // Format [text](url) as hyperlinks
                TextFlow itemFlow = parseInlineMarkdown("• " + text);
                itemFlow.getStyleClass().add("changelog-list-item");
                container.getChildren().add(itemFlow);
                continue;
            }

            // Regular paragraph
            TextFlow paragraph = parseInlineMarkdown(line.trim());
            paragraph.getStyleClass().add("changelog-paragraph");
            container.getChildren().add(paragraph);
        }

        // Flush remaining code block content
        if (inCodeBlock && !codeBlockBuffer.isEmpty()) {
            Text codeText = new Text(codeBlockBuffer.toString());
            codeText.getStyleClass().add("changelog-code");
            TextFlow codeFlow = new TextFlow(codeText);
            codeFlow.getStyleClass().add("changelog-code-block");
            container.getChildren().add(codeFlow);
        }

        return container;
    }

    /**
     * Parse inline Markdown formatting: bold (**), italic (*), links ([text](url)), inline code (`).
     */
    private static TextFlow parseInlineMarkdown(String text) {
        TextFlow flow = new TextFlow();
        StringBuilder current = new StringBuilder();
        int i = 0;
        int len = text.length();

        while (i < len) {
            char c = text.charAt(i);

            // Inline code: `code`
            if (c == '`') {
                flushText(flow, current.toString());
                current = new StringBuilder();
                i++;
                while (i < len && text.charAt(i) != '`') {
                    current.append(text.charAt(i));
                    i++;
                }
                if (!current.isEmpty()) {
                    Text codeText = new Text(current.toString());
                    codeText.getStyleClass().add("changelog-inline-code");
                    flow.getChildren().add(codeText);
                    current = new StringBuilder();
                }
                i++; // skip closing `
                continue;
            }

            // **bold** or *italic*
            if (c == '*' && i + 1 < len) {
                if (text.charAt(i + 1) == '*') {
                    // **bold**
                    flushText(flow, current.toString());
                    current = new StringBuilder();
                    i += 2;
                    while (i < len && !(text.charAt(i) == '*' && i + 1 < len && text.charAt(i + 1) == '*')) {
                        current.append(text.charAt(i));
                        i++;
                    }
                    Text boldText = new Text(current.toString());
                    boldText.setStyle("-fx-font-weight: bold;");
                    flow.getChildren().add(boldText);
                    current = new StringBuilder();
                    i += 2; // skip closing **
                    continue;
                } else {
                    // *italic*
                    flushText(flow, current.toString());
                    current = new StringBuilder();
                    i++;
                    while (i < len && text.charAt(i) != '*') {
                        current.append(text.charAt(i));
                        i++;
                    }
                    Text italicText = new Text(current.toString());
                    italicText.setStyle("-fx-font-style: italic;");
                    flow.getChildren().add(italicText);
                    current = new StringBuilder();
                    i++; // skip closing *
                    continue;
                }
            }

            current.append(c);
            i++;
        }

        flushText(flow, current.toString());
        return flow;
    }

    private static void flushText(TextFlow flow, String text) {
        if (text != null && !text.isEmpty()) {
            flow.getChildren().add(new Text(text));
        }
    }
}