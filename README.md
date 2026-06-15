# Ranked.hu Bot
Ranked.hu's bot is a Discord bot that allows the user to:
- Get tier tested (main feature)
- Open a ticket
- Get different roles for queues
- Open high test tickets
- Get moderated (lmao)

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
