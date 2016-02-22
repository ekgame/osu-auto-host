package lt.ekgame.autohost;

import java.io.File;
import java.util.LinkedList;
import java.util.Queue;

import lt.ekgame.bancho.api.units.Beatmap;

public class BeatmapHandler {
	
	private Queue<Beatmap> beatmaps = new LinkedList<>();
	private Queue<Beatmap> completed = new LinkedList<>();
	private Beatmap current = null;
	
	public BeatmapHandler(String folder) {
		File beatmapFolder = new File(folder);
		if (beatmapFolder.exists() && beatmapFolder.isDirectory()) {
			File[] files = beatmapFolder.listFiles(i -> i.getName().toLowerCase().endsWith("osu"));
			for (File file : files) {
				try {
					Beatmap beatmap = new Beatmap(file);
					beatmaps.add(beatmap);
				}
				catch (Exception e) {
					System.err.println("Failed to load " + file.getName());
				}
			}
		}
	}
	
	public boolean recentlyPlayed(Beatmap check, int recentness) {
		int i = 0;
		for (Beatmap beatmap : completed) {
			if (beatmap.getChecksum().toLowerCase().equals(check.getChecksum().toLowerCase()))
				if (completed.size() - i < recentness)
					return true;
			i++;
		}
		return false;
	}
	
	public boolean inQueue(Beatmap check) {
		if (current != null && current.getChecksum().toLowerCase().equals(check.getChecksum().toLowerCase()))
			return true;
		for (Beatmap beatmap : beatmaps) {
			if (beatmap.getChecksum().toLowerCase().equals(check.getChecksum().toLowerCase()))
				return true;
		}
		return false;
	}
	
	public Beatmap nextBeatmap() {
		if (current != null)
			completed.add(current);
		current = beatmaps.poll();
		return current;
	}
	
	public Beatmap getBeatmap() {
		return current;
	}

	public void reset() {
		beatmaps.addAll(completed);
		completed.clear();
		current = null;
	}

	public void push(Beatmap beatmap) {
		beatmaps.add(beatmap);
	}

}
