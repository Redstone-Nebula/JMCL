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
package org.Open_code_Studio.jmcl.ui.main;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import org.Open_code_Studio.jmcl.setting.Profile;
import org.Open_code_Studio.jmcl.setting.Profiles;
import org.Open_code_Studio.jmcl.ui.FXUtils;
import org.Open_code_Studio.jmcl.ui.SVG;
import org.Open_code_Studio.jmcl.ui.animation.TransitionPane;
import org.Open_code_Studio.jmcl.ui.construct.*;
import org.Open_code_Studio.jmcl.ui.decorator.DecoratorAnimatedPage;
import org.Open_code_Studio.jmcl.ui.decorator.DecoratorPage;
import org.Open_code_Studio.jmcl.ui.versions.VersionSettingsPage;

import java.util.Locale;

import static org.Open_code_Studio.jmcl.util.i18n.I18n.i18n;

public class LauncherSettingsPage extends DecoratorAnimatedPage implements DecoratorPage, PageAware {
    private final ReadOnlyObjectWrapper<State> state = new ReadOnlyObjectWrapper<>(State.fromTitle(i18n("settings")));
    private final TabHeader tab;
    private final TabHeader.Tab<VersionSettingsPage> gameTab = new TabHeader.Tab<>("versionSettingsPage");
    private final TabControl.Tab<JavaManagementPage> javaManagementTab = new TabControl.Tab<>("javaManagementPage");
    private final TabHeader.Tab<SettingsPage> settingsTab = new TabHeader.Tab<>("settingsPage");
    private final TabHeader.Tab<PersonalizationPage> personalizationTab = new TabHeader.Tab<>("personalizationPage");
    private final TabHeader.Tab<DownloadSettingsPage> downloadTab = new TabHeader.Tab<>("downloadSettingsPage");
    private final TabHeader.Tab<DeveloperOptionsPage> developerTab = new TabHeader.Tab<>("developerOptionsPage");
    private final TabHeader.Tab<AboutPage> aboutTab = new TabHeader.Tab<>("aboutPage");
    private final TabHeader.Tab<FeedbackPage> feedbackTab = new TabHeader.Tab<>("feedbackPage");
    private final TransitionPane transitionPane = new TransitionPane();
    private ClassTitle developerCategory;
    private AdvancedListItem developerNavItem;

    public LauncherSettingsPage() {
        gameTab.setNodeSupplier(() -> new VersionSettingsPage(true));
        javaManagementTab.setNodeSupplier(JavaManagementPage::new);
        settingsTab.setNodeSupplier(SettingsPage::new);
        personalizationTab.setNodeSupplier(PersonalizationPage::new);
        downloadTab.setNodeSupplier(DownloadSettingsPage::new);
        developerTab.setNodeSupplier(DeveloperOptionsPage::new);
        feedbackTab.setNodeSupplier(FeedbackPage::new);
        aboutTab.setNodeSupplier(AboutPage::new);
        tab = new TabHeader(transitionPane, gameTab, javaManagementTab, settingsTab, personalizationTab, downloadTab, developerTab, feedbackTab, aboutTab);

        tab.select(gameTab);
        addEventHandler(Navigator.NavigationEvent.NAVIGATED, event -> gameTab.getNode().loadVersion(Profiles.getSelectedProfile(), null));

        AdvancedListBox sideBar = new AdvancedListBox()
                .addNavigationDrawerTab(tab, gameTab, i18n("settings.type.global.manage"), SVG.STADIA_CONTROLLER, SVG.STADIA_CONTROLLER_FILL)
                .addNavigationDrawerTab(tab, javaManagementTab, i18n("java.management"), SVG.LOCAL_CAFE, SVG.LOCAL_CAFE_FILL)
                .startCategory(i18n("launcher").toUpperCase(Locale.ROOT))
                .addNavigationDrawerTab(tab, settingsTab, i18n("settings.launcher.general"), SVG.TUNE)
                .addNavigationDrawerTab(tab, personalizationTab, i18n("settings.launcher.appearance"), SVG.STYLE, SVG.STYLE_FILL)
                .addNavigationDrawerTab(tab, downloadTab, i18n("download"), SVG.DOWNLOAD)
                .addNavigationDrawerTab(tab, feedbackTab, i18n("contact"), SVG.FEEDBACK, SVG.FEEDBACK_FILL)
                .addNavigationDrawerTab(tab, aboutTab, i18n("about"), SVG.INFO, SVG.INFO_FILL);

        developerCategory = new ClassTitle(i18n("settings.developer").toUpperCase(Locale.ROOT));
        developerCategory.setVisible(false);
        developerCategory.setManaged(false);

        AdvancedListItem item = new AdvancedListItem();
        item.getStyleClass().add("navigation-drawer-item");
        item.setTitle(i18n("settings.developer"));
        item.setLeftIcon(SVG.TUNE);
        item.activeProperty().bind(tab.getSelectionModel().selectedItemProperty().isEqualTo(developerTab));
        item.setOnAction(e -> tab.select(developerTab));
        item.setVisible(false);
        item.setManaged(false);
        developerNavItem = item;

        sideBar.add(6, developerCategory);
        sideBar.add(7, item);

        FXUtils.setLimitWidth(sideBar, 200);
        setLeft(sideBar);

        setCenter(transitionPane);
    }

    @Override
    public void onPageShown() {
        tab.onPageShown();
    }

    @Override
    public void onPageHidden() {
        tab.onPageHidden();
    }

    public void showGameSettings(Profile profile) {
        gameTab.getNode().loadVersion(profile, null);
        tab.select(gameTab, false);
    }

    public void showFeedback() {
        tab.select(feedbackTab, false);
    }

    public void showDeveloperOptions() {
        tab.select(developerTab, false);
    }

    public void unlockDeveloperOptions() {
        developerCategory.setVisible(true);
        developerCategory.setManaged(true);
        developerNavItem.setVisible(true);
        developerNavItem.setManaged(true);
        tab.select(developerTab, false);
    }

    @Override
    public ReadOnlyObjectProperty<State> stateProperty() {
        return state.getReadOnlyProperty();
    }
}
