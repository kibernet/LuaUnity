# LuaUnity 插件

LuaUnity 是一个面向 Unity + Lua 工作流的双端 IDE 插件工程，用于把 Unity/C# 类型信息暴露给 Lua 编辑环境，提供类、字段、属性、方法等类型提示能力。

本仓库同时维护两套插件源码：

- **IntelliJ 插件 (`intellij-plugin`)**: 面向 JetBrains 平台，监听 Unity 类型协议并生成 Lua 注解文件。
- **VS Code 插件 (`vscode-plugin`)**: 面向 VS Code、Cursor 等编辑器，监听 Unity 类型协议并生成 Lua 注解文件。

## 功能

- 接收 Unity 端导出的 C# 类型元数据。
- 生成 Lua 注解，包含 `---@class`、`---@field`、`---@param`、`---@return` 和重载提示。
- VS Code/Cursor 端可自动把生成目录加入 Lua Language Server 的 `Lua.workspace.library`。
- 默认使用本地类型协议端口 `996`。
- 提供 IntelliJ 与 VS Code/Cursor 两套插件工程，方便后续统一维护。

## 工程结构

```text
LuaUnity/
├── build.bat
├── LICENSE
├── README.md
├── intellij-plugin/
│   ├── build.gradle.kts
│   └── src/main/
└── vscode-plugin/
    ├── package.json
    ├── tsconfig.json
    └── src/
```

## 构建

在 Windows 下直接运行：

```bat
build.bat
```

构建脚本会依次处理：

1. `intellij-plugin`: 优先使用 `gradlew.bat`，没有 wrapper 时回退到系统 `gradle`。如果本机没有 Gradle，会跳过 IntelliJ 构建并给出提示。
2. `vscode-plugin`: 使用 `npm install`/`npm ci` 安装依赖，执行 TypeScript 编译并打包 `.vsix`。

环境要求：

- JDK 17 或更高版本。
- Node.js LTS 和 npm。
- 如使用便携 Node，可设置 `LUAUNITY_NODE_HOME`，或放到 `.tools\node`。

## IntelliJ 插件说明

IntelliJ 插件默认监听 `127.0.0.1:996`。收到 Unity 类型数据后，会在当前项目生成 `.luaunity/annotations` 注解目录。可在 `Settings | Tools | LuaUnity` 中调整端口、输出目录和自动启动选项。

## VS Code 插件说明

VS Code 插件默认监听 `127.0.0.1:996`。收到 Unity 类型数据后，会在当前工作区生成：

```text
.luaunity/annotations
```

可用命令：

- `LuaUnity: Start Type Server`
- `LuaUnity: Stop Type Server`
- `LuaUnity: Open Generated Annotations`
- `LuaUnity: Clear Generated Annotations`
- `LuaUnity: Add Annotations To Lua Workspace Library`

## 授权

本工程使用 [Apache License 2.0](./LICENSE)。
