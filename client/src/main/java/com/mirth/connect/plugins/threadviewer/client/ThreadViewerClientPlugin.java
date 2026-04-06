package com.mirth.connect.plugins.threadviewer.client;

import java.util.List;

import javax.swing.JComponent;

import com.mirth.connect.client.core.ClientException;
import com.mirth.connect.model.DashboardStatus;
import com.mirth.connect.plugins.DashboardTabPlugin;
import com.mirth.connect.plugins.threadviewer.shared.ThreadViewerServletInterface;

public class ThreadViewerClientPlugin extends DashboardTabPlugin {

    private ThreadViewerPanel panel;

    public ThreadViewerClientPlugin(String name) {
        super(name);
    }

    @Override
    public String getPluginPointName() {
        return ThreadViewerServletInterface.PLUGIN_POINT;
    }

    @Override
    public JComponent getTabComponent() {
        if (panel == null) {
            panel = new ThreadViewerPanel();
        }
        return panel;
    }

    @Override
    public void start() {
        // User controls monitoring via the Start/Stop button.
    }

    @Override
    public void stop() {
        if (panel != null) {
            panel.deactivateMonitoring();
        }
    }

    @Override
    public void reset() {
        if (panel != null) {
            panel.deactivateMonitoring();
            panel.reset();
        }
    }

    @Override
    public void update() {
        // No-op — thread data is fetched on-demand by the panel.
    }

    @Override
    public void update(List<DashboardStatus> statuses) {
        // No-op — thread data is fetched on-demand by the panel.
    }

    @Override
    public void prepareData() throws ClientException {
        // No pre-fetch — everything is on-demand.
    }

    @Override
    public void prepareData(List<DashboardStatus> statuses) throws ClientException {
        // No pre-fetch — everything is on-demand.
    }
}
