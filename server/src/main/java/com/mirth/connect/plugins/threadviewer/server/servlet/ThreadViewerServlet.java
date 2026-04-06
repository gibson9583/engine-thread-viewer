package com.mirth.connect.plugins.threadviewer.server.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

import com.mirth.connect.client.core.ClientException;
import com.mirth.connect.server.api.MirthServlet;
import com.mirth.connect.plugins.threadviewer.server.ThreadSnapshotService;
import com.mirth.connect.plugins.threadviewer.shared.ThreadSnapshot;
import com.mirth.connect.plugins.threadviewer.shared.ThreadViewerServletInterface;

/**
 * Server-side servlet implementation.
 *
 * Extends MirthServlet, implements the shared ThreadViewerServletInterface.
 * Mirth auto-discovers this class because it implements the interface
 * registered in plugin.xml as apiProvider type="SERVLET_INTERFACE".
 */
public class ThreadViewerServlet extends MirthServlet implements ThreadViewerServletInterface {

    private static final ThreadSnapshotService service = new ThreadSnapshotService();

    public ThreadViewerServlet(@Context HttpServletRequest request, @Context SecurityContext sc) {
        super(request, sc, PLUGIN_POINT);
    }

    @Override
    public boolean activate() throws ClientException {
        return service.activate();
    }

    @Override
    public void deactivate() throws ClientException {
        service.deactivate();
    }

    @Override
    public ThreadSnapshot getThreadSnapshot() throws ClientException {
        ThreadSnapshot snapshot = service.capture();
        if (snapshot == null) {
            throw new ClientException("Thread monitoring is not active. Call activate first.");
        }
        return snapshot;
    }

    @Override
    public boolean isActive() throws ClientException {
        return service.isActive();
    }
}
