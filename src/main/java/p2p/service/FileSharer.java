package p2p.service;

import p2p.utils.UploadUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class FileSharer {

    private final Map<Integer, String> availableFiles;

    public FileSharer() {
        this.availableFiles = new HashMap<>();
    }

    public int offerFile(String filePath) {
        int port;
        while(true) {
            port = UploadUtils.generatePort();
            if(!availableFiles.containsKey(port)) {
                availableFiles.put(port, filePath);
                break;
            }
        }
        return port;
    }

    public void startFileSharing(int port) {
        if(!availableFiles.containsKey(port)) {
            // Raise exception
            String message = String.format("No available files on port: %d", port);
            System.out.println(message);
        }

        String filePath = availableFiles.get(port);

        Thread newWorkerThread = new Thread(new FileSharingJob(port, filePath));
        newWorkerThread.start();
    }

    private static class FileSharingJob implements Runnable {
        private final int port;
        private final File file;

        private FileSharingJob(int port, String filePath) {
            this.port = port;
            this.file = new File(filePath);
        }

        @Override
        public void run() {
            try(ServerSocket serverSocket = new ServerSocket(port)) {
                // Blocking call
                Socket clientSocket = serverSocket.accept();
                System.out.printf("Accepted connection from client: %s%n", clientSocket.getInetAddress());

                try (FileInputStream fileInputStream = new FileInputStream(file);
                     OutputStream outputStream = clientSocket.getOutputStream();
                ) {
                    String fileName = file.getName();
                    String header = String.format("FileName: %s%n", fileName);

                    outputStream.write(header.getBytes());

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while((bytesRead = fileInputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }

                    outputStream.flush();
                }
            } catch (IOException exception) {
                // Raise Exception
                System.out.println(exception.getMessage());
            }
        }
    }
}
