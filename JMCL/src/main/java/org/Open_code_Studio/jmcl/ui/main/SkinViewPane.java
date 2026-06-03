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

import javafx.beans.InvalidationListener;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import org.Open_code_Studio.jmcl.auth.Account;
import org.Open_code_Studio.jmcl.auth.yggdrasil.TextureModel;
import org.Open_code_Studio.jmcl.game.TexturesLoader;
import org.Open_code_Studio.jmcl.game.TexturesLoader.LoadedTexture;
import org.Open_code_Studio.jmcl.setting.Accounts;
import org.Open_code_Studio.jmcl.ui.skin.SkinCanvas;
import org.Open_code_Studio.jmcl.ui.skin.animation.SkinAniRunning;
import org.Open_code_Studio.jmcl.ui.skin.animation.SkinAniWavingArms;

public class SkinViewPane extends StackPane {
    private final SkinCanvas skinCanvas;

    public SkinViewPane() {
        getStyleClass().add("skin-view-pane");

        skinCanvas = new SkinCanvas(TexturesLoader.getDefaultSkinImage(), 180, 280, true);
        skinCanvas.enableRotation(0.5);

        skinCanvas.getAnimationPlayer().addSkinAnimation(
                new SkinAniWavingArms(100, 2000, 7.5, skinCanvas),
                new SkinAniRunning(100, 100, 30, skinCanvas)
        );

        getChildren().add(skinCanvas);

        InvalidationListener updateSkinListener = obs -> {
            Account selectedAccount = Accounts.selectedAccountProperty().get();
            if (selectedAccount != null) {
                updateSkinForAccount(selectedAccount);
            } else {
                skinCanvas.updateSkin(
                        TexturesLoader.getDefaultSkinImage(),
                        false,
                        null
                );
            }
        };

        Accounts.selectedAccountProperty().addListener(updateSkinListener);
        updateSkinListener.invalidated(null);
    }

    private void updateSkinForAccount(Account account) {
        TexturesLoader.skinBinding(account).addListener((obs, old, val) -> {
            if (val != null) {
                Image skin = val.getImage();
                boolean isSlim = TextureModel.SLIM.modelName.equals(val.getMetadata().get("model"));
                skinCanvas.updateSkin(skin, isSlim, null);
            }
        });
    }
}