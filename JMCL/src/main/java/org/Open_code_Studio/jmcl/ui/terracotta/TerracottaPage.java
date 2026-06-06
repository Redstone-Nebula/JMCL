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
package org.Open_code_Studio.jmcl.ui.terracotta;

import com.jfoenix.controls.JFXPopup;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.Open_code_Studio.jmcl.setting.Accounts;
import org.Open_code_Studio.jmcl.setting.Profile;
import org.Open_code_Studio.jmcl.setting.Profiles;
import org.Open_code_Studio.jmcl.terracotta.TerracottaMetadata;
import org.Open_code_Studio.jmcl.ui.Controllers;
import org.Open_code_Studio.jmcl.ui.FXUtils;
import org.Open_code_Studio.jmcl.ui.SVG;
import org.Open_code_Studio.jmcl.ui.account.AccountAdvancedListItem;
import org.Open_code_Studio.jmcl.ui.account.AccountListPopupMenu;
import org.Open_code_Studio.jmcl.ui.animation.TransitionPane;
import org.Open_code_Studio.jmcl.ui.construct.*;
import org.Open_code_Studio.jmcl.ui.decorator.DecoratorAnimatedPage;
import org.Open_code_Studio.jmcl.ui.decorator.DecoratorPage;
import org.Open_code_Studio.jmcl.ui.main.MainPage;
import org.Open_code_Studio.jmcl.ui.versions.GameListPopupMenu;
import org.Open_code_Studio.jmcl.ui.versions.Versions;
import org.Open_code_Studio.jmcl.util.Lang;
import org.Open_code_Studio.jmcl.util.StringUtils;

import static org.Open_code_Studio.jmcl.setting.ConfigHolder.globalConfig;
import static org.Open_code_Studio.jmcl.util.i18n.I18n.i18n;

public class TerracottaPage extends DecoratorAnimatedPage implements DecoratorPage, PageAware {
    private static final int TERRACOTTA_AGREEMENT_VERSION = 2;

    private final ReadOnlyObjectWrapper<State> state = new ReadOnlyObjectWrapper<>(State.fromTitle(i18n("terracotta.terracotta")));
    private final TabHeader tab;
    private final TabHeader.Tab<TerracottaControllerPage> statusPage = new TabHeader.Tab<>("statusPage");
    private final TransitionPane transitionPane = new TransitionPane();

    @SuppressWarnings("unused")
    private ChangeListener<String> instanceChangeListenerHolder;

    public TerracottaPage() {
        statusPage.setNodeSupplier(TerracottaControllerPage::new);
        tab = new TabHeader(transitionPane, statusPage);
        tab.select(statusPage);

        BorderPane left = new BorderPane();
        FXUtils.setLimitWidth(left, 200);
        VBox.setVgrow(left, Priority.ALWAYS);
        setLeft(left);

        AdvancedListBox sideBar = new AdvancedListBox()
                .addNavigationDrawerTab(tab, statusPage, i18n("terracotta.status"), SVG.TUNE);
        left.setTop(sideBar);

        AccountAdvancedListItem accountListItem = new AccountAdvancedListItem();
        accountListItem.setOnAction(e -> Controllers.navigate(Controllers.getAccountListPage()));
        accountListItem.accountProperty().bind(Accounts.selectedAccountProperty());
        FXUtils.onSecondaryButtonClicked(accountListItem, () -> AccountListPopupMenu.show(accountListItem, JFXPopup.PopupVPosition.BOTTOM, JFXPopup.PopupHPosition.LEFT, accountListItem.getWidth(), 0));

        AdvancedListBox toolbar = new AdvancedListBox()
                .add(accountListItem)
                .addNavigationDrawerItem(i18n("version.launch"), SVG.ROCKET_LAUNCH, () -> {
                    Profile profile = Profiles.getSelectedProfile();
                    Versions.launch(profile, profile.getSelectedVersion(), launcherHelper -> {
                        launcherHelper.setKeep();
                        launcherHelper.setDisableOfflineSkin();
                    });
                }, item -> {
                    instanceChangeListenerHolder = FXUtils.onWeakChangeAndOperate(Profiles.selectedVersionProperty(),
                            instanceName -> item.setSubtitle(StringUtils.isNotBlank(instanceName) ? instanceName : i18n("version.empty"))
                    );

                    MainPage mainPage = Controllers.getRootPage().getMainPage();
                    FXUtils.onScroll(item, mainPage.getVersions(), list -> {
                        String currentId = mainPage.getCurrentGame();
                        return Lang.indexWhere(list, instance -> instance.getId().equals(currentId));
                    }, it -> mainPage.getProfile().setSelectedVersion(it.getId()));

                    FXUtils.onSecondaryButtonClicked(item, () -> GameListPopupMenu.show(item,
                            JFXPopup.PopupVPosition.BOTTOM,
                            JFXPopup.PopupHPosition.LEFT,
                            item.getWidth(),
                            0,
                            mainPage.getProfile(), mainPage.getVersions()));
                })
                .addNavigationDrawerItem(i18n("terracotta.feedback.title"), SVG.FEEDBACK, () -> FXUtils.openLink(TerracottaMetadata.FEEDBACK_LINK));
        BorderPane.setMargin(toolbar, new Insets(0, 0, 12, 0));
        left.setBottom(toolbar);

        setCenter(transitionPane);
    }

    @Override
    public void onPageShown() {
        tab.onPageShown();

        if (globalConfig().getTerracottaAgreementVersion() < TERRACOTTA_AGREEMENT_VERSION) {
            Controllers.confirmWithCountdown(i18n("terracotta.confirm.desc"), i18n("terracotta.confirm.title"), 5, MessageDialogPane.MessageType.INFO, () -> {
                globalConfig().setTerracottaAgreementVersion(TERRACOTTA_AGREEMENT_VERSION);
            }, () -> fireEvent(new PageCloseEvent()));
        }
    }

    @Override
    public void onPageHidden() {
        tab.onPageHidden();
    }

    @Override
    public ReadOnlyObjectProperty<State> stateProperty() {
        return state.getReadOnlyProperty();
    }
}
