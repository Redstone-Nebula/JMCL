package org.Open_code_Studio.jmcl.ui.construct;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.Open_code_Studio.jmcl.ui.SVG;
import org.Open_code_Studio.jmcl.ui.SVGContainer;
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
        initStyle(StageStyle.UNDECORATED);

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setStyle("-fx-font-family: 'Menlo', 'Monaco', 'Courier New', monospace; -fx-font-size: 11px;");

        // --- MD3 Title Bar ---
        Label titleLabel = new Label(i18n("log.viewer"));
        titleLabel.getStyleClass().add("task-log-title");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        SVGContainer closeIcon = SVG.CLOSE.createIcon(18);
        closeIcon.getStyleClass().add("task-log-close-icon");

        StackPane closeButton = new StackPane(closeIcon);
        closeButton.getStyleClass().add("task-log-close-button");
        closeButton.setCursor(Cursor.HAND);
        closeButton.setOnMouseClicked(e -> close());

        HBox titleBar = new HBox(titleLabel, closeButton);
        titleBar.getStyleClass().add("task-log-title-bar");
        titleBar.setAlignment(Pos.CENTER_LEFT);

        // Window dragging via title bar
        final double[] dragOffset = new double[2];
        titleBar.setOnMousePressed(e -> {
            dragOffset[0] = e.getSceneX();
            dragOffset[1] = e.getSceneY();
        });
        titleBar.setOnMouseDragged(e -> {
            setX(e.getScreenX() - dragOffset[0]);
            setY(e.getScreenY() - dragOffset[1]);
        });

        // --- Content ---
        BorderPane contentPane = new BorderPane();
        contentPane.setTop(titleBar);
        contentPane.setCenter(logArea);
        contentPane.getStyleClass().add("task-log-window");

        Rectangle clip = new Rectangle();
        clip.setArcWidth(12);
        clip.setArcHeight(12);
        clip.widthProperty().bind(contentPane.widthProperty());
        clip.heightProperty().bind(contentPane.heightProperty());
        contentPane.setClip(clip);

        Scene scene = new Scene(contentPane, 700, 500);
        scene.setFill(null);
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