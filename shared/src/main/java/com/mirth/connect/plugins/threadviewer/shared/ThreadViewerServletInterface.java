package com.mirth.connect.plugins.threadviewer.shared;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.mirth.connect.client.core.ClientException;
import com.mirth.connect.client.core.Operation.ExecuteType;
import com.mirth.connect.client.core.api.BaseServletInterface;
import com.mirth.connect.client.core.api.MirthOperation;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * Shared servlet interface for the Thread Viewer plugin.
 *
 * Follows the OIE 4.6.0 plugin pattern established in 3.4.0:
 *   - Extends BaseServletInterface
 *   - Uses Swagger 2 annotations (@Api, @ApiOperation)
 *   - XML and JSON media types (XStream serialization) — the Swing client
 *     negotiates XML; the OIE Web Administrator negotiates JSON
 *   - MirthOperation for permission control
 *
 * Client calls: mirthClient.getServlet(ThreadViewerServletInterface.class).method()
 * Server implements: ThreadViewerServlet extends MirthServlet implements this
 * Web client calls: GET/POST /api/extensions/threadviewer/* (see package/webadmin)
 */
@Path("/extensions/threadviewer")
@Api("Extension Services")
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public interface ThreadViewerServletInterface extends BaseServletInterface {

    public static final String PLUGIN_POINT = "Thread Viewer";
    public static final String PERMISSION_VIEW = "View Thread Viewer";

    @POST
    @Path("/activate")
    @ApiOperation("Activates JVM thread monitoring (enables CPU time and contention tracking).")
    @MirthOperation(name = "activateMonitoring", display = "Activate Thread Monitoring",
            permission = PERMISSION_VIEW, type = ExecuteType.ASYNC)
    public boolean activate() throws ClientException;

    @POST
    @Path("/deactivate")
    @ApiOperation("Deactivates thread monitoring and disables contention tracking.")
    @MirthOperation(name = "deactivateMonitoring", display = "Deactivate Thread Monitoring",
            permission = PERMISSION_VIEW, type = ExecuteType.ASYNC)
    public void deactivate() throws ClientException;

    @GET
    @Path("/threads")
    @ApiOperation("Retrieves a complete snapshot of all JVM threads.")
    @MirthOperation(name = "getThreadSnapshot", display = "Get Thread Snapshot",
            permission = PERMISSION_VIEW, type = ExecuteType.ASYNC, auditable = false)
    public ThreadSnapshot getThreadSnapshot() throws ClientException;

    @GET
    @Path("/active")
    @ApiOperation("Returns whether thread monitoring is currently active.")
    @MirthOperation(name = "isMonitoringActive", display = "Check Monitoring Status",
            permission = PERMISSION_VIEW, type = ExecuteType.ASYNC, auditable = false)
    public boolean isActive() throws ClientException;
}
