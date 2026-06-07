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

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.Open_code_Studio.jmcl.Metadata;
import org.Open_code_Studio.jmcl.countly.CrashReport;
import org.Open_code_Studio.jmcl.upgrade.UpdateChecker;

import static org.Open_code_Studio.jmcl.util.i18n.I18n.i18n;

/**
 * @author Open Code Studio
 */
public class CrashWindow extends Stage {

    private static final String GITHUB_ISSUES_URL = "https://github.com/Open-code-Studio/JMCL/issues";

    public CrashWindow(CrashReport report) {
        Label lblCrash = new Label();
        if (report.getThrowable() instanceof InternalError)
            lblCrash.setText(i18n("launcher.crash.java_internal_error"));
        else if (UpdateChecker.isOutdated())
            lblCrash.setText(i18n("launcher.crash.hmcl_out_dated"));
        else
            lblCrash.setText(i18n("launcher.crash"));
        lblCrash.setWrapText(true);
        lblCrash.getStyleClass().add("crash-header");

        TextArea textArea = new TextArea();
        textArea.setText(report.getDisplayText());
        textArea.setEditable(false);
        textArea.getStyleClass().add("crash-text-area");

        Button btnReport = new Button(i18n("contact.feedback.github.statement"));
        btnReport.getStyleClass().add("md3-text-button");
        btnReport.setOnAction(e -> FXUtils.openLink(GITHUB_ISSUES_URL));

        HBox bottomBar = new HBox(btnReport);
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        bottomBar.setPadding(new Insets(8, 12, 8, 12));

        BorderPane pane = new BorderPane();
        pane.getStyleClass().add("crash-window");
        StackPane stackPane = new StackPane();
        stackPane.setStyle("-fx-padding: 8px;");
        stackPane.getChildren().add(lblCrash);
        pane.setTop(stackPane);
        pane.setCenter(textArea);
        pane.setBottom(bottomBar);

        Scene scene = new Scene(pane, 800, 480);
        setScene(scene);
        FXUtils.setIcon(this);
        setTitle(i18n("message.error"));

        setOnCloseRequest(e -> javafx.application.Platform.exit());
    }

}
