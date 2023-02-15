/*
 * KOTL - Don't let others to climb top of the ladders!
 * Copyright (C) 2023 Despical
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package me.despical.kotl.arena;

import me.despical.commons.configuration.ConfigUtils;
import me.despical.commons.miscellaneous.MiscUtils;
import me.despical.commons.serializer.LocationSerializer;
import me.despical.kotl.ConfigPreferences;
import me.despical.kotl.Main;
import me.despical.kotl.api.StatsStorage;
import me.despical.kotl.event.ListenerAdapter;
import me.despical.kotl.handler.ChatManager.ActionType;
import me.despical.kotl.handler.rewards.Reward;
import me.despical.kotl.user.User;
import org.apache.commons.lang.math.IntRange;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;

/**
 * @author Despical
 * <p>
 * Created at 22.06.2020
 */
public class ArenaEvents extends ListenerAdapter {

	public ArenaEvents(Main plugin) {
		super (plugin);
	}

	@EventHandler
	public void onEnterAndLeaveGameArea(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		Arena arena = isInArea(player.getLocation());

		if (!plugin.getArenaRegistry().isInArena(player) && arena != null) {
			arena.addPlayer(player);

			player.setGameMode(GameMode.SURVIVAL);
			player.setFoodLevel(20);

			arena.doBarAction(Arena.BarAction.ADD, player);

			if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.JOIN_NOTIFY)) {
				chatManager.broadcastAction(arena, player, ActionType.JOIN);
			}
		}

		if (plugin.getArenaRegistry().isInArena(player) && arena == null) {
			Arena tempArena = plugin.getArenaRegistry().getArena(player);

			tempArena.doBarAction(Arena.BarAction.REMOVE, player);

			if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.LEAVE_NOTIFY)) {
				chatManager.broadcastAction(tempArena, player, ActionType.LEAVE);
			}

			tempArena.removePlayer(player);

			plugin.getUserManager().getDatabase().saveAllStatistic(plugin.getUserManager().getUser(player));
		}
	}

	@EventHandler
	public void onInteractWithPlate(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		Arena arena = plugin.getArenaRegistry().getArena(player);

		if (arena == null) {
			return;
		}

		if (event.getAction() == Action.PHYSICAL) {
			if (event.getClickedBlock().getType() == arena.getArenaPlate().parseMaterial()) {
				if (arena.getPlayers().size() == 1 && arena.getKing() != null && arena.getKing().equals(player)) return;
				arena.setKing(player);

				chatManager.broadcastAction(arena, player, ActionType.NEW_KING);
				plugin.getRewardsFactory().performReward(player, Reward.RewardType.WIN);

				User user = plugin.getUserManager().getUser(player);
				user.addStat(StatsStorage.StatisticType.SCORE, 1);
				user.addStat(StatsStorage.StatisticType.TOURS_PLAYED, 1);

				Set<Player> players = arena.getPlayers();
				players.remove(player);

				spawnFireworks(arena, player);

				for (Player p : players) {
					plugin.getUserManager().getUser(p).addStat(StatsStorage.StatisticType.TOURS_PLAYED, 1);
					plugin.getRewardsFactory().performReward(p, Reward.RewardType.LOSE);

					spawnFireworks(arena, p);
				}
			}
		}
	}

	@EventHandler
	public void onInteractWithDeathBlocks(PlayerInteractEvent event) {
		Player player = event.getPlayer();

		if (!plugin.getConfigPreferences().getOption(ConfigPreferences.Option.DEATH_BLOCKS_ENABLED)) {
			return;
		}

		Arena arena = plugin.getArenaRegistry().getArena(player);

		if (arena == null) {
			return;
		}

		if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK) {
			for (String material : plugin.getConfig().getStringList("Death-Blocks.Blacklisted-Blocks")) {
				if (event.getClickedBlock().getType() == Material.valueOf(material.toUpperCase())) {
					arena.doBarAction(Arena.BarAction.REMOVE, player);
					arena.broadcastMessage(chatManager.prefixedMessage("in_game.clicked_death_block").replace("%player%", player.getName()));
					arena.removePlayer(player);
					arena.teleportToEndLocation(player);

					plugin.getRewardsFactory().performReward(player, Reward.RewardType.LOSE);
					return;
				}
			}
		}
	}

	@EventHandler
	public void onDamage(EntityDamageByEntityEvent event) {
		if (!(event.getEntity() instanceof Player && event.getDamager() instanceof Player)) {
			return;
		}

		Player entity = (Player) event.getEntity(), damager = (Player) event.getDamager();

		if (plugin.getArenaRegistry().isInArena(entity) && plugin.getArenaRegistry().isInArena(damager)) {
			event.setCancelled(false);
			event.setDamage(0d);
		}
	}

	private Arena isInArea(Location origin) {
		FileConfiguration config = ConfigUtils.getConfig(plugin, "arenas");
		Location first, second;

		for (Arena arena : plugin.getArenaRegistry().getArenas()) {
			if (!config.getBoolean("instances." + arena.getId() + ".isdone")) {
				continue;
			}

			first = LocationSerializer.fromString(config.getString("instances." + arena.getId() + ".areaMin"));
			second = LocationSerializer.fromString(config.getString("instances." + arena.getId() + ".areaMax"));

			if (first.getWorld() != null && !first.getWorld().equals(origin.getWorld())) return null;

			if (new IntRange(first.getX(), second.getX()).containsDouble(origin.getX())
				&& new IntRange(first.getY(), second.getY()).containsDouble(origin.getY())
				&& new IntRange(first.getZ(), second.getZ()).containsDouble(origin.getZ())) {
				return arena;
			}
		}

		return null;
	}

	private void spawnFireworks(Arena arena, Player player) {
		if (!plugin.getConfigPreferences().getOption(ConfigPreferences.Option.FIREWORKS_ON_NEW_KING)) return;

		new BukkitRunnable() {

			private int i = 0;

			public void run() {
				if (i == 2 || !arena.getPlayers().contains(player)) {
					cancel();
				}

				MiscUtils.spawnRandomFirework(player.getLocation());
				i++;
			}
		}.runTaskTimer(plugin, 10, 20);
	}
}