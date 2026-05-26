package org.Open_code_Studio.jmcl.ui.skin.animation;

import javafx.util.Duration;
import org.Open_code_Studio.jmcl.ui.skin.FunctionHelper;
import org.Open_code_Studio.jmcl.ui.skin.SkinAnimation;
import org.Open_code_Studio.jmcl.ui.skin.SkinCanvas;
import org.Open_code_Studio.jmcl.ui.skin.SkinTransition;

public final class SkinAniRunning extends SkinAnimation {

    private SkinTransition larmTransition, rarmTransition;

    public SkinAniRunning(int weight, int time, double angle, SkinCanvas canvas) {
        larmTransition = new SkinTransition(Duration.millis(time),
                v -> v * (larmTransition.getCount() % 4 < 2 ? 1 : -1) * angle,
                canvas.larm.getXRotate().angleProperty(), canvas.rleg.getXRotate().angleProperty());

        rarmTransition = new SkinTransition(Duration.millis(time),
                v -> v * (rarmTransition.getCount() % 4 < 2 ? 1 : -1) * -angle,
                canvas.rarm.getXRotate().angleProperty(), canvas.lleg.getXRotate().angleProperty());

        FunctionHelper.alwaysB(SkinTransition::setAutoReverse, true, larmTransition, rarmTransition);
        FunctionHelper.alwaysB(SkinTransition::setCycleCount, 16, larmTransition, rarmTransition);
        FunctionHelper.always(transitions::add, larmTransition, rarmTransition);
        this.weight = weight;
        init();
    }

}
