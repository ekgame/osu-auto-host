package lt.ekgame.autohost;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.google.common.reflect.TypeToken;

import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

public class Settings {
	
	public String username, password, roomName, roomPassword, osuApi;
	public int roomSlots;
	public List<Integer> operatorIds;
	
	public Settings(String path) throws IOException, ObjectMappingException {
		ConfigurationLoader<CommentedConfigurationNode> loader = HoconConfigurationLoader.builder().setPath(Paths.get(path)).build(); // Create the loader
		CommentedConfigurationNode node = loader.load();
		
		CommentedConfigurationNode account = node.getNode("account");
		username = account.getNode("username").getString();
		password = account.getNode("password").getString();
		osuApi = account.getNode("osu-api-key").getString();
		
		CommentedConfigurationNode general = node.getNode("general");
		operatorIds = general.getNode("operators").getList(TypeToken.of(Integer.class));
		
		CommentedConfigurationNode room = node.getNode("room");
		roomName = room.getNode("name").getString();
		roomPassword = room.getNode("password").getString();
		roomSlots = room.getNode("slots").getInt();
	}

}
