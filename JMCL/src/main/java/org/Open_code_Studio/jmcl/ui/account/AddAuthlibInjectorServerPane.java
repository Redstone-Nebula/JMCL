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
package org.Open_code_Studio.jmcl.ui.account;

import javafx.beans.binding.Bindings;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import com.jfoenix.controls.JFXDialogLayout;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import org.Open_code_Studio.jmcl.auth.authlibinjector.AuthlibInjectorServer;
import org.Open_code_Studio.jmcl.task.Schedulers;
import org.Open_code_Studio.jmcl.task.Task;
import org.Open_code_Studio.jmcl.ui.FXUtils;
import org.Open_code_Studio.jmcl.ui.animation.ContainerAnimations;
import org.Open_code_Studio.jmcl.ui.animation.TransitionPane;
import org.Open_code_Studio.jmcl.ui.construct.*;
import org.Open_code_Studio.jmcl.util.Lang;

import javax.net.ssl.SSLException;
import java.io.IOException;

import static org.Open_code_Studio.jmcl.setting.ConfigHolder.config;
import static org.Open_code_Studio.jmcl.ui.FXUtils.onEscPressed;
import static org.Open_code_Studio.jmcl.util.logging.Logger.LOG;
import static org.Open_code_Studio.jmcl.util.i18n.I18n.i18n;

public final class AddAuthlibInjectorServerPane extends TransitionPane implements DialogAware {

    private final Label lblServerUrl;
    private final Label lblServerName;
    private final Label lblCreationWarning;
    private final Label lblServerWarning;
    private final TextField txtServerUrl;
    private final JFXDialogLayout addServerPane;
    private final JFXDialogLayout confirmServerPane;
    private final SpinnerPane nextPane;
    private final Button btnAddNext;

    private AuthlibInjectorServer serverBeingAdded;

    public AddAuthlibInjectorServerPane(String url) {
        this();
        txtServerUrl.setText(url);
        onAddNext();
    }

    public AddAuthlibInjectorServerPane() {
        addServerPane = new JFXDialogLayout();
        addServerPane.setHeading(new Label(i18n("account.injector.add")));
        {
            txtServerUrl = new TextField();
            txtServerUrl.setPromptText(i18n("account.injector.server_url"));
            txtServerUrl.setOnAction(e -> onAddNext());

            lblCreationWarning = new Label();
            lblCreationWarning.setWrapText(true);
            HBox actions = new HBox();
            {
                Button cancel = new Button(i18n("button.cancel"));
                cancel.getStyleClass().add("dialog-accept");
                cancel.setOnAction(e -> onAddCancel());

                nextPane = new SpinnerPane();
                nextPane.getStyleClass().add("small-spinner-pane");
                btnAddNext = new Button(i18n("wizard.next"));
                btnAddNext.getStyleClass().add("dialog-accept");
                btnAddNext.setOnAction(e -> onAddNext());
                nextPane.setContent(btnAddNext);

                actions.getChildren().setAll(cancel, nextPane);
            }

            addServerPane.setBody(txtServerUrl);
            addServerPane.setActions(lblCreationWarning, actions);

            FXUtils.setValidateWhileTextChanged(txtServerUrl, true);
            btnAddNext.disableProperty().bind(Bindings.createBooleanBinding(() -> txtServerUrl.getText().isEmpty(), txtServerUrl.textProperty()));
        }

        confirmServerPane = new JFXDialogLayout();
        confirmServerPane.setHeading(new Label(i18n("account.injector.add")));
        {
            GridPane body = new GridPane();
            body.setStyle("-fx-padding: 15 0 0 0;");
            body.setVgap(15);
            body.setHgap(15);
            {
                body.getColumnConstraints().setAll(
                        Lang.apply(new ColumnConstraints(), c -> c.setMaxWidth(100)),
                        new ColumnConstraints()
                );

                lblServerUrl = new Label();
                GridPane.setColumnIndex(lblServerUrl, 1);
                GridPane.setRowIndex(lblServerUrl, 0);

                lblServerName = new Label();
                GridPane.setColumnIndex(lblServerName, 1);
                GridPane.setRowIndex(lblServerName, 1);

                lblServerWarning = new Label(i18n("account.injector.http"));
                lblServerWarning.setStyle("-fx-text-fill: -monet-error;");
                GridPane.setColumnIndex(lblServerWarning, 0);
                GridPane.setRowIndex(lblServerWarning, 2);
                lblServerWarning.managedProperty().bind(lblServerWarning.visibleProperty());
                GridPane.setColumnSpan(lblServerWarning, 2);

                body.getChildren().setAll(
                        Lang.apply(new Label(i18n("account.injector.server_url")), l -> {
                            GridPane.setColumnIndex(l, 0);
                            GridPane.setRowIndex(l, 0);
                        }),
                        Lang.apply(new Label(i18n("account.injector.server_name")), l -> {
                            GridPane.setColumnIndex(l, 0);
                            GridPane.setRowIndex(l, 1);
                        }),
                        lblServerUrl, lblServerName, lblServerWarning
                );
            }

            Button prevButton = new Button(i18n("wizard.prev"));
            prevButton.getStyleClass().add("dialog-cancel");
            prevButton.setOnAction(e -> onAddPrev());

            Button cancelButton = new Button(i18n("button.cancel"));
            cancelButton.getStyleClass().add("dialog-cancel");
            cancelButton.setOnAction(e -> onAddCancel());

            Button finishButton = new Button(i18n("wizard.finish"));
            finishButton.getStyleClass().add("dialog-accept");
            finishButton.setOnAction(e -> onAddFinish());

            confirmServerPane.setBody(body);
            confirmServerPane.setActions(prevButton, cancelButton, finishButton);
        }

        this.setContent(addServerPane, ContainerAnimations.NONE);

        lblCreationWarning.maxWidthProperty().bind(((FlowPane) lblCreationWarning.getParent()).widthProperty());
        nextPane.hideSpinner();

        onEscPressed(this, this::onAddCancel);
    }

    @Override
    public void onDialogShown() {
        txtServerUrl.requestFocus();
    }

    private String resolveFetchExceptionMessage(Throwable exception) {
        if (exception instanceof SSLException) {
            if (exception.getMessage() != null && exception.getMessage().contains("Remote host terminated")) {
                return i18n("account.failed.connect_injector_server");
            }
            if (exception.getMessage() != null && (exception.getMessage().contains("No name matching") || exception.getMessage().contains("No subject alternative DNS name matching"))) {
                return i18n("account.failed.dns");
            }
            return i18n("account.failed.ssl");
        } else if (exception instanceof IOException) {
            return i18n("account.failed.connect_injector_server");
        } else {
            return exception.getClass().getName() + ": " + exception.getLocalizedMessage();
        }
    }

    private void onAddCancel() {
        fireEvent(new DialogCloseEvent());
    }

    private void onAddNext() {
        if (btnAddNext.isDisabled())
            return;

        lblCreationWarning.setText("");

        String url = txtServerUrl.getText();

        nextPane.showSpinner();
        addServerPane.setDisable(true);

        Task.runAsync(() -> {
            serverBeingAdded = AuthlibInjectorServer.locateServer(url);
        }).whenComplete(Schedulers.javafx(), exception -> {
            addServerPane.setDisable(false);
            nextPane.hideSpinner();

            if (exception == null) {
                lblServerName.setText(serverBeingAdded.getName());
                lblServerUrl.setText(serverBeingAdded.getUrl());

                //noinspection HttpUrlsUsage
                lblServerWarning.setVisible(serverBeingAdded.getUrl().startsWith("http://"));

                this.setContent(confirmServerPane, ContainerAnimations.SWIPE_LEFT);
            } else {
                LOG.warning("Failed to resolve auth server: " + url, exception);
                lblCreationWarning.setText(resolveFetchExceptionMessage(exception));
            }
        }).start();

    }

    private void onAddPrev() {
        this.setContent(addServerPane, ContainerAnimations.SWIPE_RIGHT);
    }

    private void onAddFinish() {
        if (!config().getAuthlibInjectorServers().contains(serverBeingAdded)) {
            config().getAuthlibInjectorServers().add(serverBeingAdded);
        }
        fireEvent(new DialogCloseEvent());
    }

}
