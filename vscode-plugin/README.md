# LuaUnity for VS Code

LuaUnity receives Unity type metadata on a local TCP port and writes Lua annotation files into the current workspace.

## Usage

1. Open a Unity Lua workspace in VS Code or Cursor.
2. Run `LuaUnity: Start Type Server`, or keep `luaUnity.autoStartServer` enabled.
3. Send Unity type metadata to the configured port (`luaUnity.port`, default `996`).
4. Generated annotations are written to `.luaunity/annotations`.

When `luaUnity.updateLuaWorkspaceLibrary` is enabled, the extension also adds the generated annotation directory to `Lua.workspace.library` in workspace settings.
