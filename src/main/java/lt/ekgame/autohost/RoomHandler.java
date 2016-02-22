package lt.ekgame.autohost;

import java.util.ArrayList;
import java.util.List;

import lt.ekgame.bancho.api.packets.Packet;
import lt.ekgame.bancho.api.packets.server.PacketRoomEveryoneFinished;
import lt.ekgame.bancho.api.packets.server.PacketRoomJoined;
import lt.ekgame.bancho.api.packets.server.PacketRoomUpdate;
import lt.ekgame.bancho.api.units.Beatmap;
import lt.ekgame.bancho.api.units.MultiplayerRoom;
import lt.ekgame.bancho.client.MultiplayerHandler;
import lt.ekgame.bancho.client.PacketHandler;

public class RoomHandler implements PacketHandler {
	
	public AutoHost bot;
	
	private int slotsTaken;
	public TimerThread timer;
	private List<Integer> skipVotes = new ArrayList<>();
	
	public RoomHandler(AutoHost bot) {
		this.bot = bot;
	}
	
	public void registerVoteSkip(int userId) {
		if (!skipVotes.contains(userId)) {
			skipVotes.add(userId);
		}
		if (((double)skipVotes.size())/((double)slotsTaken) > 0.5) {
			MultiplayerHandler mp = bot.bancho.getMultiplayerHandler();
			mp.setBeatmap(bot.beatmaps.nextBeatmap());
			//bot.bancho.sendMessage("#multiplayer", "The beatmap was voted off.");
			onBeatmapChange();
		}
	}
	
	public void resetVoteSkip() {
		skipVotes.clear();
	}
	
	public void onBeatmapChange() {
		resetVoteSkip();
		timer.resetTimer();
		Beatmap beatmap = bot.beatmaps.getBeatmap();
		if (beatmap == null) {
			bot.bancho.sendMessage("#multiplayer", "No more beatmaps in the queue. Use !add [link to beatmap] to add more beatmaps.");
		} else {
			bot.bancho.sendMessage("#multiplayer", String.format("Up next: %s - %s [%s] mapped by %s.", 
					beatmap.getArtist(), beatmap.getTitle(), beatmap.getVersion(), beatmap.getCreator()));
		}
	}

	@Override
	public void handle(Packet packet) {
		MultiplayerHandler mp = bot.bancho.getMultiplayerHandler();
		
		if (packet instanceof PacketRoomJoined) {
			bot.beatmaps.reset();
			mp.setBeatmap(bot.beatmaps.nextBeatmap());
			timer = new TimerThread(this);
			timer.start();
		}
		
		if (packet instanceof PacketRoomUpdate && mp.isHost()) {
			PacketRoomUpdate update = (PacketRoomUpdate) packet;
			if (update.room.matchId == mp.getMatchId()) {
				byte[] status = update.room.slotStatus;
				String statuses = "";
				slotsTaken = 0;
				int slotsReady = 0;
				for (int i = 0; i < 16; i++)  {
					statuses += status[i] + " ";
					if (status[i] != 1 && status[i] != 2) {
						if (update.room.slotId[i] != bot.bancho.getClientHandler().getUserId()) {
							slotsTaken++;
							if (status[i] == 8)
								slotsReady++;
						}
					}
				}
				//System.out.println(statuses);
				if (slotsTaken > 0 && slotsTaken == slotsReady) {
					startGame();
					timer.skipEvents();
				}
			}
		}
		
		if (packet instanceof PacketRoomEveryoneFinished) {
			resetVoteSkip();
			mp.setBeatmap(bot.beatmaps.nextBeatmap());
			onBeatmapChange();
		}
	}
	
	public void startGame() {
		MultiplayerHandler mp = bot.bancho.getMultiplayerHandler();
		mp.setReady(true);
		mp.startGame();
	}

	public void onBeatmapAdded(Beatmap beatmap) {
		bot.bancho.sendMessage("#multiplayer", String.format("Added %s - %s [%s] mapped by %s",
				beatmap.getArtist(), beatmap.getTitle(), beatmap.getVersion(), beatmap.getCreator()));
		if (bot.beatmaps.getBeatmap() == null) {
			MultiplayerHandler mp = bot.bancho.getMultiplayerHandler();
			mp.setBeatmap(bot.beatmaps.nextBeatmap());
		}
	}
	
	public void tryStart() {
		MultiplayerHandler mp = bot.bancho.getMultiplayerHandler();
		MultiplayerRoom room = mp.getRoom();
		byte[] status = room.slotStatus;
		slotsTaken = 0;
		int slotsReady = 0;
		String statuses = "";
		for (int i = 0; i < 16; i++)  {
			statuses += status[i] + " ";
			if (status[i] != 1 && status[i] != 2) {
				if (room.slotId[i] != bot.bancho.getClientHandler().getUserId()) {
					slotsTaken++;
					if (status[i] == 8)
						slotsReady++;
				}
			}
		}
		System.out.println(statuses);
		if (slotsTaken > 0 && ((double)slotsReady)/((double)slotsTaken) > 0.7) {
			bot.bancho.sendMessage("#multiplayer", String.format("%d/%d people are ready - starting the game.", slotsReady, slotsTaken));
			startGame();
		} else {
			bot.bancho.sendMessage("#multiplayer", String.format("%d/%d people are ready - extending wait time.", slotsReady, slotsTaken));
			timer.resetTimer();
		}
	}
	
	public void resetBeatmaps() {
		bot.beatmaps.reset();
		MultiplayerHandler mp = bot.bancho.getMultiplayerHandler();
		mp.setBeatmap(bot.beatmaps.nextBeatmap());
	}

	public class TimerThread extends Thread {
		
		private RoomHandler handler;
		
		private boolean stopped = false;
		private long prevTime = System.currentTimeMillis();
		private long startTime;
		private long startAfter = 3*60*1000;
		
		public TimerThread(RoomHandler handler) {
			this.handler = handler;
		}
		
		public void stopTimer() {
			stopped = true;
		}
		
		public void  skipEvents() {
			startTime = System.currentTimeMillis() - 5000;
		}
		
		public void resetTimer() {
			startTime = System.currentTimeMillis() + startAfter + 200;
		}
		
		private void sendMessage(String message) {
			handler.bot.bancho.sendMessage("#multiplayer", message);
		}
		
		public void run() {
			resetTimer();
			while (!stopped) {
				//System.out.println("tick");
				long currTime = System.currentTimeMillis();
				long min3mark = startTime - 3*60*1000;
				long min2mark = startTime - 2*60*1000;
				long min1mark = startTime - 1*60*1000;
				long sec10mark = startTime - 10*1000;
				if (currTime >= min3mark && prevTime<min3mark) {
					sendMessage("Starting in 3 minutes.");
				}
				if (currTime >= min2mark && prevTime<min2mark) {
					sendMessage("Starting in 2 minutes.");
				}
				if (currTime >= min1mark && prevTime<min1mark) {
					sendMessage("Starting in 1 minute.");
				}
				if (currTime >= sec10mark && prevTime<sec10mark) {
					sendMessage("Starting in 10 seconds.");
				}
				if (currTime >= startTime && prevTime<=startTime) {
					handler.tryStart();
				}
				try {
					Thread.sleep(1000);
				} catch (Exception e) {}
				prevTime = currTime;
			}
		}
	}
}
