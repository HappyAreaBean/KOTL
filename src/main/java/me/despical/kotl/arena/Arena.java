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

import me.despical.commons.ReflectionUtils;
import me.despical.commons.compat.XMaterial;
import me.despical.commons.miscellaneous.AttributeUtils;
import me.despical.commons.serializer.InventorySerializer;
import me.despical.kotl.ConfigPreferences;
import me.despical.kotl.Main;
import me.despical.kotl.arena.managers.BossBarManager;
import me.despical.kotl.arena.managers.ScoreboardManager;
import me.despical.kotl.handler.ChatManager;
import me.despical.kotl.handler.rewards.Reward;
import org.apache.commons.lang.math.IntRange;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Despical
 * <p>
 * Created at 20.06.2020
 */
public class Arena {
	
	private static final Main plugin = JavaPlugin.getPlugin(Main.class);

	private final String id;
	private boolean ready = true;

	private final Set<Player> players;
	private final Map<GameLocation, Location> gameLocations;

	private String king;
	private XMaterial arenaPlate;
	private BossBarManager bossBarManager;

	private final ScoreboardManager scoreboardManager;

	public Arena(String id) {
		this.id = id;
		this.players = new HashSet<>();
		this.gameLocations = new EnumMap<>(GameLocation.class);
		this.scoreboardManager = new ScoreboardManager(plugin, this);
		this.arenaPlate = XMaterial.OAK_PRESSURE_PLATE;

		if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BOSS_BAR_ENABLED)) {
			if (!ReflectionUtils.supports(9)) return;

			this.bossBarManager = new BossBarManager(plugin);
		}
	}
	
	public boolean isReady() {
		return ready;
	}

	public void setReady(boolean ready) {
		this.ready = ready;
	}
	
	/**
	 * Get arena identifier used to get arenas by string.
	 *
	 * @return arena name
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * Get all players in arena.
	 *
	 * @return set of players in arena
	 */
	public Set<Player> getPlayers() {
		return new HashSet<>(players);
	}
	
	/**
	  * Get end location of arena.
	  *
	  * @return end location of arena
	  */
	 public Location getEndLocation() {
	   return gameLocations.get(GameLocation.END);
	 }

	/**
	 * Set end location of arena.
	 *
	 * @param endLoc new end location of arena
	 */
	public void setEndLocation(Location endLoc) {
		gameLocations.put(GameLocation.END, endLoc);
	}

	/**
	 * Get arena's plate location.
	 * 
	 * @return plate location of arena
	 */
	public Location getPlateLocation() {
		return gameLocations.get(GameLocation.PLATE);
	}
	
	/**
	 * Set plate location.
	 * 
	 * @param plateLoc new plate location of arena
	 */
	public void setPlateLocation(Location plateLoc) {
		gameLocations.put(GameLocation.PLATE, plateLoc);
	}

	public Location getMinCorner() {
		return gameLocations.get(GameLocation.MIN);
	}

	public void setMinCorner(final Location minCorner) {
		gameLocations.put(GameLocation.MIN, minCorner);
	}

	public Location getMaxCorner() {
		return gameLocations.get(GameLocation.MAX);
	}

	public void setMaxCorner(final Location maxCorner) {
		gameLocations.put(GameLocation.MAX, maxCorner);
	}

	public void setKing(String king) {
		this.king = king;
	}

	@Nullable
	public String getKing() {
		return king;
	}

	@NotNull
	public String getKingName() {
		return king == null ? plugin.getChatManager().message("in_game.there_is_no_king") : king;
	}

	public void setArenaPlate(XMaterial arenaPlate) {
		this.arenaPlate = arenaPlate;
	}

	public XMaterial getArenaPlate() {
		return arenaPlate;
	}

	@Nullable
	public Arena isInArea(final Player player) {
		if (!ready) return null;

		final Location min = getMinCorner(), max = getMaxCorner(), origin = player.getLocation();

		if (!min.getWorld().equals(player.getWorld())) return null;

		if (new IntRange(min.getX(), max.getX()).containsDouble(origin.getX())
			&& new IntRange(min.getY(), max.getY()).containsDouble(origin.getY())
			&& new IntRange(min.getZ(), max.getZ()).containsDouble(origin.getZ())) {

			return this;
		}

		return null;
	}
	
	/**
	 * Get arena's scoreboard manager
	 * 
	 * @return scoreboard manager of arena
	 */
	public ScoreboardManager getScoreboardManager() {
		return scoreboardManager;
	}

	public void broadcastMessage(String message) {
		for (Player player : players) player.sendMessage(message);
	}

	public void addPlayer(Player player) {
		players.add(player);

		AttributeUtils.setAttackCooldown(player, plugin.getConfig().getDouble("Hit-Cooldown-Delay", 4));

		final var preferences = plugin.getConfigPreferences();
		final var user = plugin.getUserManager().getUser(player);

		if (preferences.getOption(ConfigPreferences.Option.INVENTORY_MANAGER_ENABLED)) {
			InventorySerializer.saveInventoryToFile(plugin, player);
		}

		if (preferences.getOption(ConfigPreferences.Option.HEAL_PLAYER)) {
			AttributeUtils.healPlayer(player);
		}

		if (preferences.getOption(ConfigPreferences.Option.SCOREBOARD_ENABLED)) {
			user.cacheScoreboard();

			scoreboardManager.createScoreboard(player);
		}

		if (preferences.getOption(ConfigPreferences.Option.CLEAR_INVENTORY)) {
			player.getInventory().clear();
		}

		if (preferences.getOption(ConfigPreferences.Option.CLEAR_EFFECTS)) {
			player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
		}

		player.setGameMode(GameMode.SURVIVAL);
		player.setFoodLevel(20);

		doBarAction(player, 1);

		if (preferences.getOption(ConfigPreferences.Option.JOIN_NOTIFY)) {
			plugin.getChatManager().broadcastAction(this, player, ChatManager.ActionType.JOIN);
		}

		user.performReward(Reward.RewardType.JOIN);
	}
	
	public void removePlayer(Player player) {
		if (player == null) return;

		final var preferences = plugin.getConfigPreferences();
		final var user = plugin.getUserManager().getUser(player);
		user.performReward(Reward.RewardType.LEAVE);

		players.remove(player);

		if (preferences.getOption(ConfigPreferences.Option.CLEAR_INVENTORY)) {
			player.getInventory().clear();
		}

		if (preferences.getOption(ConfigPreferences.Option.INVENTORY_MANAGER_ENABLED)) {
			InventorySerializer.loadInventory(plugin, player);
		}

		if (preferences.getOption(ConfigPreferences.Option.SCOREBOARD_ENABLED)) {
			scoreboardManager.removeScoreboard(player);

			user.removeScoreboard();
		}

		if (preferences.getOption(ConfigPreferences.Option.LEAVE_NOTIFY)) {
			plugin.getChatManager().broadcastAction(this, player, ChatManager.ActionType.LEAVE);
		}

		doBarAction(player, 0);

		plugin.getUserManager().getDatabase().saveStatistics(user);

		AttributeUtils.resetAttackCooldown(player);
	}

	public void teleportToEndLocation(Player player) {
		final Location location = getEndLocation();

		if (location == null) {
			plugin.getLogger().warning("Couldn't teleport " + player.getName() + " to end location!");
			return;
		}

		player.teleport(location);
	}
	
	public void teleportAllToEndLocation() {
		players.forEach(this::teleportToEndLocation);
	}

	public void doBarAction(Player player, int action) {
		if (bossBarManager == null) return;

		if (action == 1) {
			bossBarManager.addPlayer(player);
		} else {
			bossBarManager.removePlayer(player);
		}
	}

	public enum GameLocation {
		MIN, MAX, END, PLATE
	}
}