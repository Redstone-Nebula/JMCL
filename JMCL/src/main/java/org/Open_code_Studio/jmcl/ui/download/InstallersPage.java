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

import org.Open_code_Studio.jmcl.download.DownloadProvider;
import org.Open_code_Studio.jmcl.download.LibraryAnalyzer;
import org.Open_code_Studio.jmcl.download.RemoteVersion;
import org.Open_code_Studio.jmcl.game.JMCLGameRepository;
import org.Open_code_Studio.jmcl.ui.Controllers;
import org.Open_code_Studio.jmcl.ui.InstallerItem;
import org.Open_code_Studio.jmcl.ui.construct.MessageDialogPane;
import org.Open_code_Studio.jmcl.ui.wizard.WizardController;
import org.Open_code_Studio.jmcl.util.SettingsMap;
import org.Open_code_Studio.jmcl.util.i18n.I18n;

import static javafx.beans.binding.Bindings.createBooleanBinding;
import static org.Open_code_Studio.jmcl.util.i18n.I18n.i18n;

public class InstallersPage extends AbstractInstallersPage {

    private boolean isNameModifiedByUser = false;

    public InstallersPage(WizardController controller, JMCLGameRepository repository, String gameVersion, DownloadProvider downloadProvider) {
        super(controller, gameVersion, downloadProvider);

        installable.bind(createBooleanBinding(() -> !txtName.getText().isBlank(), txtName.textProperty()));

        txtName.textProperty().addListener((obs, oldText, newText) -> isNameModifiedByUser = true);
    }

    @Override
    public String getTitle() {
        return ((RemoteVersion) controller.getSettings().get("game")).getGameVersion();
    }

    private String getVersion(String id) {
        return I18n.getDisplayVersion((RemoteVersion) controller.getSettings().get(id));
    }

    protected void reload() {
        for (InstallerItem library : group.getLibraries()) {
            String libraryId = library.getLibraryId();
            if (controller.getSettings().containsKey(libraryId)) {
                library.versionProperty().set(new InstallerItem.InstalledState(getVersion(libraryId), false, false));
            } else {
                library.versionProperty().set(null);
            }
        }
        if (!isNameModifiedByUser) {
            setTxtNameWithLoaders();
        }
    }

    @Override
    public void cleanup(SettingsMap settings) {
    }

    private static boolean checkName(String name) {
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!(c >= '0' && c <= '9')
                    && !(c >= 'a' && c <= 'z')
                    && !(c >= 'A' && c <= 'Z')
                    && c != '-' && c != '_' && c != '.'
            )
                return false;
        }

        return true;
    }

    protected void onInstall() {
        String name = txtName.getText();

        if (!checkName(name)) {
            Controllers.dialog(new MessageDialogPane.Builder(
                    i18n("install.name.invalid"),
                    i18n("message.warning"),
                    MessageDialogPane.MessageType.QUESTION)
                    .yesOrNo(() -> {
                        controller.getSettings().put("name", name);
                        controller.onFinish();
                    }, () -> {
                        // The user selects Cancel and does nothing.
                    })
                    .build());
        } else {
            controller.getSettings().put("name", name);
            controller.onFinish();
        }
    }

    private void setTxtNameWithLoaders() {
        StringBuilder nameBuilder = new StringBuilder(getTitle());

        for (InstallerItem library : group.getLibraries()) {
            String libraryId = library.getLibraryId().replace(LibraryAnalyzer.LibraryType.MINECRAFT.getPatchId(), "");
            if (!controller.getSettings().containsKey(libraryId)) {
                continue;
            }

            LibraryAnalyzer.LibraryType libraryType = LibraryAnalyzer.LibraryType.fromPatchId(libraryId);

            if (libraryType != null) {
                String loaderName = switch (libraryType) {
                    case FORGE -> "Forge";
                    case NEO_FORGE -> "NeoForge";
                    case CLEANROOM -> "Cleanroom";
                    case LEGACY_FABRIC -> "LegacyFabric";
                    case FABRIC -> "Fabric";
                    case LITELOADER -> "LiteLoader";
                    case QUILT -> "Quilt";
                    case OPTIFINE -> "OptiFine";
                    default -> null;
                };

                if (loaderName != null)
                    nameBuilder.append('-').append(loaderName);
            }
        }

        txtName.setText(nameBuilder.toString());
        isNameModifiedByUser = false;
    }
}
