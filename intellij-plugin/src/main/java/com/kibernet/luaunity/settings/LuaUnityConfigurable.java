package com.kibernet.luaunity.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

public final class LuaUnityConfigurable implements Configurable {
    private JBTextField portField;
    private JBTextField outputDirectoryField;
    private JBCheckBox autoStartServerBox;
    private JPanel panel;

    @Override
    public @Nls String getDisplayName() {
        return "LuaUnity";
    }

    @Override
    public @Nullable JComponent createComponent() {
        LuaUnitySettings settings = LuaUnitySettings.getInstance();
        portField = new JBTextField(String.valueOf(settings.getPort()), 12);
        outputDirectoryField = new JBTextField(settings.getOutputDirectory(), 30);
        autoStartServerBox = new JBCheckBox("Start the local type server when a project opens", settings.isAutoStartServer());

        panel = new JPanel(new GridBagLayout());
        panel.setBorder(JBUI.Borders.empty(12));

        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = 0;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = JBUI.insets(0, 0, 8, 8);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = 0;
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = JBUI.insets(0, 0, 8, 0);

        panel.add(new JBLabel("Server port:"), labelConstraints);
        panel.add(portField, fieldConstraints);

        labelConstraints.gridy++;
        fieldConstraints.gridy++;
        panel.add(new JBLabel("Output directory:"), labelConstraints);
        panel.add(outputDirectoryField, fieldConstraints);

        GridBagConstraints checkConstraints = new GridBagConstraints();
        checkConstraints.gridx = 0;
        checkConstraints.gridy = 2;
        checkConstraints.gridwidth = 2;
        checkConstraints.anchor = GridBagConstraints.WEST;
        panel.add(autoStartServerBox, checkConstraints);

        return panel;
    }

    @Override
    public boolean isModified() {
        LuaUnitySettings settings = LuaUnitySettings.getInstance();
        return parsePort() != settings.getPort()
                || !outputDirectoryField.getText().trim().equals(settings.getOutputDirectory())
                || autoStartServerBox.isSelected() != settings.isAutoStartServer();
    }

    @Override
    public void apply() {
        LuaUnitySettings settings = LuaUnitySettings.getInstance();
        settings.setPort(parsePort());
        settings.setOutputDirectory(outputDirectoryField.getText().trim());
        settings.setAutoStartServer(autoStartServerBox.isSelected());
    }

    @Override
    public void reset() {
        LuaUnitySettings settings = LuaUnitySettings.getInstance();
        portField.setText(String.valueOf(settings.getPort()));
        outputDirectoryField.setText(settings.getOutputDirectory());
        autoStartServerBox.setSelected(settings.isAutoStartServer());
    }

    @Override
    public void disposeUIResources() {
        panel = null;
        portField = null;
        outputDirectoryField = null;
        autoStartServerBox = null;
    }

    private int parsePort() {
        try {
            return Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ignored) {
            return 996;
        }
    }
}
