package com.kibernet.luaunity.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Service(Service.Level.APP)
@State(name = "LuaUnitySettings", storages = @Storage("luaunity.xml"))
public final class LuaUnitySettings implements PersistentStateComponent<LuaUnitySettings.StateData> {
    private StateData state = new StateData();

    public static LuaUnitySettings getInstance() {
        return ApplicationManager.getApplication().getService(LuaUnitySettings.class);
    }

    @Override
    public @Nullable StateData getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull StateData state) {
        this.state = state;
    }

    public int getPort() {
        return state.port;
    }

    public void setPort(int port) {
        state.port = port;
    }

    public String getOutputDirectory() {
        return state.outputDirectory;
    }

    public void setOutputDirectory(String outputDirectory) {
        state.outputDirectory = outputDirectory;
    }

    public boolean isAutoStartServer() {
        return state.autoStartServer;
    }

    public void setAutoStartServer(boolean autoStartServer) {
        state.autoStartServer = autoStartServer;
    }

    public static final class StateData {
        public int port = 996;
        public String outputDirectory = ".luaunity/annotations";
        public boolean autoStartServer = true;
    }
}
