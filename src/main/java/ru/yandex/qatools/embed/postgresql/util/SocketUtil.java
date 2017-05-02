package ru.yandex.qatools.embed.postgresql.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;

public final class SocketUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(SocketUtil.class);

    private SocketUtil() {
    }


    /**
     * Returns a free port number on localhost.
     * <p/>
     * Heavily inspired from org.eclipse.jdt.launching.SocketUtil (to avoid a dependency to JDT just because of this).
     * Slightly improved with close() missing in JDT. And throws exception instead of returning -1.
     *
     * @return a free port number on localhost
     * @throws IllegalStateException if unable to find a free port
     */
    public static int findFreePort() {
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(0);
            socket.setReuseAddress(true);
            int port = socket.getLocalPort();
            closeQuietly(socket);
            return port;
        } catch (IOException e) {
            LOGGER.trace("Failed to open socket", e);
        } finally {
            if (socket != null) {
                closeQuietly(socket);
            }
        }
        throw new IllegalStateException("Could not find a free TCP/IP port to start embedded Jetty HTTP Server on");
    }

    private static void closeQuietly(ServerSocket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            LOGGER.trace("Failed to close socket", e);
        }
    }
}
