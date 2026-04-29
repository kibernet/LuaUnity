package com.kibernet.luaunity.startup;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.kibernet.luaunity.service.LuaUnityProjectService;
import com.kibernet.luaunity.settings.LuaUnitySettings;
import org.jetbrains.annotations.NotNull;

public final class LuaUnityStartupActivity implements StartupActivity, DumbAware {
    @Override
    public void runActivity(@NotNull Project project) {
        if (LuaUnitySettings.getInstance().isAutoStartServer()) {
            LuaUnityProjectService.getInstance(project).start();
        }
    }
}
