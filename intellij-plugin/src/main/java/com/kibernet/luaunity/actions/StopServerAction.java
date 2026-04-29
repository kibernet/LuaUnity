package com.kibernet.luaunity.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.kibernet.luaunity.service.LuaUnityProjectService;
import org.jetbrains.annotations.NotNull;

public final class StopServerAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project != null) {
            LuaUnityProjectService.getInstance(project).stop();
        }
    }
}
