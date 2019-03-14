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

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import eu.mikroskeem.ps.Messages;
import me.vik1395.ProtectionStones.ProtectionStones;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.regex.Pattern;

public class ArgAddRemove {
    private static OfflinePlayer checks(Player p, String args[], String psID, RegionManager rgm, WorldGuardPlugin wg, String permType, boolean checkPlayer) {
        if (permType.equals("members") && !p.hasPermission("protectionstones.members")) {
            p.sendMessage(Messages.getMessage("no-permission", ""));
            return null;
        } else if (permType.equals("owners") && !p.hasPermission("protectionstones.owners")) {
            p.sendMessage(Messages.getMessage("no-permission", ""));
            return null;
        } else if (ProtectionStones.hasNoAccess(rgm.getRegion(psID), p, wg.wrapPlayer(p), false)) {
            p.sendMessage(Messages.getMessage("not-allowed-to-use", ""));
            return null;
        } else if (args.length < 2) {
            p.sendMessage(Messages.getMessage("player-required", ""));
            return null;
        } else if (psID.equals("")) {
            p.sendMessage(Messages.getMessage("not-in-claim", ""));
            return null;
        }
        OfflinePlayer op = Bukkit.getOfflinePlayer(args[1]);
        if ((op == null || !op.hasPlayedBefore()) && checkPlayer) {
            p.sendMessage(Messages.getMessage("player-not-found-db", "").replaceAll(Pattern.quote("{player}"), args[1]));
            return null;
        }
        return op;
    }

    // Handles adding and removing players to region, both as members and owners
    // type:
    //   add: add member
    //   remove: remove member
    //   addowner: add owner
    //   removeowner: remove owner

    public static boolean template(Player p, String[] args, String psID, String type) {
        WorldGuardPlugin wg = (WorldGuardPlugin) ProtectionStones.wgd;
        RegionManager rgm = ProtectionStones.getRegionManagerWithPlayer(p);
        OfflinePlayer op = checks(p, args, psID, rgm, wg, (type.equals("add") || type.equals("remove")) ? "members" : "owners", type.startsWith("add")); // validate permissions and stuff
        if (op == null) return true;

        switch (type) {
            case "add":
                rgm.getRegion(psID).getMembers().addPlayer(op.getUniqueId());
                break;
            case "remove":
                rgm.getRegion(psID).getMembers().removePlayer(op.getUniqueId());
                rgm.getRegion(psID).getMembers().removePlayer(op.getName());
                break;
            case "addowner":
                rgm.getRegion(psID).getOwners().addPlayer(op.getUniqueId());
                break;
            case "removeowner":
                rgm.getRegion(psID).getOwners().removePlayer(op.getUniqueId());
                rgm.getRegion(psID).getOwners().removePlayer(op.getName());
                break;
        }
        try {
            rgm.save();
        } catch (Exception e) {
            ProtectionStones.getPlugin().getLogger().severe("WorldGuard Error [" + e + "] during Region File Save");
        }

        if (type.equals("add") || type.equals("addowner")) {
            p.sendMessage(Messages.getMessage("added-to-protection", "").replaceAll(Pattern.quote("{player}"), op.getName()));
        } else if (type.equals("remove") || type.equals("removeowner")) {
            p.sendMessage(Messages.getMessage("removed-from-protection", "").replaceAll(Pattern.quote("{player}"), op.getName()));
        }
        return true;
    }
}
