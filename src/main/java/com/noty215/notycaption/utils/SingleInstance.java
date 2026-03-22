package com.noty215.notycaption.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Single instance check using socket binding
 */
public class SingleInstance {

    private static final Logger logger = LoggerFactory.getLogger(SingleInstance.class);
    private static final int PORT = 65432;

    private static ServerSocket serverSocket;
    private static boolean alreadyRunning = false;

    static {
        try {
            serverSocket = new ServerSocket(PORT);
            logger.info("Single instance lock acquired successfully");

            // Start a thread to accept connections (just to keep the socket open)
            new Thread(() -> {
                while (!serverSocket.isClosed()) {
                    try {
                        Socket socket = serverSocket.accept();
                        socket.close();
                    } catch (IOException e) {
                        // Socket closed, exit
                        break;
                    }
                }
            }).start();

        } catch (IOException e) {
            alreadyRunning = true;
            logger.warn("Another instance is already running: {}", e.getMessage());
        }
    }

    public static boolean isAlreadyRunning() {
        return alreadyRunning;
    }

    public static void release() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                logger.info("Single instance lock released");
            } catch (IOException e) {
                logger.warn("Failed to close socket: {}", e.getMessage());
            }
        }
    }
}