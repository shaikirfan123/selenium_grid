import com.moandjiezana.toml.Toml;

import java.io.File;

public class TOMLReader {
    public static void main(String[] args) {
        // Specify the path to your TOML file
        String tomlFilePath = "C:\\Users\\SHAIK IRFAN\\IdeaProjects\\powertask\\config1.toml";

        // Parse the TOML file
        Toml toml = new Toml().read(new File("C:\\Users\\SHAIK IRFAN\\IdeaProjects\\powertask\\config1.toml"));

        // Retrieve values from the TOML file
        String host = toml.getString("database.host");
        int port = toml.getLong("database.port").intValue();
        String username = toml.getString("database.username");
        String password = toml.getString("database.password");

        // Print the retrieved values
        System.out.println("Host: " + host);
        System.out.println("Port: " + port);
        System.out.println("Username: " + username);
        System.out.println("Password: " + password);
    }
}
