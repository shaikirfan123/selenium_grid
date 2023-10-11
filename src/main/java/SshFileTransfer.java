    import com.jcraft.jsch.*;
    import com.moandjiezana.toml.Toml;

    import java.io.*;

    public class SshFileTransfer {

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
            Session hubSession = createSession(host, user, password);
            Session nodeSession = createSession(host, user, password);

            // Start Hub and Node in separate sessions
            startHubAndNode(hubSession, nodeSession, localFilePath, remoteFilePath, hubCommands, nodeCommands);

            // Disconnect sessions when done
            hubSession.disconnect();
            nodeSession.disconnect();
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

        private static void startHubAndNode(Session hubSession, Session nodeSession, String localFilePath, String remoteFilePath, String[] hubCommands, String[] nodeCommands) {
            if (hubSession != null && nodeSession != null) {
                try {
                    // Transfer JAR file from local to remote for both Hub and Node
                    transferFile(hubSession, localFilePath, remoteFilePath);
                    transferFile(nodeSession, localFilePath, remoteFilePath);

                    // Start Hub
                    executeCommands(hubSession, hubCommands);

                    // Start Node
                    executeCommands(nodeSession, nodeCommands);

                    System.out.print("Hub and Node started successfully.");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                System.err.println("Failed to create SSH sessions.");
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
