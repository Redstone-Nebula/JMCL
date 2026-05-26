package org.Open_code_Studio.jmcl.ui.main;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.util.Duration;
import org.Open_code_Studio.jmcl.setting.ConfigHolder;
import org.Open_code_Studio.jmcl.ui.FXUtils;
import org.Open_code_Studio.jmcl.ui.construct.ComponentList;
import org.Open_code_Studio.jmcl.ui.construct.LineButton;
import org.Open_code_Studio.jmcl.ui.construct.LineToggleButton;
import org.Open_code_Studio.jmcl.util.platform.Architecture;
import org.Open_code_Studio.jmcl.util.platform.OperatingSystem;
import org.Open_code_Studio.jmcl.util.platform.SystemInfo;
import org.Open_code_Studio.jmcl.util.platform.hardware.CentralProcessor;
import org.Open_code_Studio.jmcl.util.platform.hardware.GraphicsCard;
import org.Open_code_Studio.jmcl.util.platform.hardware.PhysicalMemoryStatus;

import java.lang.management.ManagementFactory;
import java.util.List;

import static org.Open_code_Studio.jmcl.util.i18n.I18n.i18n;

public class DeveloperOptionsPage extends ScrollPane {

    private final Timeline timeline;

    public DeveloperOptionsPage() {
        setFitToWidth(true);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        setContent(content);
        FXUtils.smoothScrolling(this);

        // Logging Section
        {
            ComponentList logSection = new ComponentList();

            {
                LineToggleButton showInstallationLog = new LineToggleButton();
                showInstallationLog.setTitle(i18n("settings.developer.show_installation_log"));
                showInstallationLog.setSubtitle(i18n("settings.developer.show_installation_log.subtitle"));
                showInstallationLog.selectedProperty().bindBidirectional(ConfigHolder.globalConfig().showInstallationLogProperty());
                logSection.getContent().add(showInstallationLog);
            }

            content.getChildren().addAll(
                    ComponentList.createComponentListTitle(i18n("settings.developer.logging")),
                    logSection
            );
        }

        // System Information Section
        {
            ComponentList systemSection = new ComponentList();

            LineButton osInfo = new LineButton();
            osInfo.setTitle(i18n("settings.developer.os"));
            osInfo.setSubtitle(OperatingSystem.SYSTEM_NAME + " " + OperatingSystem.SYSTEM_VERSION.getVersion() + " (" + Architecture.SYSTEM_ARCH + ")");
            systemSection.getContent().add(osInfo);

            LineButton javaInfo = new LineButton();
            javaInfo.setTitle(i18n("settings.developer.java_version"));
            javaInfo.setSubtitle(System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
            systemSection.getContent().add(javaInfo);

            LineButton jvmMemory = new LineButton();
            jvmMemory.setTitle(i18n("settings.developer.jvm_max_memory"));
            jvmMemory.setSubtitle(Runtime.getRuntime().maxMemory() / 1024 / 1024 + " MiB");
            systemSection.getContent().add(jvmMemory);

            content.getChildren().addAll(
                    ComponentList.createComponentListTitle(i18n("settings.developer.system_info")),
                    systemSection
            );
        }

        // Display Information Section
        {
            ComponentList displaySection = new ComponentList();

            LineButton screenInfo = new LineButton();
            screenInfo.setTitle(i18n("settings.developer.screen_resolution"));
            Screen primaryScreen = Screen.getPrimary();
            screenInfo.setSubtitle(
                    (int) primaryScreen.getBounds().getWidth() + "x" +
                            (int) primaryScreen.getBounds().getHeight() +
                            " @ " + (int) primaryScreen.getOutputScaleX() + "x (" +
                            (int) primaryScreen.getDpi() + " dpi)"
            );
            displaySection.getContent().add(screenInfo);

            LineButton refreshInfo = new LineButton();
            refreshInfo.setTitle(i18n("settings.developer.refresh_rate"));
            int refreshRate = getScreenRefreshRate();
            refreshInfo.setSubtitle(refreshRate > 0 ? refreshRate + " Hz" : i18n("unknown"));
            displaySection.getContent().add(refreshInfo);

            content.getChildren().addAll(
                    ComponentList.createComponentListTitle(i18n("settings.developer.display_info")),
                    displaySection
            );
        }

        // Hardware Information Section
        {
            ComponentList hardwareSection = new ComponentList();

            LineButton cpuInfo = new LineButton();
            cpuInfo.setTitle(i18n("settings.developer.cpu"));
            CentralProcessor cpu = SystemInfo.getCentralProcessor();
            cpuInfo.setSubtitle(cpu != null ? cpu.toString() : i18n("unknown"));
            hardwareSection.getContent().add(cpuInfo);

            LineButton gpuInfo = new LineButton();
            gpuInfo.setTitle(i18n("settings.developer.gpu"));
            List<GraphicsCard> gpus = SystemInfo.getGraphicsCards();
            if (gpus != null && !gpus.isEmpty()) {
                gpuInfo.setSubtitle(gpus.get(0).toString());
            } else {
                gpuInfo.setSubtitle(i18n("unknown"));
            }
            hardwareSection.getContent().add(gpuInfo);

            LineButton memoryInfo = new LineButton();
            memoryInfo.setTitle(i18n("settings.developer.memory"));
            hardwareSection.getContent().add(memoryInfo);

            LineButton cpuUsageInfo = new LineButton();
            cpuUsageInfo.setTitle(i18n("settings.developer.cpu_usage"));
            hardwareSection.getContent().add(cpuUsageInfo);

            content.getChildren().addAll(
                    ComponentList.createComponentListTitle(i18n("settings.developer.hardware_info")),
                    hardwareSection
            );

            // Real-time updates every 2 seconds
            timeline = new Timeline(
                    new KeyFrame(Duration.seconds(2), e -> {
                        updateMemorySubtitle(memoryInfo);
                        updateCpuUsageSubtitle(cpuUsageInfo);
                    })
            );
            timeline.setCycleCount(Timeline.INDEFINITE);
            timeline.play();
        }
    }

    private void updateMemorySubtitle(LineButton memoryInfo) {
        PhysicalMemoryStatus memory = SystemInfo.getPhysicalMemoryStatus();
        if (memory.hasAvailable()) {
            long totalMB = memory.getTotal() / 1024 / 1024;
            long usedMB = memory.getUsed() / 1024 / 1024;
            int pct = (int) (((double) memory.getUsed() / memory.getTotal()) * 100);
            memoryInfo.setSubtitle(usedMB + " MB / " + totalMB + " MB (" + pct + "%)");
        } else {
            memoryInfo.setSubtitle(i18n("unknown"));
        }
    }

    private void updateCpuUsageSubtitle(LineButton cpuUsageInfo) {
        try {
            if (ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean bean) {
                double load = bean.getSystemCpuLoad();
                if (load >= 0) {
                    cpuUsageInfo.setSubtitle(String.format("%.1f%%", load * 100));
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
        cpuUsageInfo.setSubtitle(i18n("unknown"));
    }

    private static int getScreenRefreshRate() {
        try {
            java.awt.GraphicsEnvironment ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
            java.awt.GraphicsDevice gd = ge.getDefaultScreenDevice();
            return gd.getDisplayMode().getRefreshRate();
        } catch (Throwable e) {
            return -1;
        }
    }
}