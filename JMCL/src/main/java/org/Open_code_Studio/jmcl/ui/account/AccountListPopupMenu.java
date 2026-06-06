/*
 * JMCL
 * Copyright (C) 2026 OCS
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

import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import org.Open_code_Studio.jmcl.auth.Account;
import org.Open_code_Studio.jmcl.setting.Accounts;
import org.Open_code_Studio.jmcl.ui.FXUtils;
import org.Open_code_Studio.jmcl.ui.construct.AdvancedListBox;

import static org.Open_code_Studio.jmcl.util.i18n.I18n.i18n;

public final class AccountListPopupMenu extends StackPane {
    public static void show(Node owner, Side vAlign, Side hAlign,
                            double initOffsetX, double initOffsetY) {
        var menu = new AccountListPopupMenu();
        Popup popup = new Popup();
        popup.getContent().add(menu);
        popup.setAutoHide(true);

        // Calculate position based on alignment
        var bounds = owner.localToScreen(owner.getBoundsInLocal());
        if (bounds == null) return;

        double x = bounds.getMinX();
        double y = bounds.getMinY();

        if (hAlign == Side.LEFT) {
            x = bounds.getMinX() + initOffsetX;
        } else if (hAlign == Side.RIGHT) {
            x = bounds.getMaxX() - initOffsetX;
        }

        if (vAlign == Side.TOP) {
            y = bounds.getMinY() + initOffsetY;
        } else if (vAlign == Side.BOTTOM) {
            y = bounds.getMaxY() - initOffsetY;
        }

        popup.show(owner, x, y);
    }

    @SuppressWarnings("FieldCanBeLocal")
    private final BooleanBinding isEmpty = Bindings.isEmpty(Accounts.getAccounts());
    @SuppressWarnings("FieldCanBeLocal")
    private final InvalidationListener listener;

    public AccountListPopupMenu() {
        AdvancedListBox box = new AdvancedListBox();
        box.getStyleClass().add("no-padding");
        box.setPrefWidth(220);
        box.setPrefHeight(-1);
        box.setMaxHeight(260);

        listener = o -> {
            box.clear();

            for (Account account : Accounts.getAccounts()) {
                AccountAdvancedListItem item = new AccountAdvancedListItem(account);
                item.setOnAction(e -> {
                    Accounts.setSelectedAccount(account);
                    if (getScene().getWindow() instanceof Popup popup)
                        popup.hide();
                });
                box.add(item);
            }
        };
        listener.invalidated(null);
        Accounts.getAccounts().addListener(new WeakInvalidationListener(listener));

        Label placeholder = new Label(i18n("account.empty"));
        placeholder.setStyle("-fx-padding: 10px; -fx-text-fill: -monet-on-surface-variant; -fx-font-style: italic;");

        FXUtils.onChangeAndOperate(isEmpty, empty -> {
            getChildren().setAll(empty ? placeholder : box);
        });
    }

}