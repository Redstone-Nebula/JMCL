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
package org.Open_code_Studio.jmcl.ui.download;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXListView;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import org.Open_code_Studio.jmcl.download.DownloadProvider;
import org.Open_code_Studio.jmcl.download.RemoteVersion;
import org.Open_code_Studio.jmcl.download.VersionList;
import org.Open_code_Studio.jmcl.download.cleanroom.CleanroomRemoteVersion;
import org.Open_code_Studio.jmcl.download.fabric.FabricAPIRemoteVersion;
import org.Open_code_Studio.jmcl.download.fabric.FabricRemoteVersion;
import org.Open_code_Studio.jmcl.download.forge.ForgeRemoteVersion;
import org.Open_code_Studio.jmcl.download.game.GameRemoteVersion;
import org.Open_code_Studio.jmcl.download.legacyfabric.LegacyFabricAPIRemoteVersion;
import org.Open_code_Studio.jmcl.download.legacyfabric.LegacyFabricRemoteVersion;
import org.Open_code_Studio.jmcl.download.liteloader.LiteLoaderRemoteVersion;
import org.Open_code_Studio.jmcl.download.neoforge.NeoForgeRemoteVersion;
import org.Open_code_Studio.jmcl.download.optifine.OptiFineRemoteVersion;
import org.Open_code_Studio.jmcl.download.quilt.QuiltAPIRemoteVersion;
import org.Open_code_Studio.jmcl.download.quilt.QuiltRemoteVersion;
import org.Open_code_Studio.jmcl.setting.VersionIconType;
import org.Open_code_Studio.jmcl.task.Schedulers;
import org.Open_code_Studio.jmcl.task.Task;
import org.Open_code_Studio.jmcl.ui.FXUtils;
import org.Open_code_Studio.jmcl.ui.SVG;
import org.Open_code_Studio.jmcl.ui.animation.ContainerAnimations;
import org.Open_code_Studio.jmcl.ui.animation.TransitionPane;
import org.Open_code_Studio.jmcl.ui.construct.ComponentList;
import org.Open_code_Studio.jmcl.ui.construct.RipplerContainer;
import org.Open_code_Studio.jmcl.ui.construct.SpinnerPane;
import org.Open_code_Studio.jmcl.ui.construct.TwoLineListItem;
import org.Open_code_Studio.jmcl.ui.wizard.Navigation;
import org.Open_code_Studio.jmcl.ui.wizard.Refreshable;
import org.Open_code_Studio.jmcl.ui.wizard.WizardPage;
import org.Open_code_Studio.jmcl.util.NativePatcher;
import org.Open_code_Studio.jmcl.util.StringUtils;
import org.Open_code_Studio.jmcl.util.i18n.I18n;
import org.Open_code_Studio.jmcl.util.platform.OperatingSystem;
import org.Open_code_Studio.jmcl.util.platform.Platform;
import org.Open_code_Studio.jmcl.util.versioning.GameVersionNumber;

import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.Open_code_Studio.jmcl.ui.FXUtils.*;
import static org.Open_code_Studio.jmcl.util.i18n.I18n.i18n;
import static org.Open_code_Studio.jmcl.util.logging.Logger.LOG;

public final class VersionsPage extends Control implements WizardPage, Refreshable {
    private final String gameVersion;
    private final String libraryId;
    private final String title;
    private final Navigation navigation;
    private final DownloadProvider downloadProvider;
    private final VersionList<?> versionList;
    private final Runnable callback;

    private final ObservableList<RemoteVersion> versions = FXCollections.observableArrayList();
    private final ObjectProperty<Status> status = new SimpleObjectProperty<>(Status.LOADING);

    public VersionsPage(Navigation navigation,
                        String title, String gameVersion,
                        DownloadProvider downloadProvider,
                        String libraryId,
                        Runnable callback) {
        this.title = title;
        this.gameVersion = gameVersion;
        this.libraryId = libraryId;
        this.navigation = navigation;
        this.downloadProvider = downloadProvider;
        this.versionList = downloadProvider.getVersionListById(libraryId);
        this.callback = callback;

        refresh();
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new VersionsPageSkin(this);
    }

    @Override
    public void refresh() {
        status.set(Status.LOADING);
        Task<?> task = versionList.refreshAsync(gameVersion)
                .thenSupplyAsync(() -> versionList.getVersions(gameVersion).stream().sorted().collect(Collectors.toList()))
                .whenComplete(Schedulers.javafx(), (items, exception) -> {
                    if (exception == null) {
                        versions.setAll(items);
                        status.set(Status.SUCCESS);
                    } else {
                        LOG.warning("Failed to fetch versions list", exception);
                        status.set(Status.FAILED);
                    }
                });
        task.start();
    }

    @Override
    public String getTitle() {
        return title;
    }

    private void onRefresh() {
        refresh();
    }

    private void onBack() {
        navigation.onPrev(true);
    }

    private enum Status {
        LOADING,
        FAILED,
        SUCCESS,
    }

    private enum VersionTypeFilter {
        ALL,
        RELEASE,
        SNAPSHOTS,
        APRIL_FOOLS,
        OLD
    }

    private static class RemoteVersionListCell extends ListCell<RemoteVersion> {
        private final VersionsPage control;

        private final TwoLineListItem twoLineListItem = new TwoLineListItem();
        private final ImageView imageView = new ImageView();
        private final StackPane pane = new StackPane();

        RemoteVersionListCell(VersionsPage control) {
            this.control = control;

            imageView.setMouseTransparent(true);

            HBox hbox = new HBox(16);
            HBox.setHgrow(twoLineListItem, Priority.ALWAYS);
            hbox.setAlignment(Pos.CENTER);

            HBox actions = new HBox(8);
            actions.setAlignment(Pos.CENTER);
            {
                if ("game".equals(control.libraryId)) {
                    MFXButton wikiButton = newToggleButton4(SVG.GLOBE_BOOK);
                    wikiButton.setOnAction(event -> onOpenWiki());
                    FXUtils.installFastTooltip(wikiButton, i18n("wiki.tooltip"));
                    actions.getChildren().add(wikiButton);
                }

                MFXButton actionButton = newToggleButton4(SVG.ARROW_FORWARD);
                actionButton.setOnAction(e -> onAction());
                actions.getChildren().add(actionButton);
            }

            hbox.getChildren().setAll(imageView, twoLineListItem, actions);

            pane.getStyleClass().add("md-list-cell");
            StackPane.setMargin(hbox, new Insets(10, 16, 10, 16));
            pane.getChildren().setAll(new RipplerContainer(hbox));

            FXUtils.onClicked(this, this::onAction);
        }

        private void onAction() {
            RemoteVersion item = getItem();
            if (item == null)
                return;

            control.navigation.getSettings().put(control.libraryId, item);
            control.callback.run();
        }

        private void onOpenWiki() {
            RemoteVersion item = getItem();
            if (!(item instanceof GameRemoteVersion))
                return;

            FXUtils.openLink(I18n.getWikiLink((GameRemoteVersion) item));
        }

        @Override
        public void updateItem(RemoteVersion remoteVersion, boolean empty) {
            super.updateItem(remoteVersion, empty);

            if (empty) {
                setGraphic(null);
                return;
            }
            setGraphic(pane);

            twoLineListItem.setTitle(I18n.getDisplayVersion(remoteVersion));
            if (remoteVersion.getReleaseDate() != null) {
                twoLineListItem.setSubtitle(I18n.formatDateTime(remoteVersion.getReleaseDate()));
            } else {
                twoLineListItem.setSubtitle(null);
            }
            twoLineListItem.getTags().clear();

            if (remoteVersion instanceof GameRemoteVersion) {
                RemoteVersion.Type versionType = remoteVersion.getVersionType();
                GameVersionNumber gameVersion = GameVersionNumber.asGameVersion(remoteVersion.getGameVersion());

                switch (versionType) {
                    case RELEASE -> {
                        twoLineListItem.addTag(i18n("version.game.release"));
                        imageView.setImage(VersionIconType.GRASS.getIcon());
                    }
                    case SNAPSHOT, PENDING, UNOBFUSCATED -> {
                        if (versionType == RemoteVersion.Type.SNAPSHOT
                                && GameVersionNumber.asGameVersion(remoteVersion.getGameVersion()).isAprilFools()) {
                            twoLineListItem.addTag(i18n("version.game.april_fools"));
                            imageView.setImage(VersionIconType.APRIL_FOOLS.getIcon());
                        } else {
                            twoLineListItem.addTag(i18n("version.game.snapshot"));
                            imageView.setImage(VersionIconType.COMMAND.getIcon());
                        }
                    }
                    default -> {
                        twoLineListItem.addTag(i18n("version.game.old"));
                        imageView.setImage(VersionIconType.CRAFT_TABLE.getIcon());
                    }
                }

                switch (NativePatcher.checkSupportedStatus(gameVersion, Platform.SYSTEM_PLATFORM, OperatingSystem.SYSTEM_VERSION)) {
                    case UNTESTED -> twoLineListItem.addTagWarning(i18n("version.game.support_status.untested"));
                    case UNSUPPORTED -> twoLineListItem.addTagWarning(i18n("version.game.support_status.unsupported"));
                }
            } else {
                VersionIconType iconType;
                if (remoteVersion instanceof LiteLoaderRemoteVersion)
                    iconType = VersionIconType.CHICKEN;
                else if (remoteVersion instanceof OptiFineRemoteVersion)
                    iconType = VersionIconType.OPTIFINE;
                else if (remoteVersion instanceof ForgeRemoteVersion)
                    iconType = VersionIconType.FORGE;
                else if (remoteVersion instanceof CleanroomRemoteVersion)
                    iconType = VersionIconType.CLEANROOM;
                else if (remoteVersion instanceof NeoForgeRemoteVersion)
                    iconType = VersionIconType.NEO_FORGE;
                else if (remoteVersion instanceof LegacyFabricRemoteVersion || remoteVersion instanceof LegacyFabricAPIRemoteVersion)
                    iconType = VersionIconType.LEGACY_FABRIC;
                else if (remoteVersion instanceof FabricRemoteVersion || remoteVersion instanceof FabricAPIRemoteVersion)
                    iconType = VersionIconType.FABRIC;
                else if (remoteVersion instanceof QuiltRemoteVersion || remoteVersion instanceof QuiltAPIRemoteVersion)
                    iconType = VersionIconType.QUILT;
                else
                    iconType = VersionIconType.COMMAND;

                imageView.setImage(iconType.getIcon());
                String displayGameVersion = I18n.getDisplayVersion(GameVersionNumber.asGameVersion(remoteVersion.getGameVersion()));

                if (twoLineListItem.getSubtitle() == null)
                    twoLineListItem.setSubtitle(displayGameVersion);
                else
                    twoLineListItem.addTag(displayGameVersion);
            }
        }
    }

    private static final class VersionsPageSkin extends SkinBase<VersionsPage> {
        private final MFXListView<RemoteVersion, ?> list;

        private final TransitionPane transitionPane;
        private final ProgressIndicator spinner;

        private final MFXTextField nameField;
        private final ComboBox<VersionTypeFilter> categoryField = new ComboBox<>();

        VersionsPageSkin(VersionsPage control) {
            super(control);

            BorderPane root = new BorderPane();

            GridPane searchPane = new GridPane();
            root.setTop(searchPane);
            searchPane.getStyleClass().addAll("card");
            BorderPane.setMargin(searchPane, new Insets(10, 10, 0, 10));

            ColumnConstraints nameColumn = new ColumnConstraints();
            nameColumn.setMinWidth(USE_PREF_SIZE);
            ColumnConstraints column1 = new ColumnConstraints();
            column1.setHgrow(Priority.ALWAYS);
            ColumnConstraints column2 = new ColumnConstraints();
            column2.setMaxWidth(150);
            ColumnConstraints column3 = new ColumnConstraints();

            if (control.versionList.hasType())
                searchPane.getColumnConstraints().setAll(nameColumn, column1, nameColumn, column2, column3);
            else
                searchPane.getColumnConstraints().setAll(nameColumn, column1, column3);

            searchPane.setHgap(16);
            searchPane.setVgap(10);

            {
                int rowIndex = 0;

                {
                    nameField = new MFXTextField();
                    nameField.setPromptText(i18n("version.search.prompt"));
                    nameField.textProperty().addListener(o -> updateList());

                    if ("game".equals(control.libraryId)) {
                        categoryField.getItems().setAll(
                                VersionTypeFilter.ALL,
                                VersionTypeFilter.RELEASE,
                                VersionTypeFilter.SNAPSHOTS,
                                VersionTypeFilter.APRIL_FOOLS,
                                VersionTypeFilter.OLD
                        );
                        categoryField.getSelectionModel().select(VersionTypeFilter.RELEASE);
                    } else {
                        categoryField.getItems().setAll(
                                VersionTypeFilter.ALL,
                                VersionTypeFilter.RELEASE,
                                VersionTypeFilter.SNAPSHOTS
                        );
                        categoryField.getSelectionModel().select(VersionTypeFilter.ALL);
                    }
                    categoryField.setConverter(stringConverter(type -> i18n("version.game." + type.name().toLowerCase(Locale.ROOT))));
                    categoryField.getSelectionModel().selectedItemProperty().addListener(o -> updateList());

                    MFXButton refreshButton = FXUtils.newRaisedButton(i18n("button.refresh"));
                    refreshButton.setOnAction(event -> control.onRefresh());

                    if (control.versionList.hasType()) {
                        searchPane.addRow(rowIndex++,
                                new Label(i18n("version.search")), nameField,
                                new Label(i18n("version.game.type")), categoryField,
                                refreshButton
                        );
                    } else {
                        searchPane.addRow(rowIndex++,
                                new Label(i18n("version.search")), nameField,
                                refreshButton
                        );
                    }
                }
            }

            {
                SpinnerPane spinnerPane = new SpinnerPane();
                root.setCenter(spinnerPane);

                transitionPane = new TransitionPane();
                spinner = new ProgressIndicator();

                StackPane centerWrapper = new StackPane();
                centerWrapper.setStyle("-fx-padding: 10;");
                {
                    ComponentList centrePane = new ComponentList();
                    centrePane.getStyleClass().add("no-padding");
                    {
                        list = new MFXListView<>();
                        list.getStyleClass().add("jfx-list-view-float");
                        VBox.setVgrow(list, Priority.ALWAYS);

                        control.versions.addListener((InvalidationListener) o -> updateList());

                        list.setCellFactory(listView -> new RemoteVersionListCell(control));

                        ComponentList.setVgrow(list, Priority.ALWAYS);

                        // ListViewBehavior would consume ESC pressed event, preventing us from handling it, so we ignore it here
                        ignoreEvent(list, KeyEvent.KEY_PRESSED, e -> e.getCode() == KeyCode.ESCAPE);

                        centrePane.getContent().setAll(list);
                    }

                    centerWrapper.getChildren().setAll(centrePane);
                }

                StackPane failedPane = new StackPane();
                failedPane.getStyleClass().add("notice-pane");
                {
                    Label label = new Label(i18n("download.failed.refresh"));
                    FXUtils.onClicked(label, control::onRefresh);

                    failedPane.getChildren().setAll(label);
                }

                StackPane emptyPane = new StackPane();
                emptyPane.getStyleClass().add("notice-pane");
                {
                    Label label = new Label(i18n("download.failed.empty"));
                    FXUtils.onClicked(label, control::onBack);

                    emptyPane.getChildren().setAll(label);
                }

                FXUtils.onChangeAndOperate(control.status, status -> {
                    if (status == Status.LOADING)
                        transitionPane.setContent(spinner, ContainerAnimations.FADE);
                    else if (status == Status.SUCCESS)
                        if (control.versions.isEmpty()) {
                            transitionPane.setContent(emptyPane, ContainerAnimations.FADE);
                        } else {
                            transitionPane.setContent(centerWrapper, ContainerAnimations.FADE);
                        }
                    else // if (status == Status.FAILED)
                        transitionPane.setContent(failedPane, ContainerAnimations.FADE);
                });

                root.setCenter(transitionPane);
            }

            this.getChildren().setAll(root);
        }

        private void updateList() {
            Stream<RemoteVersion> versions = getSkinnable().versions.stream();

            VersionTypeFilter filter = categoryField.getSelectionModel().getSelectedItem();
            if (filter != null)
                versions = versions.filter(it -> {
                    RemoteVersion.Type versionType = it.getVersionType();
                    return switch (filter) {
                        case RELEASE -> versionType == RemoteVersion.Type.RELEASE;
                        case SNAPSHOTS -> versionType == RemoteVersion.Type.SNAPSHOT
                                || versionType == RemoteVersion.Type.PENDING
                                || versionType == RemoteVersion.Type.UNOBFUSCATED;
                        case APRIL_FOOLS -> versionType == RemoteVersion.Type.SNAPSHOT
                                && GameVersionNumber.asGameVersion(it.getGameVersion()).isAprilFools();
                        case OLD -> versionType == RemoteVersion.Type.OLD;
                        // case ALL,
                        default -> true;
                    };
                });

            String nameQuery = nameField.getText();
            if (!StringUtils.isBlank(nameQuery)) {
                if (nameQuery.startsWith("regex:")) {
                    try {
                        Pattern pattern = Pattern.compile(nameQuery.substring("regex:".length()));
                        versions = versions.filter(it -> pattern.matcher(it.getSelfVersion()).find());
                    } catch (Throwable e) {
                        LOG.warning("Illegal regular expression: " + nameQuery, e);
                    }
                } else {
                    String lowerQueryString = nameQuery.toLowerCase(Locale.ROOT);
                    versions = versions.filter(it -> it.getSelfVersion().toLowerCase(Locale.ROOT).contains(lowerQueryString));
                }
            }

            //noinspection DataFlowIssue
            list.getItems().setAll(versions.collect(Collectors.toList()));
            list.scrollTo(0);
        }
    }
}
