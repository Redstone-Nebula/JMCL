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

import static javafx.collections.FXCollections.emptyObservableList;
import static javafx.collections.FXCollections.observableList;
import static javafx.collections.FXCollections.singletonObservableList;

import javafx.util.Callback;
import org.Open_code_Studio.jmcl.ui.FXUtils;
import org.Open_code_Studio.jmcl.util.javafx.BindingMapping;

import io.github.palexdev.materialfx.controls.MFXComboBox;

import javafx.beans.binding.Bindings;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.text.Font;

public final class FontComboBox extends MFXComboBox<String> {

    private boolean loaded = false;

    public FontComboBox() {
        setMinWidth(260);

        styleProperty().bind(Bindings.concat("-fx-font-family: \"", valueProperty(), "\""));

        setCellFactory((Callback<ListView<String>, ListCell<String>>) listView -> new ListCell<String>() {
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty) {
                    setText(item);
                    setGraphic(null);
                    setStyle("-fx-font-family: \"" + item + "\"");
                }
            }
        });

        itemsProperty().bind(BindingMapping.of(valueProperty())
                        .map(value -> value == null ? emptyObservableList() : singletonObservableList(value)));

        FXUtils.onClicked(this, () -> {
            if (loaded)
                return;
            itemsProperty().unbind();
            setItems(observableList(Font.getFamilies()));
            loaded = true;
        });
    }
}