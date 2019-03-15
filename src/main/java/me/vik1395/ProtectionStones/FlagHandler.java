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

import com.google.common.base.Joiner;
import com.sk89q.worldedit.world.entity.EntityType;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.*;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import eu.mikroskeem.ps.Messages;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Pattern;

public class FlagHandler {

    public void setFlag(String[] args, ProtectedRegion region, Player p) {
        Flag<?> rawFlag = Flags.fuzzyMatchFlag(WorldGuard.getInstance().getFlagRegistry(), args[1]);
        if (rawFlag instanceof StateFlag) {
            StateFlag flag = (StateFlag) rawFlag;
            if (args[2].equalsIgnoreCase("default")) {
                region.setFlag(flag, flag.getDefault());
                region.setFlag(flag.getRegionGroupFlag(), null);
                p.sendMessage(Messages.getMessage("flag-set", "").replaceAll(Pattern.quote("{flag}"), args[1]));
            } else {
                RegionGroup group = null;
                if (Arrays.toString(args).contains("-g")) {
                    int i = 0;
                    for (String s : args) {
                        i++;
                        if (s.equalsIgnoreCase("-g")) {
                            group = getRegionGroup(args[i]);
                        }
                    }
                }
                if (Arrays.toString(args).contains("allow")) {
                        region.setFlag(flag, StateFlag.State.ALLOW);
                        if (group != null) {
                            region.setFlag(flag.getRegionGroupFlag(), group);
                        }
                        p.sendMessage(Messages.getMessage("flag-set", "").replaceAll(Pattern.quote("{flag}"), args[1]));
                } else if (Arrays.toString(args).contains("deny")) {
                    region.setFlag(flag, StateFlag.State.DENY);
                    if (group != null) {
                        region.setFlag(flag.getRegionGroupFlag(), group);
                    }
                    p.sendMessage(Messages.getMessage("flag-set", "").replaceAll(Pattern.quote("{flag}"), args[1]));
                } else {
                    if (group != null) {
                        region.setFlag(flag.getRegionGroupFlag(), group);
                        p.sendMessage(Messages.getMessage("flag-set", "").replaceAll(Pattern.quote("{flag}"), args[1]));
                    } else {
                        p.sendMessage(Messages.getMessage("flag-not-set", "").replaceAll(Pattern.quote("{flag}"), args[1]));
                    }
                }
            }
        } else if (rawFlag instanceof DoubleFlag) {
            DoubleFlag flag = (DoubleFlag) rawFlag;
            if (args[2].equalsIgnoreCase("default")) {
                region.setFlag(flag, flag.getDefault());
                region.setFlag(flag.getRegionGroupFlag(), null);
            } else {
                region.setFlag(flag, Double.parseDouble(args[1]));
            }
            p.sendMessage(Messages.getMessage("flag-set", "").replaceAll(Pattern.quote("{flag}"), args[1]));
        } else if (rawFlag instanceof IntegerFlag) {
            IntegerFlag flag = (IntegerFlag) rawFlag;
            if (args[2].equalsIgnoreCase("default")) {
                region.setFlag(flag, flag.getDefault());
                region.setFlag(flag.getRegionGroupFlag(), null);
            } else {
                region.setFlag(flag, Integer.parseInt(args[1]));
            }
            p.sendMessage(Messages.getMessage("flag-set", "").replaceAll(Pattern.quote("{flag}"), args[1]));
        } else if (rawFlag instanceof StringFlag) {
            StringFlag flag = (StringFlag) rawFlag;
            if (args[2].equalsIgnoreCase("default")) {
                region.setFlag(flag, flag.getDefault());
                region.setFlag(flag.getRegionGroupFlag(), null);
            } else {
                String flagValue = Joiner.on(" ").join(args).substring(args[0].length() + args[1].length() + 2);
                String msg = flagValue.replaceAll("%player%", p.getName());
                region.setFlag(flag, msg);
            }
            p.sendMessage(Messages.getMessage("flag-set", "").replaceAll(Pattern.quote("{flag}"), args[1]));
        } else if (rawFlag instanceof BooleanFlag) {
            BooleanFlag flag = (BooleanFlag) rawFlag;
            if (args[2].equalsIgnoreCase("default")) {
                region.setFlag(flag, flag.getDefault());
                region.setFlag(flag.getRegionGroupFlag(), null);
                p.sendMessage(Messages.getMessage("flag-set", "").replaceAll(Pattern.quote("{flag}"), args[1]));
            } else {
                if (args[2].equalsIgnoreCase("true")) {
                    region.setFlag(flag, true);
                    p.sendMessage(Messages.getMessage("flag-set", "").replaceAll(Pattern.quote("{flag}"), args[1]));
                } else if (args[2].equalsIgnoreCase("false")) {
                    region.setFlag(flag, false);
                    p.sendMessage(Messages.getMessage("flag-set", "").replaceAll(Pattern.quote("{flag}"), args[1]));
                }
            }
        } /*else if(rawFlag instanceof LocationFlag){ //
            System.out.print("LocationFlag!!");
            // NOT PROPERLY IMPLEMENTED YET
        }*/ else if(rawFlag instanceof SetFlag){

            SetFlag flag = (SetFlag) rawFlag;
            if (args[1].equalsIgnoreCase("deny-spawn")) {
                HashSet<EntityType> mobs = new HashSet<>();
                String[] m = new String[args.length-2];
                System.arraycopy(args, 2, m, 0, args.length - 2);
                for (String str : m) {
                    mobs.add(new EntityType(str.toLowerCase()));
                }
                region.setFlag(flag, mobs);
            } else if (args[2].equalsIgnoreCase("default")) {
                region.setFlag(flag, flag.getDefault());
                region.setFlag(flag.getRegionGroupFlag(), null);
            } else {
                region.setFlag(flag, args[2]);
            } // TODO NOT FULLY IMPLEMENTED YET
            p.sendMessage(Messages.getMessage("flag-set", "").replaceAll(Pattern.quote("{flag}"), args[1]));
        }
    }

    private RegionGroup getRegionGroup(String arg) {
        if (arg.equalsIgnoreCase("member") || arg.equalsIgnoreCase("members")) {
            return RegionGroup.MEMBERS;
        } else if (arg.equalsIgnoreCase("nonmembers") || arg.equalsIgnoreCase("nonmember") 
                || arg.equalsIgnoreCase("nomember") || arg.equalsIgnoreCase("nomembers")
                || arg.equalsIgnoreCase("non_members") || arg.equalsIgnoreCase("non_member") 
                || arg.equalsIgnoreCase("no_member") || arg.equalsIgnoreCase("no_members")) {
            return RegionGroup.NON_MEMBERS;
        } else if (arg.equalsIgnoreCase("nonowners") || arg.equalsIgnoreCase("nonowner") 
                || arg.equalsIgnoreCase("noowner") || arg.equalsIgnoreCase("noowners")
                || arg.equalsIgnoreCase("non_owners") || arg.equalsIgnoreCase("non_owner") 
                || arg.equalsIgnoreCase("no_owner") || arg.equalsIgnoreCase("no_owners")) {
            return RegionGroup.NON_OWNERS;
        } else if (arg.equalsIgnoreCase("owner") || arg.equalsIgnoreCase("owners")) {
            return RegionGroup.OWNERS;
        } else if (arg.equalsIgnoreCase("none") || arg.equalsIgnoreCase("noone")) {
            return RegionGroup.NONE;
        } else if (arg.equalsIgnoreCase("all") || arg.equalsIgnoreCase("everyone")) {
            return RegionGroup.ALL;
        } else if (arg.endsWith("empty")) {
            return null;
        }
        
        return null;
    }
}