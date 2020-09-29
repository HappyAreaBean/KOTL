package me.despical.kotl.arena.managers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import me.clip.placeholderapi.PlaceholderAPI;
import me.despical.commonsbox.configuration.ConfigUtils;
import me.despical.commonsbox.scoreboard.ScoreboardLib;
import me.despical.commonsbox.scoreboard.common.EntryBuilder;
import me.despical.commonsbox.scoreboard.type.Entry;
import me.despical.commonsbox.scoreboard.type.Scoreboard;
import me.despical.commonsbox.scoreboard.type.ScoreboardHandler;
import me.despical.kotl.Main;
import me.despical.kotl.api.StatsStorage;
import me.despical.kotl.arena.Arena;
import me.despical.kotl.user.User;

public class ScoreboardManager {

	private static final Main plugin = JavaPlugin.getPlugin(Main.class);
	private static final FileConfiguration config = ConfigUtils.getConfig(plugin, "messages");
	private final List<Scoreboard> scoreboards = new ArrayList<>();
	private final Arena arena;

	public ScoreboardManager(Arena arena) {
		this.arena = arena;
	}

	/**
	 * Creates arena scoreboard for target user
	 *
	 * @param user user that represents game player
	 * @see User
	 */
	public void createScoreboard(User user) {
		Scoreboard scoreboard = ScoreboardLib.createScoreboard(user.getPlayer()).setHandler(new ScoreboardHandler() {

			@Override
			public String getTitle(Player player) {
				return plugin.getChatManager().colorMessage("Scoreboard.Title");
			}

			@Override
			public List<Entry> getEntries(Player player) {
				return formatScoreboard(user);
			}
		});

		scoreboard.activate();
		scoreboards.add(scoreboard);
	}

	/**
	 * Removes scoreboard of user
	 *
	 * @param user user that represents game player
	 * @see User
	 */
	public void removeScoreboard(User user) {
		for (Scoreboard board : scoreboards) {
			if (board.getHolder().equals(user.getPlayer())) {
				scoreboards.remove(board);
				board.deactivate();
				return;
			}
		}
	}

	/**
	 * Forces all scoreboards to deactivate.
	 */
	public void stopAllScoreboards() {
		scoreboards.forEach(Scoreboard::deactivate);

		scoreboards.clear();
	}

	private List<Entry> formatScoreboard(User user) {
		EntryBuilder builder = new EntryBuilder();
		List<String> lines = config.getStringList("Scoreboard.Content.Playing");

		for (String line : lines) {
			builder.next(formatScoreboardLine(line, user));
		}

		return builder.build();
	}

	private String formatScoreboardLine(String line, User user) {
		String formattedLine = line;
		formattedLine = StringUtils.replace(formattedLine, "%arena%", arena.getId());
		formattedLine = StringUtils.replace(formattedLine, "%players%", String.valueOf(arena.getPlayers().size()));
		formattedLine = StringUtils.replace(formattedLine, "%king%", arena.getKing() != null ? arena.getKing().getName() : "Nobody");
		formattedLine = StringUtils.replace(formattedLine, "%score%", String.valueOf(StatsStorage.getUserStats(user.getPlayer(), StatsStorage.StatisticType.SCORE)));
		formattedLine = StringUtils.replace(formattedLine, "%tours_played%", String.valueOf(StatsStorage.getUserStats(user.getPlayer(), StatsStorage.StatisticType.TOURS_PLAYED)));
		formattedLine = plugin.getChatManager().colorRawMessage(formattedLine);

		if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			formattedLine = PlaceholderAPI.setPlaceholders(user.getPlayer(), formattedLine);
		}

		return formattedLine;
	}
}