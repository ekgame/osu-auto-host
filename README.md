# AutoHost for osu!
A bot for osu! multiplayer to allow automatic beatmap rotation. This software was created to test capabilities of my [Bancho client framework](https://github.com/ekgame/bancho-client) project.

## When does a game start?

Before starting a game, the bot will wait 3 minutes for everyone to get ready. If everyone is ready before the time runs out - the game will start instantly. If about 70% people are ready when the timer runs out - the game will be force-started.

## Interaction

To use the bot you can type `!command` into the chat. Some commands only work in the multiplayer chat, some only in private chat. There are only two permission levels at the moment: user (default) and an operator. Operator is an administrative role and therefore it can use some more commands.

## Commands

| Command       | Description |
|---|---|
| !add [link to beatmap]  | Adds a beatmap to a queue of maps to play. The beatmaps must match a criteria decribed below. |
| !voteskip | Vote to skip a map. Over 50% of users have to vote to skip the current song. |
| !skip | An operator command. Instantly skips a map. |
| !wait | An operator command. Resets the waiting timer (default: 3 minutes). |
| !isop | Only works in private chat. Tells you whether or not you are an operator. |
| !help | Only works in private chat. Links you to this page. |

## Adding beatmaps

When you're adding a beatmap, the link should look something like this: `https://osu.ppy.sh/b/665240` of just `osu.ppy.sh/b/665240`. Notice the `/b/` - it denotes that this link point to a specific difficulty in a beatmap set. If the link has `/s/` - it points to a beatmap set and therefore is too ambigious and invalid. If you want to get the valid link, on the beatmap set page, click on one of the **difficulty tabs**.

## Beatmap queue
To setup a beatmap queue, you will need to create `beatmaps` folder next to the executable file. This folder should contain various `.osu` files of beatmaps to play.

## Beatmap criteria
This is the current criteria for using the !add command. This will be configurable later, but for now it's hardcoded to:
* The map must be for osu! standard gamemode.   
* The map's star difficulty must be between 4.8 and 6 stars.
* The map must be either ranked, qualified or pending.
* The map can not be longer that 6 minutes.
* The map can not be a repeat of the last 30 songs.

## Compiling
First of all, you will need some dependencies. Most of the dependency management is done with Maven. There are two libraries that you will need to reference manually:
* [Bancho API](https://github.com/ekgame/bancho-api) - the commons API used for packet parsing.
* [Bancho Client](https://github.com/ekgame/bancho-client) - the framework for Bancho client.

## Running
First of all compile or download the executable file from [releases page](https://github.com/ekgame/osu-auto-host/releases).
Then to run the bot, you will first need to make a configuration file `settings.conf` like this:
```PYTHON
account {
  username = "username"
  password = "password"
  osu-api-key = "apikey" # obtained here https://osu.ppy.sh/p/api
}

general {
  operators = [ # user IDs of trusted users
    2982427
    3872846
  ]
}

room {
  name = "AutoHost testing"
  password = null  
  slots = 16
}
```
Before running the bot, don't forget to setup a `beatmaps` folder. Your file structure should be something like this:
```
/autohost
|--/beatmaps
|  |--beatmap1.osu
|  |--beatmap2.osu
|  |--etc...
|--settings.conf
|--autohost.jar
```

Then to actually run the bot the bot use the file as an argument:

```java -jar autohost.jar settings.conf```
