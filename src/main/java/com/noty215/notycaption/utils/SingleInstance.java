package com.noty215.notycaption.utils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

public class SingleInstance {
    private static final int PORT = 65432;
    private ServerSocket serverSocket;
    private boolean alreadyExists;
    private static final Logger logger = Logger.getLogger(SingleInstance.class.getName());

    public SingleInstance() {
        try {
            serverSocket = new ServerSocket(PORT, 50);
            serverSocket.setReuseAddress(true);
            logger.info("Single instance lock acquired successfully");
            alreadyExists = false;

            new Thread(this::listen).start();

        } catch (IOException e) {
            alreadyExists = true;
            logger.warning("Single instance check failed: " + e.getMessage() +
                    " - Another instance may be running");
        }
    }

    private void listen() {
        while (serverSocket != null && !serverSocket.isClosed()) {
            try (Socket client = serverSocket.accept()) {
                client.getOutputStream().write("ALREADY_RUNNING".getBytes());
            } catch (IOException e) {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    logger.warning("Error in single instance listener: " + e.getMessage());
                }
            }
        }
    }

    public boolean isAlreadyRunning() {
        return alreadyExists;
    }

    public void release() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                logger.info("Single instance lock released");
            } catch (IOException e) {
                logger.warning("Failed to close server socket: " + e.getMessage());
            }
        }
    }
}