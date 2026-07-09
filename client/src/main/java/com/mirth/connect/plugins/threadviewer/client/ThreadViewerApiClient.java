package com.mirth.connect.plugins.threadviewer.client;

import com.mirth.connect.client.ui.PlatformUI;
import com.mirth.connect.plugins.threadviewer.shared.ThreadSnapshot;
import com.mirth.connect.plugins.threadviewer.shared.ThreadViewerServletInterface;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Client-side API calls for the Thread Viewer.
 *
 * ──────────────────────────────────────────────────────────────
 *  TARGET OIE VERSION: 4.6.0
 *
 *  Uses the servlet pattern introduced in 3.4.0:
 *    mirthClient.getServlet(ThreadViewerServletInterface.class)
 *
 *  When upgrading, verify:
 *    - PlatformUI.MIRTH_FRAME.mirthClient  (Client reference)
 *    - Client.getServlet()                 (servlet dispatch)
 * ──────────────────────────────────────────────────────────────
 */
public final class ThreadViewerApiClient {

    private static final Logger logger = LogManager.getLogger(ThreadViewerApiClient.class);

    private ThreadViewerApiClient() {}

    private static ThreadViewerServletInterface getServlet() {
        return PlatformUI.MIRTH_FRAME.mirthClient
                .getServlet(ThreadViewerServletInterface.class);
    }

    public static void activate() throws Exception {
        getServlet().activate();
    }

    public static void deactivate() {
        try {
            getServlet().deactivate();
        } catch (Exception e) {
            logger.debug("Deactivate failed (expected if disconnecting): {}", e.getMessage());
        }
    }

    public static ThreadSnapshot fetchSnapshot() throws Exception {
        return getServlet().getThreadSnapshot();
    }

    public static boolean isServerActive() {
        try {
            return getServlet().isActive();
        } catch (Exception e) {
            return false;
        }
    }
}
