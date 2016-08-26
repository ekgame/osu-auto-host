package lt.ekgame.autohost;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import lt.ekgame.bancho.api.units.Beatmap;
import lt.ekgame.bancho.client.CommandExecutor;
import lt.ekgame.bancho.client.MultiplayerHandler;

public class CommandsRoom implements CommandExecutor {
	
	private AutoHost bot;
	private Pattern beatmapMatcher = Pattern.compile("((https?:\\/\\/)?osu\\.ppy\\.sh\\/b\\/)(\\d*)");
	private String osuApiKey;
	
	
	public CommandsRoom(AutoHost bot, String osuApiKey) {
		this.bot = bot;
		this.osuApiKey = osuApiKey;
	}

	@Override
	public boolean accept(String channel, String sender) {
		return channel.equals("#multiplayer");
	}

	@Override
	public void handle(String channel, String sender, int userId, String label, List<String> args) {
		MultiplayerHandler mp = bot.bancho.getMultiplayerHandler();
		if (label.equals("leave") && bot.perms.isOperator(userId)) {
			bot.bancho.sendMessage(channel, "Goodbye!");
			bot.roomHandler.timer.stopTimer();
			mp.leaveRoom();
		}
		
		if (label.equals("reset") && bot.perms.isOperator(userId)) {
			bot.roomHandler.resetBeatmaps();
		}
		
		if (label.equals("voteskip")) {
			bot.roomHandler.registerVoteSkip(userId);
		}
		
		if (label.equals("skip") && bot.perms.isOperator(userId)) {
			if (mp.isHost()) {
				mp.setBeatmap(bot.beatmaps.nextBeatmap());
				bot.roomHandler.onBeatmapChange();
			}
		}
		
		if (label.equals("freemods") && bot.perms.isOperator(userId)) {
			if (mp.isHost()) {
				mp.setFreeMods(!mp.isFreeModsEnabled());
			}
		}
		
		if (label.equals("start") && bot.perms.isOperator(userId)) {
			if (mp.isHost()) {
				mp.startGame();
			}
		}
		
		if (label.equals("info")) {
			bot.bancho.sendMessage(channel, AutoHost.instance.settings.infoText);
		}
		
		if (label.equals("add") && args.size() > 0) {
			Matcher matcher = beatmapMatcher.matcher(args.get(0));
			if (matcher.find()) {
				//bot.bancho.sendMessage(channel, "Analyzing beatmap #" + matcher.group(3) + ". Please wait...");
				int beatmapId = Integer.parseInt(matcher.group(3));
				try {
					// Is this JavaScript? This is probably JavaScript.
					getBeatmap(beatmapId, (obj) -> {
						if (obj == null) {
							bot.bancho.sendMessage(channel, sender + ": Beatmap not found.");
						} else {
							int approval = obj.getInt("approved");
							double difficulty = obj.getDouble("difficultyrating");
							int length = obj.getInt("total_length");
							int mode = obj.getInt("mode");
							
							Settings settings = AutoHost.instance.settings;
							boolean matchingGamemode = mode == settings.gamemode;
							boolean matchingDifficulty = difficulty >= settings.minDifficulty && difficulty <= settings.maxDifficulty;
							boolean matchingLength = length <= settings.maxLength;
							boolean matchingApproval = settings.allowGraveyard ? true : (approval >= 0 &&  approval <= 2);
							
							if (!matchingGamemode) {
								bot.bancho.sendMessage(channel, sender + ": This gamemode is not allowed.");
							}
							else if (!matchingDifficulty) {
								bot.bancho.sendMessage(channel, sender + ": Invalid difficulty. Must be between " + settings.minDifficulty + " and " + settings.maxDifficulty + ".");
							}
							else if (!matchingLength) {
								bot.bancho.sendMessage(channel, sender + ": This map is too long.");
							}
							else if (!matchingApproval) {
								bot.bancho.sendMessage(channel, sender + ": Graveyarded maps not allowed.");
							}
							else {
								String title = obj.getString("title");
								String artist = obj.getString("artist");
								String creator = obj.getString("creator");
								String version = obj.getString("version");
								String beatmapMD5 = obj.getString("file_md5");
								Beatmap beatmap = new Beatmap(artist, title, version, creator, beatmapMD5, beatmapId);
								if (bot.beatmaps.inQueue(beatmap)) {
									bot.bancho.sendMessage(channel, sender + ": This beatmap is already in the queue.");
								} else if (bot.beatmaps.recentlyPlayed(beatmap, 30)) {
									bot.bancho.sendMessage(channel, sender + ": This beatmap has been played recently. PM me !help for more info.");
								}
								else{
									bot.beatmaps.push(beatmap);
									bot.roomHandler.onBeatmapAdded(beatmap);
								}
							}
						}
					});
				} catch (JSONException | URISyntaxException | IOException e) {
					e.printStackTrace();
					bot.bancho.sendMessage(channel, sender + ": osu! servers seem to be a little slow right now, try again later.");
				}
			} else {
				//bot.bancho.sendMessage(channel, "not found");
			}
		}
	}
	
	void getBeatmap(int beatmapId, Consumer<JSONObject> callback) throws URISyntaxException, ClientProtocolException, IOException {
		RequestConfig defaultRequestConfig = RequestConfig.custom()
			    .setSocketTimeout(10000)
			    .setConnectTimeout(10000)
			    .setConnectionRequestTimeout(10000)
			    .build();
		
		HttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(defaultRequestConfig).build();
		URI uri = new URIBuilder()
				.setScheme("http")
				.setHost("osu.ppy.sh")
				.setPath("/api/get_beatmaps")
				.setParameter("k", osuApiKey)
				.setParameter("b", ""+beatmapId)
				.build();
		HttpGet request = new HttpGet(uri);
		HttpResponse response = httpClient.execute(request);
		InputStream content = response.getEntity().getContent();
		String stringContent = IOUtils.toString(content, "UTF-8"); 
		JSONArray array = new JSONArray(stringContent);
		callback.accept(array.length() > 0 ? (JSONObject)array.get(0) : null);
	}
}
