package me.despical.kotl.handler.rewards;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import me.despical.commonsbox.configuration.ConfigUtils;
import me.despical.commonsbox.engine.ScriptEngine;
import me.despical.kotl.Main;
import me.despical.kotl.arena.Arena;
import me.despical.kotl.arena.ArenaRegistry;
import me.despical.kotl.utils.Debugger;

/**
 * @author Despical
 * <p>
 * Created at 30.06.2020
 */
public class RewardsFactory {

	private Set<Reward> rewards = new HashSet<>();
	private FileConfiguration config;
	private boolean enabled;

	public RewardsFactory(Main plugin) {
		enabled = plugin.getConfig().getBoolean("Rewards-Enabled");
		config = ConfigUtils.getConfig(plugin, "rewards");
		registerRewards();
	}

	public void performReward(Arena arena, Reward.RewardType type) {
		if (!enabled) {
			return;
		}
		for (Player p : arena.getPlayers()) {
			performReward(p, type);
		}
	}

	public void performReward(Player player, Reward.RewardType type) {
		if (!enabled) {
			return;
		}
		Arena arena = ArenaRegistry.getArena(player);
		ScriptEngine engine = new ScriptEngine();
		engine.setValue("player", player);
		engine.setValue("server", Bukkit.getServer());
		engine.setValue("arena", arena);
		for (Reward reward : rewards) {
			if (reward.getType() == type) {
				if (reward.getChance() != -1 && ThreadLocalRandom.current().nextInt(0, 100) > reward.getChance()) {
					continue;
				}
				String command = reward.getExecutableCode();
				command = StringUtils.replace(command, "%player%", player.getName());
				command = formatCommandPlaceholders(command, arena);
				switch (reward.getExecutor()) {
				case CONSOLE:
					Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
					break;
				case PLAYER:
					player.performCommand(command);
					break;
				case SCRIPT:
					engine.execute(command);
					break;
				default:
					break;
				}
			}
		}
	}

	private String formatCommandPlaceholders(String command, Arena arena) {
		String formatted = command;
		formatted = StringUtils.replace(formatted, "%arena%", arena.getId());
		formatted = StringUtils.replace(formatted, "%players%", String.valueOf(arena.getPlayers().size()));
		return formatted;
	}

	private void registerRewards() {
		if (!enabled) {
			return;
		}
		Debugger.debug(Level.INFO, "[RewardsFactory] Starting rewards registration");
		long start = System.currentTimeMillis();

		Map<Reward.RewardType, Integer> registeredRewards = new HashMap<>();
		for (Reward.RewardType rewardType : Reward.RewardType.values()) {
			for (String reward : config.getStringList("rewards." + rewardType.getPath())) {
				rewards.add(new Reward(rewardType, reward));
				registeredRewards.put(rewardType, registeredRewards.getOrDefault(rewardType, 0) + 1);
			}
		}
		for (Reward.RewardType rewardType : registeredRewards.keySet()) {
			Debugger.debug(Level.INFO, "[RewardsFactory] Registered {0} {1} rewards!", registeredRewards.get(rewardType), rewardType.name());
		}
		Debugger.debug(Level.INFO, "[RewardsFactory] Registered all rewards took {0} ms", System.currentTimeMillis() - start);
	}
}