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

package me.vik1395.ProtectionStones.commands;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import eu.mikroskeem.ps.Messages;
import me.vik1395.ProtectionStones.ProtectionStones;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.regex.Pattern;

public class ArgCount {

    // /ps count
    public static boolean argumentCount(Player p, String[] args) {
        WorldGuardPlugin wg = (WorldGuardPlugin) ProtectionStones.wgd;
        RegionManager rgm = ProtectionStones.getRegionManagerWithPlayer(p);
        int count;

        if (args.length == 1) {
            if (p.hasPermission("protectionstones.count")) {
                count = countRegionsOfPlayer(wg.wrapPlayer(p), rgm);
                p.sendMessage(Messages.getMessage("owned-regions", "").replaceAll(Pattern.quote("{count}"), "" + count));
            } else {
                p.sendMessage(Messages.getMessage("no-permission", ""));
            }
            return true;
        } else if (args.length == 2) {
            if (p.hasPermission("protectionstones.count.others")) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(args[1]);
                if (op == null || !op.hasPlayedBefore()) {
                    p.sendMessage(Messages.getMessage("invalid-player", "").replaceAll(Pattern.quote("{player}"), args[1]));
                    return true;
                }
                count = countRegionsOfPlayer(wg.wrapOfflinePlayer(op), rgm);
                p.sendMessage(Messages.getMessage("player-region-count", "").replaceAll(Pattern.quote("{player}"), args[1]).replaceAll(Pattern.quote("{count}"), "" + count));
            } else {
                p.sendMessage(Messages.getMessage("no-permission", ""));
            }
            return true;
        } else {
            p.sendMessage(Messages.getMessage("ps-count-usage", ""));
            return true;
        }
    }

    // Only PS regions, not other regions
    public static int countRegionsOfPlayer(LocalPlayer lp, RegionManager rgm) {
        int count = 0;
        try {
            Map<String, ProtectedRegion> regions = rgm.getRegions();
            for (String selected : regions.keySet()) {
                if (regions.get(selected).getOwners().contains(lp) && regions.get(selected).getId().startsWith("ps")) {
                    count++;
                }
            }
        } catch (Exception ignored) {

        }
        return count;
    }
}
