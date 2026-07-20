# Ranked.hu Discord bot

> This bot was created by Polokalap for [HackClub StarDance](https://stardance.hackclub.com/projects/1911) and is licensed under AGPL-3.0.

---

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

---

## How to set up the bot for yourself:
> I do NOT recommend setting it up yourself. The bot writes directly to the DB, uses the API (closed source for now). If you are the person reviewing it from HackClub, I'd rather just read how tier testing works and maybe DM me about it on Discord, or shoot her an email at `Polokalap@proton.me` if I have questions.

If you **do** have this much free time, basically you need to clone the repo, and build it with the following stuff changed to your bot:

- `resources/.env`
```
BOT_TOKEN=DC_BOT_TOKEN
DB_HOST=YOUR_SERVER
DB_PORT=YOUR_DB_PORT
DB_DATABASE=YOUR_DB_NAME (most likely postgres lol)
DB_USER=DB_USERNAME
DB_PASSWORD=DB_PASSWORD
```

- `resources/data.json`
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

You need to fill **all of these out** in the `data.json`.

- `resources/ur_LANG.json`
> Don't bother to make it yourself, ask claude to translate it. It's not worth your time.

Soo yup good luck with all that. When you are done, you can just run `./gradlew clean build` and run the jar archive via `java -jar build/libs/RankedBOT-1.0-SNAPSHOT.jar`.

---

## How the tier testing works
> So first, your server needs **tier testers**. Tier testers are basically known good players who can rate new players. They test players by going to the Queue Panel that you set up as an admin by typing the following command: `/queue-panel`. This creates a new queue panel that looks something like this:
<img width="602" height="386" alt="image" src="https://github.com/user-attachments/assets/bc61d2f9-3255-4b78-87b6-c68b750e3f04" />

---

> Tier testers can select a gamemode, which is **mace** in my case and open a **queue**. A queue looks like this:
<img width="433" height="315" alt="image" src="https://github.com/user-attachments/assets/5f622312-1af4-4d86-986b-800d803e6cc8" />

---

> The bot pings all users who have the queue ping role for that gamemode, and lets users join by clicking the button under it, which is "Belépés a queue-ba" in my language.
> The tester can click the "Következő ember" button on the queue panel, which opens a new ticket for the #1 person in the queue. Here, a tester can give a tier to the player from **LT5-LT3**, LT3 being the best.
<img width="602" height="252" alt="image" src="https://github.com/user-attachments/assets/0029eef1-6747-4532-b1c4-22f48b3dc353" />

---

> If a player plays well and gets the highest tier (aka LT3), they can apply for a **High Test**. The panel looks like this and can be set up with the `/high-ticket-panel` command:
<img width="605" height="299" alt="image" src="https://github.com/user-attachments/assets/5ce72faf-ec9a-4664-995e-d0209de7f9e9" />

---

> The player can select a gamemode, and if they meet the requirements, a **High Ticket** opens, which has a panel like this that only managers and above interact with:
<img width="485" height="288" alt="image" src="https://github.com/user-attachments/assets/b4cabef9-7913-4e4e-967b-9b685f3cdc7d" />

---

> A manager operates the high tier ticket, they use stuff like `/spin`.

If you really care, you can also read about it on [our website](https://ranked.hu/docs/szabalyok).

---

## Commands

### /profile
<img width="550" height="505" alt="image" src="https://github.com/user-attachments/assets/f9731a16-262f-4db8-96f3-31ec03128917" />

### /spin
<img width="346" height="332" alt="image" src="https://github.com/user-attachments/assets/f03248ae-e7c3-42ec-a802-c05101dd12f2" />

### /ticket-nyitas
<img width="664" height="475" alt="image" src="https://github.com/user-attachments/assets/12160753-5b8b-46a1-b96a-9b09fc738e0d" />

### /ticket-panel
<img width="478" height="339" alt="image" src="https://github.com/user-attachments/assets/a48a5b4f-704b-493f-be13-3c4629050db6" />

### /add
<img width="368" height="116" alt="image" src="https://github.com/user-attachments/assets/825409a7-e4d9-4c81-8319-30bb48c4b699" />

### /admin-profile
<img width="698" height="765" alt="image" src="https://github.com/user-attachments/assets/aa27d98b-d44c-4d9a-8065-ac8b16e77669" />

>  Yes I know it's ugly

### /high-ticket-panel
<img width="615" height="435" alt="image" src="https://github.com/user-attachments/assets/d08e1194-6fee-475a-9c62-5c94fbe8e45d" />

### /queue-panel
<img width="615" height="521" alt="image" src="https://github.com/user-attachments/assets/0b2ac0f9-163d-4499-ae23-fa0183168e1b" />

### /queue-ping
<img width="578" height="374" alt="image" src="https://github.com/user-attachments/assets/8beb7029-5ee8-455f-81db-f85d51d51d4c" />

---

## How to use the moderation system
To set up a filter, you need to navigate to `resources/filter.json` then create a field inside of the `flags` array. You must set the following fields in the flag:
- "name": "The name of the flag"
- "description": "A description of the flag"
- "punishment": "A punishment that ranges from NONE, so nothing to a KICK, or a BAN or a SEVEN_DAY cooldown"
- "flags": "An an array of strings to flag"
- "ignore": "An an array of strings not to flag (for example, youtube.com is in ignore and :// is in flags, this lets you send https://youtube.com and flags other websites)"
- "always": "An array of strings to flag no matter what"

> What a flag looks like:
<img width="633" height="289" alt="image" src="https://github.com/user-attachments/assets/4a1d398e-5e63-4e6e-ad09-debfbc356031" />

> You can see the blocked message and punish the user accordingly.

---

## Command syntax
> I'll paste here a bunch of lines generated by claude that shows the syntax because it's too late already :C
```
/ticket-nyitas
/ticket-panel
/profile {Minecraft name|Discord user}
/queue-ping
/queue-panel
/high-ticket-panel
/spin {Gamemode} {Tier}
/admin-profile {Minecraft name|Discord user}
/add {Discord user}
```
> Well I could've done that

---

## Where to test the bot
The bot is always running at [our discord server](https://dc.ranked.hu).
