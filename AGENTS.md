# 项目指南

## 项目结构

这是一个双语言项目：

- **Java 后端** — MC 启动器本体 (JavaFX + JFoenix + MD3 CSS)，Gradle 多模块
- **Dart/Flutter 前端** (`jboot/`) — 启动器 UI (MD3UI 设计规范)，当前为独立占位工程

### Java 模块 (Gradle)

| 模块                                   | 目录                                                     | 说明                                 |
| ------------------------------------ | ------------------------------------------------------ | ---------------------------------- |
| `JVM-MCL`                            | `JMCL/`                                                | 主模块：UI (JavaFX)、启动逻辑，shadow jar 打包 |
| `JVM-MCLCore`                        | `JMCLCore/`                                            | 核心库：Mod 管理、下载、任务系统                 |
| `JVM-MCLBoot`                        | `JMCLBoot/`                                            | 引导模块 (target Java 8)，版本检测、自修复      |
| `JVM-MCLTransformerDiscoveryService` | `minecraft/libraries/JMCLTransformerDiscoveryService/` | Minecraft 库                        |
| `JVM-MCLMultiMCBootstrap`            | `minecraft/libraries/JMCLMultiMCBootstrap/`            | Minecraft 库                        |

启动链：`JMCLBoot/Main.main()` → `JMCL/EntryPoint.main()` → `JMCL/Launcher.main()` (extends `javafx.application.Application`)

## 构建命令

### Java

```shell
# 标准构建 (shadow jar 输出到 JMCL/build/libs/)
./gradlew -g .gradle-user-home clean build

# 仅生成可执行文件
./gradlew -g .gradle-user-home clean makeExecutables

# 直接运行
./gradlew -g .gradle-user-home run

# 跳过测试快速构建 (测试很慢)
./gradlew -g .gradle-user-home clean build -x test
```

如果遇到 Gradle daemon `posix_spawn` 问题，使用 `build-ultimate.sh` (它会设置 `_JAVA_OPTIONS=-Djdk.lang.Process.launchMechanism=FORK`)。

### Flutter (jboot/)

```shell
cd jboot
flutter pub get
flutter run
flutter build apk / ios / macos / linux / windows
```

## 代码风格

- `@NotNullByDefault` 标注每个类；所有可能为 null 的地方必须用 `@Nullable` 标注
- 不可变集合/数组用 `@Unmodifiable` / `@UnmodifiableView` 标注
- 文档使用 `///` Markdown 风格 Javadoc，每个类/字段/方法都必须有文档
- UI 遵循 MD3 (Material Design 3) 规范，CSS 定义在 `JMCL/src/main/resources/assets/css/`

## Gradle 要点

- 总是用 `-g .gradle-user-home` (或环境变量 `GRADLE_USER_HOME=`)
- 测试框架：JUnit 5 (Jupiter Platform)
- `gradle.properties` 中的 `java.home` 指向 `/Users/cangcang/Documents/jdk21`
- 自定义 buildSrc 任务在 `buildSrc/src/main/java/org/Open_code_Studio/jmcl/gradle/`

