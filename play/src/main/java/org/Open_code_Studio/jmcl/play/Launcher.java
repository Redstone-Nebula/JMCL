/// The entry point of Play Launcher.
///
/// A minimal, LauncherX-style launcher focusing on the core action: pick a version and play.
package org.Open_code_Studio.jmcl.play;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Play Launcher — a minimal JMCL variant similar to LauncherX.
 */
public final class Launcher extends Application {

    private static final Path DEFAULT_WORK_DIR = Paths.get(
            System.getProperty("jvmmcl.dir",
                    System.getProperty("user.home") + "/.jvm-mcl"));

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Play Launcher");

        PlayRootPage rootPage = new PlayRootPage(primaryStage, DEFAULT_WORK_DIR);
        Scene scene = new Scene(rootPage.getRoot(), 480, 640);
        scene.getStylesheets().add(getClass().getResource("/assets/css/play.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setMinWidth(360);
        primaryStage.setMinHeight(480);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    /// Main entry point.
    public static void main(String[] args) {
        launch(args);
    }
}