# Java 代码风格要求

这些规则适用于在此代码库中编写或修改的所有 Java 代码。

## 可空性

- 用 JetBrains 注解 `@NotNullByDefault` 标注每个类。
- 任何可能为 `null` 的类型、字段、参数、返回值、局部变量或泛型类型参数都必须明确标注 `@Nullable`。
- Nullability must never be implicit.

## 不可变性

- 不可变数组和集合必须根据情况明确使用 JetBrains 注解 `@Unmodifiable` 或 `@UnmodifiableView` 标注。
- 对于数组，请使用类型使用语法，例如 `String @Unmodifiable []`。

## 文档

- 每个类、字段和方法都必须有文档。
- 文档必须使用 `///` Markdown 风格的 Javadoc 注释。
- 保持文档的准确性，并具体说明实际的行为、限制和副作用。
- 在复杂逻辑中添加简明的实现注释，只有在它们能够显著提高可读性或解释非显而易见的行为时才添加。

## Gradle

- 在本仓库中调用 Gradle 时，总是将 `GRADLE_USER_HOME` 设置为工作区本地的 `.gradle-user-home` 目录。
- 优先使用如 `./gradlew -g .gradle-user-home ...` 的命令，或等效的基于环境变量的配置。
- 运行 Gradle 的 `test` 任务时，使用更高的十分钟超时时间。

