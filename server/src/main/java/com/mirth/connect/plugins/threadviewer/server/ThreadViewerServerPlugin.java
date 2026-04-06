package com.mirth.connect.plugins.threadviewer.server;

import java.util.Properties;

import com.mirth.connect.model.ExtensionPermission;
import com.mirth.connect.plugins.ServicePlugin;
import com.mirth.connect.plugins.threadviewer.shared.ThreadViewerServletInterface;

public class ThreadViewerServerPlugin implements ServicePlugin {

    @Override
    public String getPluginPointName() {
        return ThreadViewerServletInterface.PLUGIN_POINT;
    }

    @Override
    public void init(Properties properties) {}

    @Override
    public void start() {}

    @Override
    public void stop() {}

    @Override
    public void update(Properties properties) {}

    @Override
    public Properties getDefaultProperties() {
        return new Properties();
    }

    @Override
    public ExtensionPermission[] getExtensionPermissions() {
        return new ExtensionPermission[] {
            new ExtensionPermission(
                ThreadViewerServletInterface.PLUGIN_POINT,
                ThreadViewerServletInterface.PERMISSION_VIEW,
                "View active JVM threads and stack traces",
                new String[] { "ADMIN" }, null)
        };
    }
}
