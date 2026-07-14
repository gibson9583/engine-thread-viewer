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
        // operationNames must be the @MirthOperation names from
        // ThreadViewerServletInterface — an authorization plugin (e.g. RBAC)
        // keys enforcement on them, so a placeholder here leaves every
        // operation unmapped and therefore permitted to any authenticated
        // user. The task name gates the web UI's dashboard tab through the
        // extension task-permission merge.
        return new ExtensionPermission[] {
            new ExtensionPermission(
                ThreadViewerServletInterface.PLUGIN_POINT,
                ThreadViewerServletInterface.PERMISSION_VIEW,
                "View active JVM threads and stack traces",
                new String[] { "activateMonitoring", "deactivateMonitoring",
                        "getThreadSnapshot", "isMonitoringActive" },
                new String[] { "doShowThreadViewer" })
        };
    }
}
