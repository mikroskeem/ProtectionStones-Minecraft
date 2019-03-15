package eu.mikroskeem.ps;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;

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

    public static void sendMessage(CommandSender sender, String path, String def, String... placeholders) {
        if(placeholders.length % 2 != 0) {
            throw new IllegalStateException("Placeholders array key should follow a value");
        }

        String message = getMessage(path, def);
        if(message.isEmpty())
            return;

        Iterator<String> iter = Arrays.asList(placeholders).iterator();
        while(iter.hasNext()) {
            String key = Pattern.quote("{" + iter.next() + "}");
            String value = iter.next();

            message = message.replaceAll(key, value);
        }

        sender.spigot().sendMessage(TextComponent.fromLegacyText(message, ChatColor.WHITE));
    }
}
