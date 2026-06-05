# JMCL macOS 使用技巧

在 macOS 上使用 JMCL（Java Minecraft Launcher）可能会遇到一些特有的问题。本文档汇总了常见问题及其解决方案。

---

## 目录

1. [posix_spawn failed 错误](#1-posix_spawn-failed-错误)
2. [自动权限修复](#2-自动权限修复)
3. [GitHub API 限流](#3-github-api-限流)
4. [无障碍权限（实例管理器）](#4-无障碍权限实例管理器)
5. [已知兼容性问题](#5-已知兼容性问题)
6. [macOS 专属功能](#6-macos-专属功能)

---

## 1. posix_spawn failed 错误

### 现象

启动游戏时出现如下错误：

```
java.io.IOException: posix_spawn failed
```

或退出码 `134`（SIGABRT）/ `143`（SIGTERM）。

### 原因

macOS 上 Java 默认使用 `posix_spawn` 系统调用来创建子进程。当工作目录路径包含空格或受安全策略限制时，`posix_spawn` 可能失败。

### 解决方案

#### 方案 A：设置环境变量（推荐）

启动前设置 `_JAVA_OPTIONS` 环境变量，强制 Java 使用传统的 `fork()` + `exec()` 方式创建进程：

```bash
_JAVA_OPTIONS="-Djdk.lang.Process.launchMechanism=FORK" java -jar JVM-MCL-*.jar
```

#### 方案 B：使用启动脚本

JMCL 自带的 `.sh` 启动脚本已自动处理此问题：

```bash
./JVM-MCL-DEV2026.2.1.sh
```

#### 方案 C：永久设置（全局生效）

将以下行添加到 `~/.zshrc` 或 `~/.bash_profile`：

```bash
export _JAVA_OPTIONS="-Djdk.lang.Process.launchMechanism=FORK"
```

### 自动处理

JMCL 已在以下位置自动处理此问题：

| 位置 | 作用 |
|------|------|
| `SystemUtils.java` | 命令执行时自动用 `/bin/bash -c` 包装，绕过 `posix_spawn` 限制 |
| `ManagedProcess.java` | 进程创建时自动设置 `_JAVA_OPTIONS` 环境变量 |
| `DefaultLauncher.java` | 游戏启动时向子进程注入 `_JAVA_OPTIONS` |
| `JMCLauncher.sh` | Shell 脚本中自动判断 macOS 并设置环境变量 |

---

## 2. 自动权限修复

### 现象

启动游戏时提示"权限不足"或无法执行 Java 进程。

### 解决方案

JMCL 在 `SystemUtils.java` 中实现了 macOS 专用的权限自动修复逻辑：

1. 在运行任何命令前，检查可执行文件是否有执行权限
2. 如果没有执行权限，自动添加 `chmod +x`
3. 仅在 macOS 上生效，Windows/Linux 不做此操作

### 手动检查

如果自动修复失败，可以手动执行：

```bash
chmod +x /path/to/your/executable
```

---

## 3. GitHub API 限流

### 现象

更新检查时提示：

```
GitHub API rate limit exceeded. Please try again later.
```

### 原因

GitHub API 对未认证请求的限制为 **60 次/小时**，频繁触发更新检查可能耗尽配额。

### 解决方案

1. 创建 GitHub Personal Access Token：
   - 访问 https://github.com/settings/tokens
   - 点击 **Generate new token (classic)**
   - 权限只需勾选 `public_repo`
   - 生成 token 后复制

2. 设置环境变量启动：

```bash
GITHUB_TOKEN="ghp_xxxxx" ./JVM-MCL-DEV2026.2.1.sh
```

3. 或永久添加到 `~/.zshrc`：

```bash
export GITHUB_TOKEN="ghp_xxxxx"
```

设置后限流提升至 **5000 次/小时**。

---

## 4. 无障碍权限（实例管理器）

### 功能

实例管理器窗口可以跟随 Minecraft 游戏窗口移动。此功能需要使用 macOS 的 **System Events** API。

### 权限设置

首次使用时，系统会提示 JMCL 需要**无障碍权限**：

1. 打开 **系统设置 → 隐私与安全性 → 辅助功能**
2. 点击 **+** 添加 JMCL（或使用的 Java 运行环境）
3. 勾选 JMCL 的开关

### 权限不足时的行为

如果没有授予无障碍权限，实例管理器会自动回退到跟随启动器主窗口的行为，不影响正常使用。

---

## 5. 已知兼容性问题

### macOS CPU/GPU 信息获取失败

启动时日志中出现以下警告，属于已知问题，**不影响正常使用**：

```
WARNING: Failed to get macOS CPU/GPU information
```

这是 JMCL 尝试读取 macOS 硬件信息时的兼容性问题，已捕获处理。

---

## 6. macOS 专属功能

### 命令执行包装

JMCL 在 macOS 上使用 `/bin/bash -c` 包装命令执行，确保路径包含空格时不会出错：

| 平台 | 包装方式 |
|------|---------|
| macOS | `/bin/bash -c "command"` |
| Windows | `cmd.exe /c "command"` |
| Linux | 直接执行 |

### 启动参数

| 参数 | 作用 |
|------|------|
| `-Djdk.lang.Process.launchMechanism=FORK` | 强制使用 `fork()` + `exec()` 创建进程 |
| `-XstartOnFirstThread` | JavaFX 在 macOS 上需要此参数（JMCL 自动设置） |
| `-Dglass.gtk.enableWindowDecorations=false` | 禁用窗口装饰（JMCL 使用自定义标题栏） |

---

> 最后更新: 2026-06-03