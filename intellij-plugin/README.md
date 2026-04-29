# LuaUnity IntelliJ 插件

本目录是 LuaUnity 的 JetBrains 平台插件源码，包名为 `com.kibernet.luaunity`。

## 功能

- 启动本地 Unity 类型服务器，默认监听 `127.0.0.1:996`。
- 接收类型元数据并生成 Lua 注解。
- 在 `Settings | Tools | LuaUnity` 中配置端口、输出目录和自动启动。
- 提供手动启动/停止服务器的 IDE action。

## 构建

在仓库根目录运行：

```bat
build.bat
```

也可以在本目录运行 Gradle 的 `buildPlugin` 任务。
