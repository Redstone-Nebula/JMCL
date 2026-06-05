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
package org.Open_code_Studio.jmcl.ui.construct;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.Open_code_Studio.jmcl.ui.Controllers;
import org.Open_code_Studio.jmcl.ui.FXUtils;
import org.Open_code_Studio.jmcl.ui.SVG;
import org.Open_code_Studio.jmcl.util.StringUtils;
import org.Open_code_Studio.jmcl.util.io.FileUtils;

import java.nio.file.Path;

import static org.Open_code_Studio.jmcl.util.i18n.I18n.i18n;

public class FileSelector extends HBox {
    private final StringProperty value = new SimpleStringProperty();
    private String chooserTitle = "";
    private SelectionMode selectionMode = SelectionMode.FILE;
    private final ObservableList<FileChooser.ExtensionFilter> extensionFilters = FXCollections.observableArrayList();

    MFXButton selectButton = FXUtils.newToggleButton4(SVG.FOLDER_OPEN, 15);

    public enum SelectionMode {
        FILE,
        DIRECTORY,
        FILE_OR_DIRECTORY
    }

    public String getValue() {
        return value.get();
    }

    public StringProperty valueProperty() {
        return value;
    }

    public void setValue(String value) {
        this.value.set(value);
    }

    public String getChooserTitle() {
        return chooserTitle;
    }

    public FileSelector setChooserTitle(String chooserTitle) {
        this.chooserTitle = chooserTitle;
        return this;
    }

    public SelectionMode getSelectionMode() {
        return selectionMode;
    }

    public FileSelector setSelectionMode(SelectionMode selectionMode) {
        this.selectionMode = selectionMode;
        return this;
    }

    public ObservableList<FileChooser.ExtensionFilter> getExtensionFilters() {
        return extensionFilters;
    }

    public FileSelector() {
        MFXTextField customField = new MFXTextField();
        FXUtils.bindString(customField, valueProperty());

        selectButton.setOnAction(e -> {
            switch (selectionMode) {
                case FILE -> openFileChooser(customField);
                case DIRECTORY -> openDirectoryChooser(customField);
                case FILE_OR_DIRECTORY -> {
                    ContextMenu selectPopupMenu = new ContextMenu();
                    CustomMenuItem fileItem = new CustomMenuItem(
                            new IconedMenuItem(SVG.FILE_OPEN, i18n("selector.choose_file"), () -> openFileChooser(customField), null));
                    CustomMenuItem dirItem = new CustomMenuItem(
                            new IconedMenuItem(SVG.FOLDER_OPEN, i18n("selector.choose_directory"), () -> openDirectoryChooser(customField), null));
                    selectPopupMenu.getItems().addAll(fileItem, dirItem);

                    selectPopupMenu.show(selectButton, javafx.geometry.Side.RIGHT, -selectButton.getWidth(), 0);
                }
            }
        });

        setAlignment(Pos.CENTER_LEFT);
        setSpacing(3);
        getChildren().addAll(customField, selectButton);
    }

    private void openFileChooser(MFXTextField customField) {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().addAll(getExtensionFilters());
        chooser.setTitle(StringUtils.isBlank(chooserTitle) ? i18n("selector.choose_file") : chooserTitle);
        Path file = FileUtils.toPath(chooser.showOpenDialog(Controllers.getStage()));
        if (file != null) {
            String path = FileUtils.getAbsolutePath(file);
            customField.setText(path);
            value.setValue(path);
        }
    }

    private void openDirectoryChooser(MFXTextField customField) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(StringUtils.isBlank(chooserTitle) ? i18n("selector.choose_directory") : chooserTitle);
        Path dir = FileUtils.toPath(chooser.showDialog(Controllers.getStage()));
        if (dir != null) {
            String path = FileUtils.getAbsolutePath(dir);
            customField.setText(path);
            value.setValue(path);
        }
    }
}