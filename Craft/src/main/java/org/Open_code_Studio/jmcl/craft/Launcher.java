/// The entry point of Craft Launcher.
///
/// This is the JavaFX Application class that initializes the main window
/// with a classic sidebar layout and delegates game management to JVM-MCLCore.
package org.Open_code_Studio.jmcl.craft;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Craft Launcher — a JMCL variant with a classic sidebar UI.
 */
public final class Launcher extends Application {

    private static final Path DEFAULT_WORK_DIR = Paths.get(
            System.getProperty("jvmmcl.dir",
                    System.getProperty("user.home") + "/.jvm-mcl"));

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Determine working directory
        primaryStage.setTitle("Craft Launcher");

        CraftRootPage rootPage = new CraftRootPage(primaryStage, DEFAULT_WORK_DIR);
        Scene scene = new Scene(rootPage.getRoot(), 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/assets/css/craft.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(640);
        primaryStage.show();
    }

    /// Main entry point.
    public static void main(String[] args) {
        launch(args);
    }
}