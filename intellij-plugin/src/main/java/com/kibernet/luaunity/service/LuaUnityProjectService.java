package com.kibernet.luaunity.service;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.kibernet.luaunity.generation.LuaAnnotationGenerator;
import com.kibernet.luaunity.model.LuaUnityClass;
import com.kibernet.luaunity.protocol.LuaUnityProtocolParser;
import com.kibernet.luaunity.settings.LuaUnitySettings;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service(Service.Level.PROJECT)
public final class LuaUnityProjectService implements Disposable {
    private final Project project;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ServerSocket serverSocket;
    private Thread serverThread;

    public LuaUnityProjectService(Project project) {
        this.project = project;
    }

    public static LuaUnityProjectService getInstance(Project project) {
        return project.getService(LuaUnityProjectService.class);
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        int port = LuaUnitySettings.getInstance().getPort();
        serverThread = new Thread(() -> runServer(port), "LuaUnity Type Server");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    public void stop() {
        running.set(false);
        closeServerSocket();
    }

    public boolean isRunning() {
        return running.get();
    }

    @Override
    public void dispose() {
        stop();
    }

    private void runServer(int port) {
        try (ServerSocket socket = new ServerSocket()) {
            serverSocket = socket;
            socket.bind(new InetSocketAddress("127.0.0.1", port));
            notify("LuaUnity type server is listening on 127.0.0.1:" + port + ".", NotificationType.INFORMATION);

            while (running.get() && !project.isDisposed()) {
                try (Socket client = socket.accept()) {
                    processClient(client);
                } catch (IOException error) {
                    if (running.get()) {
                        notify("LuaUnity client connection closed: " + error.getMessage(), NotificationType.WARNING);
                    }
                }
            }
        } catch (SocketException ignored) {
            // Expected when the service is stopped.
        } catch (IOException error) {
            notify("LuaUnity type server failed: " + error.getMessage(), NotificationType.ERROR);
        } finally {
            running.set(false);
            serverSocket = null;
        }
    }

    private void processClient(Socket client) throws IOException {
        DataInputStream input = new DataInputStream(client.getInputStream());
        while (running.get() && !project.isDisposed()) {
            int packetSize = readLittleEndianInt(input);
            int protocol = readLittleEndianInt(input);
            if (packetSize < 8) {
                throw new IOException("Invalid packet size: " + packetSize);
            }

            byte[] payload = input.readNBytes(packetSize - 8);
            if (payload.length != packetSize - 8) {
                return;
            }
            if (protocol == 0) {
                handleLibrary(payload);
            }
        }
    }

    private void handleLibrary(byte[] payload) {
        try {
            List<LuaUnityClass> classes = LuaUnityProtocolParser.parseLibrary(payload);
            Path outputDirectory = resolveOutputDirectory();
            int generatedFiles = new LuaAnnotationGenerator().generate(classes, outputDirectory);
            LocalFileSystem.getInstance().refreshAndFindFileByNioFile(outputDirectory);
            notify("Generated " + generatedFiles + " Lua annotation files for " + classes.size() + " classes.", NotificationType.INFORMATION);
        } catch (Exception error) {
            notify("Failed to generate Lua annotations: " + error.getMessage(), NotificationType.ERROR);
        }
    }

    private Path resolveOutputDirectory() {
        String configured = LuaUnitySettings.getInstance().getOutputDirectory();
        Path path = Path.of(configured);
        if (path.isAbsolute()) {
            return path;
        }

        String basePath = project.getBasePath();
        if (basePath == null) {
            return Path.of(System.getProperty("user.home")).resolve(configured);
        }
        return Path.of(basePath).resolve(configured);
    }

    private int readLittleEndianInt(DataInputStream input) throws IOException {
        byte[] bytes = input.readNBytes(4);
        if (bytes.length != 4) {
            throw new IOException("Connection closed.");
        }
        return (bytes[0] & 0xff)
                | ((bytes[1] & 0xff) << 8)
                | ((bytes[2] & 0xff) << 16)
                | ((bytes[3] & 0xff) << 24);
    }

    private void closeServerSocket() {
        ServerSocket socket = serverSocket;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void notify(String content, NotificationType type) {
        ApplicationManager.getApplication().invokeLater(() ->
                NotificationGroupManager.getInstance()
                        .getNotificationGroup("LuaUnity")
                        .createNotification(content, type)
                        .notify(project)
        );
    }
}
