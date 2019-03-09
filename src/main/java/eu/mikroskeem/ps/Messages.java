package eu.mikroskeem.ps;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * @author Mark Vainomaa
 */
public final class Messages {
    private static File messagesFile;
    private static YamlConfiguration configuration;

    public static void initialize(JavaPlugin plugin) {
        Messages.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if(!Messages.messagesFile.exists()) {
            try(InputStream resource = plugin.getResource("messages.yml")) {
                Files.copy(resource, Messages.messagesFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create messages.yml", e);
            }
        }
        reload();
    }

    public static void reload() {
        configuration = YamlConfiguration.loadConfiguration(Messages.messagesFile);
    }

    public static String getMessage(String path, String def) {
        String message = configuration.getString(path, def);
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
