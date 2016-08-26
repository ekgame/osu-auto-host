package lt.ekgame.autohost;

import java.util.List;
import java.util.stream.Collectors;

import lt.ekgame.bancho.client.CommandExecutor;
import lt.ekgame.bancho.client.MultiplayerHandler;

public class CommandsGeneral implements CommandExecutor {
	
	private AutoHost bot;
	
	public CommandsGeneral(AutoHost bot) {
		this.bot = bot;
	}

	@Override
	public boolean accept(String channel, String sender) {
		return !channel.startsWith("#");
	}

	@Override
	public void handle(String channel, String sender, int userId, String label, List<String> args) {
		if (label.equals("isop")) {
			String response = "You are" + (bot.perms.isOperator(userId) ? "" : " not") + " an operator.";
			bot.bancho.sendMessage(sender, response);
		}
		if (label.equals("op")) {
			if (!bot.perms.isOperator(userId)) {
				bot.bancho.sendMessage(sender, "Insufficient permissions.");
			}
			else if (args.size() == 0) {
				bot.bancho.sendMessage(sender, "You must specify a user ID.");
			}
			else {
				try {
					int id = Integer.parseInt(args.get(0));
					boolean result = bot.perms.addOperator(id);
					if (result)
						bot.bancho.sendMessage(sender, "User #" + id + " is now an operator.");
					else
						bot.bancho.sendMessage(sender, "User #" + id + " is already an operator.");
				}
				catch (Exception e) {
					bot.bancho.sendMessage(sender, "The user ID \"" + args.get(0) + "\" is invalid.");
				}
			}
		}
		/*if (label.equals("createroom") && bot.perms.isOperator(userId)) {
			System.out.println("Creating room");
			MultiplayerHandler mp = bot.bancho.getMultiplayerHandler();
			mp.enableMultiplayer();
			String roomName = "Test room";
			if (args.size() != 0) {
				roomName = args.stream().map(i -> i).collect(Collectors.joining(" "));
			}
			mp.createRoom(roomName, null, 16);
		}*/
		if (label.equals("help") || label.equals("info")) {
			bot.bancho.sendMessage(sender, AutoHost.instance.settings.helpText);
		}
	}
}
