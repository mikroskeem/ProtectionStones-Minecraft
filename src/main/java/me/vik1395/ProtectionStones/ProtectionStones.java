/*
 * Copyright 2019 ProtectionStones team and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.vik1395.ProtectionStones;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import eu.mikroskeem.ps.Messages;
import me.vik1395.ProtectionStones.commands.*;
import org.bstats.bukkit.Metrics;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProtectionStones extends JavaPlugin {
    public static Map<UUID, String> uuidToName = new HashMap<>();

    public static ProtectionStones plugin;
    public static Plugin wgd;
    public static File psStoneData;
    public static File conf;

    public static Metrics metrics;

    public static FileConfiguration config;
    public static List<String> flags = new ArrayList<>();
    public static List<String> toggleList = new ArrayList<>();
    public static List<String> allowedFlags = new ArrayList<>();
    public static List<String> deniedWorlds = new ArrayList<>();
    public static Collection<String> mats = new HashSet<>();
    public static int priority;
    public Map<CommandSender, Integer> viewTaskList;

    public static StoneTypeData StoneTypeData = new StoneTypeData();

    public static boolean isCooldownEnable = false;
    public static int cooldown = 0;
    public static String cooldownMessage = null;

    public static ProtectionStones getPlugin() {
        return plugin;
    }

    public static RegionManager getRegionManagerWithPlayer(Player p) {
        return WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(p.getWorld()));
    }

    // Turn WG region name into a location (ex. ps138x35y358z i think)
    public static PSLocation parsePSRegionToLocation(String regionName) {
        int psx = Integer.parseInt(regionName.substring(2, regionName.indexOf("x")));
        int psy = Integer.parseInt(regionName.substring(regionName.indexOf("x") + 1, regionName.indexOf("y")));
        int psz = Integer.parseInt(regionName.substring(regionName.indexOf("y") + 1, regionName.length() - 1));
        return new PSLocation(psx, psy, psz);
    }

    // Helper method to either remove, disown or regen a player's ps region
    // NOTE: be sure to save the region manager after
    public static void removeDisownRegenPSRegion(LocalPlayer lp, String arg, String region, RegionManager rgm, Player admin) {
        ProtectedRegion r = rgm.getRegion(region);
        switch (arg) {
            case "disown":
                DefaultDomain owners = r.getOwners();
                owners.removePlayer(lp);
                r.setOwners(owners);
                break;
            case "regen":
                Bukkit.dispatchCommand(admin, "region select " + region);
                Bukkit.dispatchCommand(admin, "/regen");
                rgm.removeRegion(region);
                break;
            case "remove":
                if (region.substring(0, 2).equals("ps")) {
                    PSLocation psl = ProtectionStones.parsePSRegionToLocation(region);
                    Block blockToRemove = admin.getWorld().getBlockAt(psl.x, psl.y, psl.z); //TODO getWorld might not work
                    blockToRemove.setType(Material.AIR);
                }
                rgm.removeRegion(region);
                break;
        }
    }


    @Override
    public void onEnable() {
        viewTaskList = new HashMap<>();
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        plugin = this;
        conf = new File(this.getDataFolder() + "/config.yml");
        psStoneData = new File(this.getDataFolder() + "/hiddenpstones.yml");
        Messages.initialize(this);

        // Metrics (bStats)
        metrics = null;//new Metrics(this);

        // generate protection stones stored blocks file
        if (!psStoneData.exists()) {
            try {
                ProtectionStones.psStoneData.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(ProtectionStones.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        // register event listeners
        getServer().getPluginManager().registerEvents(new ListenerClass(), this);

        // check that WorldGuard and WorldEdit are enabled (Worldguard will only be enabled if there's worldedit)
        if (getServer().getPluginManager().getPlugin("WorldGuard").isEnabled()) {
            wgd = getServer().getPluginManager().getPlugin("WorldGuard");
        } else {
            getLogger().info("WorldGuard or WorldEdit not enabled! Disabling ProtectionStones...");
            getServer().getPluginManager().disablePlugin(this);
        }

        for (String material : this.getConfig().getString("Blocks").split(",")) {
            String[] split = material.split("-");

            if (split.length > 1 && split.length < 3) {
                if (Material.getMaterial(split[0]) != null) {
                    mats.add(material.toUpperCase());
                } else {
                    getLogger().info("Unrecognized block: " + split[0] + ". Please make sure you have updated your block name for 1.13!");
                }
            } else {
                mats.add(split[0].toUpperCase());
            }
        }

        flags = getConfig().getStringList("Flags");
        allowedFlags = Arrays.asList((getConfig().getString("Allowed Flags").toLowerCase()).split(","));
        deniedWorlds = getConfig().getStringList("Worlds Denied");

        Config.initConfig();

        isCooldownEnable = getConfig().getBoolean("cooldown.enable");
        cooldown = getConfig().getInt("cooldown.cooldown") * 1000;
        cooldownMessage = getConfig().getString("cooldown.message");

        getLogger().info("Building UUID cache...");
        for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
            uuidToName.put(op.getUniqueId(), op.getName());
        }

        // check if they have been upgraded already
        getLogger().info("Checking if PS regions have been updated to UUIDs...");
        //getLogger().info("" + getConfig().contains("UUIDUpdated", true));

        if (!getConfig().contains("UUIDUpdated", true) || !getConfig().getBoolean("UUIDUpdated")) {
            getLogger().info("Updating PS regions to UUIDs...");
            for (World world : Bukkit.getWorlds()) {
                RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));

                // iterate over regions in world
                for (String regionName : rm.getRegions().keySet()) {
                    if (regionName.startsWith("ps")) {
                        ProtectedRegion region = rm.getRegion(regionName);

                        // convert owners with player names to UUIDs
                        List<String> owners = new ArrayList<>(), members = new ArrayList<>();
                        owners.addAll(region.getOwners().getPlayers());
                        members.addAll(region.getMembers().getPlayers());

                        // convert
                        for (String owner : owners) {
                            UUID uuid = nameToUUID(owner);
                            if (uuid != null) {
                                region.getOwners().removePlayer(owner);
                                region.getOwners().addPlayer(uuid);
                            }
                        }
                        for (String member : members) {
                            UUID uuid = nameToUUID(member);
                            if (uuid != null) {
                                region.getMembers().removePlayer(member);
                                region.getMembers().addPlayer(uuid);
                            }
                        }

                    }
                }

                try {
                    rm.save();
                } catch (Exception e) {
                    getLogger().severe("WorldGuard Error [" + e + "] during Region File Save");
                }
            }

            // update config to mark that uuid upgrade has been done

            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(conf, true));
                writer.write("\nUUIDUpdated: true");
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            getLogger().info("Done!");
        }

        getLogger().info("ProtectionStones has successfully started!");
    }

    private static UUID nameToUUID(String name) {
        if (Bukkit.getOfflinePlayer(name) != null) {
            return Bukkit.getOfflinePlayer(name).getUniqueId();
        }
        return null;
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (s instanceof Player) {
            Player p = (Player) s;
            if (cmd.getName().equalsIgnoreCase("ps")) {
                if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
                    p.sendMessage(Messages.getMessage("help", ""));
                    return true;
                }

                // Find the id of the current region the player is in and get WorldGuard player object for use later
                BlockVector3 v = BlockVector3.at(p.getLocation().getX(), p.getLocation().getY(), p.getLocation().getZ());
                String currentPSID;
                RegionManager rgm = getRegionManagerWithPlayer(p);
                List<String> idList = rgm.getApplicableRegionsIDs(v);
                if (idList.size() == 1) {
                    currentPSID = idList.toString().substring(1, idList.toString().length() - 1);
                } else {
                    // Get nearest protection stone if in overlapping region
                    double distanceToPS = 10000D, tempToPS;
                    String namePSID = "";
                    for (String currentID : idList) {
                        if (currentID.substring(0, 2).equals("ps")) {
                            PSLocation psl = parsePSRegionToLocation(currentID);
                            Location psLocation = new Location(p.getWorld(), psl.x, psl.y, psl.z);
                            tempToPS = p.getLocation().distance(psLocation);
                            if (tempToPS < distanceToPS) {
                                distanceToPS = tempToPS;
                                namePSID = currentID;
                            }
                        }
                    }
                    currentPSID = namePSID;
                }

                switch (args[0].toLowerCase()) {
                    case "toggle":
                        if (p.hasPermission("protectionstones.toggle")) {
                            if (!toggleList.contains(p.getName())) {
                                toggleList.add(p.getName());
                                p.sendMessage(Messages.getMessage("protection-placement-off", ""));
                            } else {
                                toggleList.remove(p.getName());
                                p.sendMessage(Messages.getMessage("protection-placement-on", ""));
                            }
                        } else {
                            p.sendMessage(Messages.getMessage("no-permissions", ""));
                        }
                        break;
                    case "count":
                        return ArgCount.argumentCount(p, args);
                    case "region":
                        return ArgRegion.argumentRegion(p, args);
                    case "tp":
                        return ArgTp.argumentTp(p, args);
                    case "home":
                        return ArgTp.argumentTp(p, args);
                    case "admin":
                        return ArgAdmin.argumentAdmin(p, args);
                    case "reclaim":
                        return ArgReclaim.argumentReclaim(p, args, currentPSID);
                    case "bypass":
                        return ArgBypass.argumentBypass(p, args);
                    case "add":
                        return ArgAddRemove.template(p, args, currentPSID, "add");
                    case "remove":
                        return ArgAddRemove.template(p, args, currentPSID, "remove");
                    case "addowner":
                        return ArgAddRemove.template(p, args, currentPSID, "addowner");
                    case "removeowner":
                        return ArgAddRemove.template(p, args, currentPSID, "removeowner");
                    case "view":
                        return ArgView.argumentView(p, args, currentPSID);
                    case "unhide":
                        return ArgHideUnhide.template(p, "unhide", currentPSID);
                    case "hide":
                        return ArgHideUnhide.template(p, "hide", currentPSID);
                    case "priority":
                        return ArgPriority.argPriority(p, args, currentPSID);
                    case "flag":
                        return ArgFlag.argumentFlag(p, args, currentPSID);
                    case "info":
                        return ArgInfo.argumentInfo(p, args, currentPSID);
                    case "reload": {
                        Messages.reload();
                        p.sendMessage(Messages.getMessage("messages-reloaded", ""));
                        break;
                    }
                    default:
                        p.sendMessage(Messages.getMessage("no-such-command", ""));
                }
            }
        } else {
            s.sendMessage(Messages.getMessage("players-only", ""));
        }
        return true;
    }

    public static boolean hasNoAccess(ProtectedRegion region, Player p, LocalPlayer lp, boolean canBeMember) {
        if (region == null) { // Region is not valid
            return true;
        }
        return !p.hasPermission("protectionstones.superowner") && !region.isOwner(lp) && (!canBeMember || !region.isMember(lp));
    }

}
