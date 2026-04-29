import * as fs from 'fs/promises';
import * as path from 'path';
import * as vscode from 'vscode';
import { LuaAnnotationGenerator } from './LuaAnnotationGenerator';
import { UnityProtocolServer } from './UnityProtocolServer';

let server: UnityProtocolServer | undefined;
let statusBarItem: vscode.StatusBarItem | undefined;

export function activate(context: vscode.ExtensionContext): void {
    statusBarItem = vscode.window.createStatusBarItem(vscode.StatusBarAlignment.Left, 100);
    statusBarItem.command = 'luaUnity.startServer';
    context.subscriptions.push(statusBarItem);

    context.subscriptions.push(
        vscode.commands.registerCommand('luaUnity.startServer', () => startServer()),
        vscode.commands.registerCommand('luaUnity.stopServer', () => stopServer()),
        vscode.commands.registerCommand('luaUnity.openAnnotations', () => openAnnotations()),
        vscode.commands.registerCommand('luaUnity.clearAnnotations', () => clearAnnotations()),
        vscode.commands.registerCommand('luaUnity.addAnnotationsToLuaWorkspace', () => addAnnotationsToLuaWorkspace())
    );

    if (getConfig().get<boolean>('autoStartServer', true)) {
        startServer();
    } else {
        updateStatus(false);
    }
}

export function deactivate(): void {
    stopServer();
}

async function startServer(): Promise<void> {
    const workspaceRoot = getWorkspaceRoot();
    if (!workspaceRoot) {
        vscode.window.showWarningMessage('LuaUnity: open a workspace before starting the type server.');
        updateStatus(false);
        return;
    }

    if (!server) {
        server = new UnityProtocolServer();
        server.on('listening', port => {
            updateStatus(true);
            vscode.window.showInformationMessage(`LuaUnity type server is listening on 127.0.0.1:${port}.`);
        });
        server.on('stopped', () => updateStatus(false));
        server.on('error', error => vscode.window.showErrorMessage(`LuaUnity: ${error.message}`));
        server.on('library', async classes => {
            try {
                const outputDirectory = getOutputDirectory(workspaceRoot);
                const count = await new LuaAnnotationGenerator().generate(classes, outputDirectory);
                if (getConfig().get<boolean>('updateLuaWorkspaceLibrary', true)) {
                    await addAnnotationsToLuaWorkspace(outputDirectory);
                }
                vscode.window.showInformationMessage(`LuaUnity generated ${count} annotation files for ${classes.length} classes.`);
            } catch (error) {
                vscode.window.showErrorMessage(`LuaUnity failed to generate annotations: ${formatError(error)}`);
            }
        });
    }

    if (!server.isRunning()) {
        server.start(getConfig().get<number>('port', 996));
    }
}

function stopServer(): void {
    server?.stop();
    server = undefined;
    updateStatus(false);
}

async function openAnnotations(): Promise<void> {
    const workspaceRoot = getWorkspaceRoot();
    if (!workspaceRoot) {
        vscode.window.showWarningMessage('LuaUnity: open a workspace first.');
        return;
    }

    const outputDirectory = getOutputDirectory(workspaceRoot);
    await fs.mkdir(outputDirectory, { recursive: true });
    await vscode.commands.executeCommand('revealFileInOS', vscode.Uri.file(outputDirectory));
}

async function clearAnnotations(): Promise<void> {
    const workspaceRoot = getWorkspaceRoot();
    if (!workspaceRoot) {
        vscode.window.showWarningMessage('LuaUnity: open a workspace first.');
        return;
    }

    await fs.rm(getOutputDirectory(workspaceRoot), { recursive: true, force: true });
    vscode.window.showInformationMessage('LuaUnity generated annotations were cleared.');
}

async function addAnnotationsToLuaWorkspace(explicitOutputDirectory?: string): Promise<void> {
    const workspaceRoot = getWorkspaceRoot();
    if (!workspaceRoot) {
        vscode.window.showWarningMessage('LuaUnity: open a workspace first.');
        return;
    }

    const outputDirectory = explicitOutputDirectory ?? getOutputDirectory(workspaceRoot);
    const luaConfig = vscode.workspace.getConfiguration('Lua');
    const current = luaConfig.get<unknown>('workspace.library');
    const normalized = normalizeLibrarySetting(current);

    if (!normalized.includes(outputDirectory)) {
        normalized.push(outputDirectory);
        await luaConfig.update('workspace.library', normalized, vscode.ConfigurationTarget.Workspace);
    }
}

function normalizeLibrarySetting(value: unknown): string[] {
    if (Array.isArray(value)) {
        return value.filter((item): item is string => typeof item === 'string');
    }

    if (value && typeof value === 'object') {
        return Object.entries(value)
            .filter(([, enabled]) => enabled)
            .map(([libraryPath]) => libraryPath);
    }

    return [];
}

function updateStatus(running: boolean): void {
    if (!statusBarItem) {
        return;
    }

    if (running) {
        statusBarItem.text = 'LuaUnity: Listening';
        statusBarItem.tooltip = 'LuaUnity type server is running. Click to restart.';
        statusBarItem.command = 'luaUnity.startServer';
    } else {
        statusBarItem.text = 'LuaUnity: Stopped';
        statusBarItem.tooltip = 'Click to start the LuaUnity type server.';
        statusBarItem.command = 'luaUnity.startServer';
    }
    statusBarItem.show();
}

function getConfig(): vscode.WorkspaceConfiguration {
    return vscode.workspace.getConfiguration('luaUnity');
}

function getWorkspaceRoot(): string | undefined {
    return vscode.workspace.workspaceFolders?.[0]?.uri.fsPath;
}

function getOutputDirectory(workspaceRoot: string): string {
    const configured = getConfig().get<string>('outputDirectory', '.luaunity/annotations');
    return path.isAbsolute(configured) ? configured : path.join(workspaceRoot, configured);
}

function formatError(error: unknown): string {
    return error instanceof Error ? error.message : String(error);
}
