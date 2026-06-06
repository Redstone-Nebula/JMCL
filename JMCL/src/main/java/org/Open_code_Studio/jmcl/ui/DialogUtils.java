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
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import org.Open_code_Studio.jmcl.ui.animation.AnimationUtils;
import org.Open_code_Studio.jmcl.ui.construct.DialogAware;
import org.Open_code_Studio.jmcl.ui.construct.DialogCloseEvent;
import org.Open_code_Studio.jmcl.ui.construct.JFXDialogPane;
import org.Open_code_Studio.jmcl.ui.decorator.Decorator;

import java.util.Optional;

public final class DialogUtils {
    private DialogUtils() {
    }

    public static final String PROPERTY_DIALOG_INSTANCE = DialogUtils.class.getName() + ".dialog.instance";
    public static final String PROPERTY_DIALOG_PANE_INSTANCE = DialogUtils.class.getName() + ".dialog.pane.instance";
    public static final String PROPERTY_DIALOG_CLOSE_HANDLER = DialogUtils.class.getName() + ".dialog.closeListener";

    public static final String PROPERTY_PARENT_PANE_REF = DialogUtils.class.getName() + ".dialog.parentPaneRef";

    public static void show(Decorator decorator, Node content) {
        if (decorator.getDrawerWrapper() == null) {
            Platform.runLater(() -> show(decorator, content));
            return;
        }

        StackPane container = decorator.getDrawerWrapper();

        JFXDialogPane dialogPane = new JFXDialogPane();
        decorator.capableDraggingWindow(dialogPane);
        decorator.forbidDraggingWindow(dialogPane);

        show(container, content, dialogPane);
    }

    public static void show(StackPane container, Node content) {
        show(container, content, null);
    }

    private static void show(StackPane container, Node content, @org.jetbrains.annotations.Nullable JFXDialogPane dialogPane) {
        FXUtils.checkFxUserThread();

        JFXDialogPane existingPane = (JFXDialogPane) container.getProperties().get(PROPERTY_DIALOG_PANE_INSTANCE);

        if (existingPane == null) {
            JFXDialogPane pane = dialogPane != null ? dialogPane : new JFXDialogPane();

            container.getProperties().put(PROPERTY_DIALOG_PANE_INSTANCE, pane);
            container.getChildren().add(pane);

            existingPane = pane;
        }

        content.getProperties().put(PROPERTY_PARENT_PANE_REF, existingPane);

        existingPane.push(content);

        EventHandler<DialogCloseEvent> handler = event -> close(content);
        content.getProperties().put(PROPERTY_DIALOG_CLOSE_HANDLER, handler);
        content.addEventHandler(DialogCloseEvent.CLOSE, handler);

        handleDialogShown(existingPane, content);
    }

    private static void handleDialogShown(JFXDialogPane dialogPane, Node node) {
        if (dialogPane.isVisible()) {
            dialogPane.requestFocus();
            if (node instanceof DialogAware dialogAware)
                dialogAware.onDialogShown();
        } else {
            dialogPane.visibleProperty().addListener(new ChangeListener<>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                    if (newValue) {
                        dialogPane.requestFocus();
                        if (node instanceof DialogAware dialogAware)
                            dialogAware.onDialogShown();
                        observable.removeListener(this);
                    }
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    public static void close(Node content) {
        FXUtils.checkFxUserThread();

        Optional.ofNullable(content.getProperties().get(PROPERTY_DIALOG_CLOSE_HANDLER))
                .ifPresent(handler -> content.removeEventHandler(DialogCloseEvent.CLOSE, (EventHandler<DialogCloseEvent>) handler));

        JFXDialogPane pane = (JFXDialogPane) content.getProperties().get(PROPERTY_PARENT_PANE_REF);

        if (pane != null) {
            if (pane.size() == 1 && pane.peek().orElse(null) == content) {
                pane.pop(content);

                StackPane container = (StackPane) pane.getParent();
                if (container != null) {
                    container.getChildren().remove(pane);
                    container.getProperties().remove(PROPERTY_DIALOG_PANE_INSTANCE);
                    container.getProperties().remove(PROPERTY_PARENT_PANE_REF);
                }
            } else {
                pane.pop(content);
            }

            if (content instanceof DialogAware dialogAware) {
                dialogAware.onDialogClosed();
            }
        }
    }
}