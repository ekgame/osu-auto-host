package lt.ekgame.autohost;

import java.io.IOException;
import java.net.URISyntaxException;

import lt.ekgame.bancho.api.exceptions.LoginException;
import lt.ekgame.bancho.api.packets.Packet;
import lt.ekgame.bancho.api.packets.server.PacketReceivingFinished;
import lt.ekgame.bancho.api.packets.server.PacketRoomJoined;
import lt.ekgame.bancho.client.BanchoClient;
import lt.ekgame.bancho.client.MultiplayerHandler;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

public class AutoHost {

	public static void main(String... args) throws Exception {
		new AutoHost(args);
	}
	
	public BanchoClient bancho;
	public Permissions perms;
	public BeatmapHandler beatmaps = new BeatmapHandler("beatmaps");
	public RoomHandler roomHandler;
	public Settings settings;

	public AutoHost(String... args) throws IOException, URISyntaxException, LoginException, ObjectMappingException {
		if (args.length == 0) {
			System.err.println("You must specify a settings file.");
			return;
		}
		
		settings = new Settings(args[0]);
		perms = new Permissions(settings.operatorIds);
		
		bancho = new BanchoClient(settings.username, settings.password, false, true);
		bancho.getCommandHandler().addExecutor(new CommandsGeneral(this));
		bancho.getCommandHandler().addExecutor(new CommandsRoom(this, settings.osuApi));
		bancho.registerHandler(roomHandler = new RoomHandler(this));
		bancho.registerHandler((Packet packet) -> {
			if (packet instanceof PacketReceivingFinished) {
				System.out.println("Creating room...");
				MultiplayerHandler mp = bancho.getMultiplayerHandler();
				mp.enableMultiplayer();
				mp.createRoom(settings.roomName, settings.roomPassword, settings.roomSlots);
			}
			if (packet instanceof PacketRoomJoined) {
				System.out.println("Room created!");
			}
		});
		
		System.out.println("Running client...");
		bancho.connect();
		System.out.println("Authanticated, starting...");
		bancho.start();
		System.out.println("Started.");
	}
	
}
