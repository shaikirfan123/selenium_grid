package org.example;

import com.jcraft.jsch.*;
import com.moandjiezana.toml.Toml;

import java.io.*;
import java.util.Vector;
import java.util.ArrayList;
import java.util.List;

public class SshFileTransfer2 {

    public static void main(String[] args) {
        String tomlFilePath = "C:\\Users\\SHAIK IRFAN\\IdeaProjects\\powertask\\config1.toml";

        // Parse the TOML file
        Toml toml = new Toml().read(new File("C:\\Users\\SHAIK IRFAN\\IdeaProjects\\powertask\\config1.toml"));

        // Retrieve values from the TOML file
        String host = toml.getString("database.host");
        int port = toml.getLong("database.port").intValue();
        String user = toml.getString("database.username");
        String password = toml.getString("database.password");

        String localFilePath = "C:\\Users\\SHAIK IRFAN\\Downloads\\selenium-server-4.13.0.jar";
        String remoteFilePath = "/home/gaian/";
        String[] hubCommands = {"java -jar selenium-server-4.13.0.jar hub"};
        String[] nodeCommands = {"java -jar selenium-server-4.13.0.jar node --detect-drivers true"};

        // Create SSH sessions for the Hub and Node
        Session tempsession = createSession(host, user, password);
        Session hubSession = createSession(host, user, password);
        Session nodeSession = createSession(host, user, password);

        // Create threads to start the Hub and Node simultaneously
        Thread tempthread = new Thread(() -> {
            try {
                List<String> tempCommands = new ArrayList<>();
                tempCommands.add("pkill -TERM -f terminal");
               // tempCommands.add("command2");
              //  tempCommands.add("command3");

                checkAndExecuteCommands(tempsession, remoteFilePath, tempCommands);
            } catch (JSchException e) {
                throw new RuntimeException(e);
            } catch (SftpException | IOException e) {
                throw new RuntimeException(e);
            }
        });

        Thread hubThread = new Thread(() -> startHub(hubSession, localFilePath, remoteFilePath, hubCommands));
        Thread nodeThread = new Thread(() -> startNode(nodeSession, localFilePath, remoteFilePath, nodeCommands));

        // Start the threads
        tempthread.start();
        hubThread.start();
        nodeThread.start();

        // Wait for both threads to finish
        try {
            tempthread.join();
            hubThread.join();
            nodeThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Disconnect sessions when done
        hubSession.disconnect();
        nodeSession.disconnect();
        tempsession.disconnect();
    }

    private static Session createSession(String host, String user, String password) {
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(user, host, 22);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            return session;
        } catch (JSchException e) {
            e.printStackTrace();
            return null;
        }

    }

    private static void checkAndExecuteCommands(Session session, String remoteFilePath, List<String> commands) throws JSchException, SftpException, IOException {
        Channel channel = session.openChannel("sftp");
        channel.connect();

        ChannelSftp channelSftp = (ChannelSftp) channel;

        // List files in the remote directory
        @SuppressWarnings("unchecked")
        Vector<ChannelSftp.LsEntry> files = channelSftp.ls(remoteFilePath);

        for (ChannelSftp.LsEntry entry : files) {
            String filename = entry.getFilename();

            // Check if the file is a temporary file (e.g., has "temp" or "tmp" in its name)
            if (filename.contains("temp") || filename.contains("tmp")) {
                String filePath = remoteFilePath + filename;
                channelSftp.rm(filePath); // Delete the temporary file
                System.out.println("Deleted temporary file: " + filePath);
            }
        }

        channel.disconnect();

        // Execute additional commands in the remote system
        for (String command : commands) {
            Channel execChannel = session.openChannel("exec");
            ((ChannelExec) execChannel).setCommand(command);

            InputStream in = execChannel.getInputStream();
            execChannel.connect();

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            execChannel.disconnect();
        }
    }

    private static void startHub(Session hubSession, String localFilePath, String remoteFilePath, String[] hubCommands) {
        if (hubSession != null) {
            try {
                // Transfer JAR file from local to remote for Hub
                transferFile(hubSession, localFilePath, remoteFilePath);

                // Start Hub
                executeCommands(hubSession, hubCommands);

                System.out.print("Hub started successfully.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("Failed to create Hub SSH session.");
        }
    }

    private static void startNode(Session nodeSession, String localFilePath, String remoteFilePath, String[] nodeCommands) {
        if (nodeSession != null) {
            try {
                // Transfer JAR file from local to remote for Node
                transferFile(nodeSession, localFilePath, remoteFilePath);

                // Start Node
                executeCommands(nodeSession, nodeCommands);

                System.out.print("Node started successfully.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("Failed to create Node SSH session.");
        }
    }

    private static void transferFile(Session session, String localFilePath, String remoteFilePath) throws JSchException, SftpException {
        Channel channel = session.openChannel("sftp");
        channel.connect();

        ChannelSftp channelSftp = (ChannelSftp) channel;
        channelSftp.put(localFilePath, remoteFilePath);

        channel.disconnect();
    }

    private static void executeCommands(Session session, String[] commands) throws JSchException, IOException {
        for (String command : commands) {
            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);

            InputStream in = channel.getInputStream();
            channel.connect();

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            channel.disconnect();
        }
    }
}
