package org.Open_code_Studio.jmcl.ui.construct;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.Open_code_Studio.jmcl.ui.FXUtils;
import org.Open_code_Studio.jmcl.setting.StyleSheets;
import org.Open_code_Studio.jmcl.util.logging.Logger;

import static org.Open_code_Studio.jmcl.util.i18n.I18n.i18n;

public final class TaskLogDialog extends Stage {

    private final TextArea logArea;
    private final Timeline pollTimer;
    private String lastLogContent = "";

    public TaskLogDialog() {
        setTitle(i18n("log.viewer"));
        initModality(Modality.NONE);

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setStyle("-fx-font-family: 'Menlo', 'Monaco', 'Courier New', monospace; -fx-font-size: 11px;");

        BorderPane root = new BorderPane(logArea);
        root.setPadding(new Insets(8));
        Scene scene = new Scene(root, 700, 500);
        StyleSheets.init(scene);
        setScene(scene);

        pollTimer = new Timeline(new KeyFrame(Duration.millis(500), e -> pollLogs()));
        pollTimer.setCycleCount(Timeline.INDEFINITE);
    }

    public void startPolling() {
        pollTimer.play();
    }

    public void stopPolling() {
        pollTimer.stop();
    }

    private void pollLogs() {
        String currentLogs = Logger.LOG.getLogs();
        if (!currentLogs.equals(lastLogContent)) {
            lastLogContent = currentLogs;
            logArea.setText(currentLogs);
            logArea.appendText("");
        }
    }

    @Override
    public void close() {
        stopPolling();
        super.close();
    }
}