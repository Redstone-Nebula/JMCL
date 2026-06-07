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
package org.Open_code_Studio.jmcl.ui.construct;

import javafx.scene.control.Button;
import com.jfoenix.controls.JFXDialogLayout;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;

import static org.Open_code_Studio.jmcl.ui.FXUtils.onEscPressed;
import static org.Open_code_Studio.jmcl.util.i18n.I18n.i18n;

public class DialogPane extends JFXDialogLayout {
    private final StringProperty title = new SimpleStringProperty();
    private final BooleanProperty valid = new SimpleBooleanProperty(true);
    protected final SpinnerPane acceptPane = new SpinnerPane();
    protected final Button cancelButton = new Button();
    protected final Label warningLabel = new Label();
    private final ProgressBar progressBar = new ProgressBar();

    public DialogPane() {
        progressBar.getStyleClass().add("md3-linear-progress");
        Label titleLabel = new Label();
        titleLabel.textProperty().bind(title);
        setHeading(titleLabel);
        getChildren().add(progressBar);

        progressBar.setVisible(false);
        StackPane.setMargin(progressBar, new Insets(-24.0D, -24.0D, -16.0D, -24.0D));
        StackPane.setAlignment(progressBar, Pos.TOP_CENTER);
        progressBar.setMaxWidth(Double.MAX_VALUE);

        Button acceptButton = new Button(i18n("button.ok"));
        acceptButton.setOnAction(e -> onAccept());
        acceptButton.disableProperty().bind(valid.not());
        acceptButton.getStyleClass().add("dialog-accept");
        acceptPane.getStyleClass().add("small-spinner-pane");
        acceptPane.setContent(acceptButton);

        cancelButton.setText(i18n("button.cancel"));
        cancelButton.setOnAction(e -> onCancel());
        cancelButton.getStyleClass().add("dialog-cancel");
        onEscPressed(this, cancelButton::fire);

        setActions(warningLabel, acceptPane, cancelButton);
    }

    protected ProgressBar getProgressBar() {
        return progressBar;
    }

    public String getTitle() {
        return title.get();
    }

    public StringProperty titleProperty() {
        return title;
    }

    public void setTitle(String title) {
        this.title.set(title);
    }

    public boolean isValid() {
        return valid.get();
    }

    public BooleanProperty validProperty() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid.set(valid);
    }

    protected void onCancel() {
        fireEvent(new DialogCloseEvent());
    }

    protected void onAccept() {
        fireEvent(new DialogCloseEvent());
    }

    protected void setLoading() {
        acceptPane.showSpinner();
        warningLabel.setText("");
    }

    protected void onSuccess() {
        acceptPane.hideSpinner();
        fireEvent(new DialogCloseEvent());
    }

    protected void onFailure(String msg) {
        acceptPane.hideSpinner();
        warningLabel.setText(msg);
    }
}
