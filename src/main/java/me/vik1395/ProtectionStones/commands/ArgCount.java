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
import me.vik1395.ProtectionStones.PSL;
import me.vik1395.ProtectionStones.ProtectionStones;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;

public class ArgCount {

    // /ps count
    public static boolean argumentCount(Player p, String[] args) {
        WorldGuardPlugin wg = ProtectionStones.wgd;
        RegionManager rgm = ProtectionStones.getRegionManagerWithPlayer(p);
        Bukkit.getScheduler().runTaskAsynchronously(ProtectionStones.getPlugin(), () -> {
            int count;

            if (args.length == 1) {
                if (p.hasPermission("protectionstones.count")) {

                    count = countRegionsOfPlayer(wg.wrapPlayer(p), rgm);
                    PSL.msg(p, PSL.PERSONAL_REGION_COUNT.msg()
                            .replace("%num%", "" + count));

                } else {
                    PSL.msg(p, PSL.NO_PERMISSION_COUNT.msg());
                }
            } else if (args.length == 2) {

                if (p.hasPermission("protectionstones.count.others")) {

                    if (!ProtectionStones.nameToUUID.containsKey(args[1])) {
                        PSL.msg(p, PSL.PLAYER_NOT_FOUND.msg());
                        return;
                    }

                    count = countRegionsOfPlayer(wg.wrapOfflinePlayer(Bukkit.getOfflinePlayer(ProtectionStones.nameToUUID.get(args[1]))), rgm);

                    PSL.msg(p, PSL.OTHER_REGION_COUNT.msg()
                            .replace("%player%", args[1])
                            .replace("%num%", "" + count));
                } else {
                    PSL.msg(p, PSL.NO_PERMISSION_COUNT_OTHERS.msg());
                }
            } else {
                PSL.msg(p, PSL.COUNT_HELP.msg());
            }
        });
        return true;
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
