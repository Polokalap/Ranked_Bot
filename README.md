# Ranked.hu Discord bot

> This bot was created by Polokalap for [HackClub StarDance](https://stardance.hackclub.com/projects/1911) and is licensed under AGPL-3.0.

## Features
- Customizeable moderation system
- Link spam filter
- Ticket system (can be opened via panel or command)
- Ticket archives
- Profile command (from discord profile or username)
- Welcome messages
- Queue pings
- Queue system with gamemodes
- High tier testing (tickets but cooler)
- Spin command (may rework later)
- Admin profile command

## How to set up the bot for yourself:
> I do NOT recommend setting it up yourself. The bot writes directly to the DB, uses the API (closed source for now). If you are the person reviewing it from HackClub, I'd rather just watch the video on it.

If you **do** have this much free time, basically you need to clone the repo, and build it with the following stuff changed to your bot:

- resources/.env
```
BOT_TOKEN=DC_BOT_TOKEN
DB_HOST=YOUR_SERVER
DB_PORT=YOUR_DB_PORT
DB_DATABASE=YOUR_DB_NAME (most likely postgres lol)
DB_USER=DB_USERNAME
DB_PASSWORD=DB_PASSWORD
```

- resources/data.json
> You need to download [this archive](https://github.com/Polokalap/Ranked_Bot/raw/refs/heads/master/emojis.tar.xz) of the icons that you need to upload to the Discord Developer Portal. This process itelf takes about **30 minutes** because you have to set **every emoji with it's id in the data.json**.
For the channels, just create the following channels:
- Mod Alerts
- Welcome
- Rules
- Test Results
- Logs

You also need to create the following roles:
- Admin
- Manager
- Moderator
- Default

And these categories:
- Ticket Archives
- Ticket Category

You need to fill **all of these out** in the `data.yml`.

## How to use the moderation system
To set up a filter, you need to navigate to `resources/filter.json` then create a field inside of the `flags` array. You must set the following fields in the flag:
- "name": "The name of the flag"
- "description": "A description of the flag"
- "punishment": "A punishment that ranges from NONE, so nothing to a KICK, or a BAN or a SEVEN_DAY cooldown"
- "flags": "An an array of strings to flag"
- "ignore": "An an array of strings not to flag (for example, youtube.com is in ignore and :// is in flags, this lets you send https://youtube.com and flags other websites)"
- "always": "An array of strings to flag no matter what"

## Where to test the bot
The bot is always running at [our discord server](https://dc.ranked.hu).
