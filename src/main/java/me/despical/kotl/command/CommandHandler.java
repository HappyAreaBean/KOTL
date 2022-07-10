package me.despical.kotl.command;

import me.despical.commons.string.StringMatcher;
import me.despical.kotl.Main;
import me.despical.kotl.command.admin.*;
import me.despical.kotl.command.player.StatsCommand;
import me.despical.kotl.command.player.TopPlayersCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Despical
 * <p>
 * Created at 27.06.2022
 */
public class CommandHandler implements CommandExecutor {

	private final Main plugin;
	private final Set<SubCommand> subCommands;

	public CommandHandler(Main plugin) {
		this.plugin = plugin;
		this.subCommands = new HashSet<>();

		SubCommand[] cmds = {new CreateCommand(), new DeleteCommand(), new EditCommand(), new ListCommand(),
			new ReloadCommand(), new HelpCommand(), new StatsCommand(), new TopPlayersCommand()};

		for (SubCommand cmd : cmds) {
			registerSubCommand(cmd);
		}

		Optional.ofNullable(plugin.getCommand("kotl")).ifPresent(command -> {
			command.setExecutor(this);
			command.setTabCompleter(new TabCompletion(plugin));
		});
	}

	public void registerSubCommand(SubCommand subCommand) {
		subCommands.add(subCommand);
	}

	public Set<SubCommand> getSubCommands() {
		return new HashSet<>(subCommands);
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
		if (args.length == 0) {
			sender.sendMessage(plugin.getChatManager().coloredRawMessage("&3This server is running &bKing of the Ladder v" + plugin.getDescription().getVersion() + " &3by&b Despical"));

			if (sender.hasPermission("kotl.admin")) {
				sender.sendMessage(plugin.getChatManager().coloredRawMessage("&3Commands: &b/" + label + " help"));
			}

			return true;
		}

		for (SubCommand subCommand : subCommands) {
			if (subCommand.getName().equalsIgnoreCase(args[0])) {
				if (!subCommand.hasPermission(sender)) {
					sender.sendMessage(plugin.getChatManager().prefixedMessage("commands.no_permission"));
					return true;
				}

				if (subCommand.getSenderType() == SubCommand.SenderType.PLAYER && !(sender instanceof Player)) {
					sender.sendMessage(plugin.getChatManager().prefixedMessage("commands.only_by_player"));
					return true;
				}

				if (args.length - 1 >= subCommand.getMinimumArguments()) {
					try {
						subCommand.execute(sender, label, Arrays.copyOfRange(args, 1, args.length));
					} catch (CommandException exception) {
						sender.sendMessage(plugin.getChatManager().coloredRawMessage("&c" + exception.getMessage()));
					}
				} else if (subCommand.getType() == SubCommand.CommandType.GENERIC) {
					sender.sendMessage(plugin.getChatManager().coloredRawMessage("&cUsage: /" + label + " " + subCommand.getName() + " " + (subCommand.getPossibleArguments().length() > 0 ? subCommand.getPossibleArguments() : "")));
				}

				return true;
			}
		}

		List<StringMatcher.Match> matches = StringMatcher.match(args[0], subCommands.stream().map(SubCommand::getName).collect(Collectors.toList()));

		if (!matches.isEmpty()) {
			sender.sendMessage(plugin.getChatManager().message("commands.did_you_mean").replace("%command%", label + " " + matches.get(0).getMatch()));
			return true;
		}

		return true;
	}
}