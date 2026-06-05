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

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.Open_code_Studio.jmcl.ui.FXUtils;
import org.Open_code_Studio.jmcl.util.FutureCallback;

import java.util.concurrent.CompletableFuture;

import static org.Open_code_Studio.jmcl.ui.FXUtils.onEscPressed;
import static org.Open_code_Studio.jmcl.util.i18n.I18n.i18n;

public class InputDialogPane extends VBox implements DialogAware {
    private final CompletableFuture<String> future = new CompletableFuture<>();

    private final MFXTextField textField;
    private final Label lblCreationWarning;
    private final SpinnerPane acceptPane;
    private final MFXButton acceptButton;

    public InputDialogPane(String text, String initialValue, FutureCallback<String> onResult) {
        textField = new MFXTextField(initialValue);

        lblCreationWarning = new Label();
        lblCreationWarning.setPadding(new Insets(0, 5, 0, 0));

        acceptPane = new SpinnerPane();
        acceptPane.getStyleClass().add("small-spinner-pane");
        acceptButton = new MFXButton(i18n("button.ok"));
        acceptButton.getStyleClass().add("dialog-accept");
        acceptPane.setContent(acceptButton);

        MFXButton cancelButton = new MFXButton(i18n("button.cancel"));
        cancelButton.getStyleClass().add("dialog-cancel");

        HBox heading = new HBox(new Label(text));
        HBox actions = new HBox(lblCreationWarning, acceptPane, cancelButton);
        VBox body = new VBox(textField);

        getChildren().setAll(heading, body, actions);
        setSpacing(8);

        cancelButton.setOnAction(e -> fireEvent(new DialogCloseEvent()));
        acceptButton.setOnAction(e -> {
            acceptPane.showSpinner();

            onResult.call(textField.getText(), new FutureCallback.ResultHandler() {
                @Override
                public void resolve() {
                    acceptPane.hideSpinner();
                    future.complete(textField.getText());
                    fireEvent(new DialogCloseEvent());
                }

                @Override
                public void reject(String reason) {
                    acceptPane.hideSpinner();
                    lblCreationWarning.setText(reason);
                }
            });
        });
        textField.setOnAction(event -> acceptButton.fire());
        onEscPressed(this, cancelButton::fire);
    }

    @Override
    public void onDialogShown() {
        textField.requestFocus();
    }

    public CompletableFuture<String> getCompletableFuture() {
        return future;
    }
}