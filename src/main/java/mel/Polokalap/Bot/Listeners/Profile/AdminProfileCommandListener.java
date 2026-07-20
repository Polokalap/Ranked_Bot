package mel.Polokalap.Bot.Listeners.Profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mel.Polokalap.Bot.Utils.CustomButton;
import mel.Polokalap.Bot.Utils.CustomSelector;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static mel.Polokalap.Bot.Listeners.Profile.AdminProfilSelectorButtonListener.adminJson;
import static mel.Polokalap.Bot.Main.*;
import static mel.Polokalap.Bot.Main.gamemodes;

public class AdminProfileCommandListener extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

        JsonObject commands = lang.get("commands").getAsJsonObject();
        JsonObject profile = commands.get("admin-profile").getAsJsonObject();
        String name = profile.get("name").getAsString();
        Guild guild = event.getGuild();
        Member member = event.getMember();

        if (!event.getName().equals(name)) return;

        event.deferReply(true).queue();

        Role managerRole = guild.getRoleById(data.get("manager-role").getAsLong());

        if (!member.getRoles().contains(managerRole)) {

            event.getHook().sendMessage(
                    profile.get("permission").getAsString()
            ).setEphemeral(true).queue();
            return;

        }

        String minecraftName = event.getOption("minecraft_name") != null
                ? event.getOption("minecraft_name").getAsString()
                : "";

        OptionMapping playerOption = event.getOption("player");
        Member player = playerOption != null ? playerOption.getAsMember() : null;

        if (player == null && minecraftName.isEmpty()) {

            event.getHook().sendMessage(profile.get("missing").getAsString()).setEphemeral(true).queue();
            return;

        }

        String requestType = player == null ? "name" : "discord_id";
        String requestText = player == null ? minecraftName : player.getId();

        JsonObject profileEmbed = profile.get("embed").getAsJsonObject();

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.ranked.hu/v1/player?" + requestType + "=" + requestText))
                .GET()
                .build();

        HttpResponse<String> response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (response.statusCode() >= 400 && response.statusCode() <= 500) {

            event.getHook().sendMessage(profile.get("fail").getAsString()).setEphemeral(true).queue();
            return;

        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonObject actions = profileEmbed.get("actions").getAsJsonObject();
        EmbedBuilder embed = buildProfileEmbed(json);

        CustomSelector gamemodeSelector = new CustomSelector(
                "admin-command-gamemode-selector",
                actions.get("selector").getAsJsonObject().get("text").getAsString()
        );

        int gamemodeId = 0;

        for (JsonElement element : gamemodes) {

            gamemodeId = element.getAsJsonObject().get("id").getAsInt() - 1;

            String gamemodeName = element.getAsJsonObject().get("html").getAsString();
            Emoji emoji = Emoji.fromCustom(
                    data.get("gamemodes").getAsJsonArray().get(gamemodeId).getAsJsonObject().get("name").getAsString(),
                    data.get("gamemodes").getAsJsonArray().get(gamemodeId).getAsJsonObject().get("id").getAsLong(),
                    false
            );

            gamemodeSelector.addOption(gamemodeName, "admin-command-gamemode-" + element.getAsJsonObject().get("id").getAsString(), emoji);

        }

        CustomButton removeCooldownButton = new CustomButton(
                actions.get("remove-cooldown").getAsJsonObject().get("text").getAsString(),
                "admin-command-remove-cooldown-" + json.get("discord_id").getAsLong(),
                ButtonStyle.valueOf(actions.get("remove-cooldown").getAsJsonObject().get("style").getAsString())
        );

        CustomButton unBanButton = new CustomButton(
                actions.get("unban-user").getAsJsonObject().get("text").getAsString(),
                "admin-command-unban-" + json.get("discord_id").getAsLong(),
                ButtonStyle.valueOf(actions.get("unban-user").getAsJsonObject().get("style").getAsString())
        );

        CustomButton banButton = new CustomButton(
                actions.get("ban-user").getAsJsonObject().get("text").getAsString(),
                "admin-command-ban-" + json.get("discord_id").getAsLong(),
                ButtonStyle.valueOf(actions.get("ban-user").getAsJsonObject().get("style").getAsString())
        );

        CustomButton addTesterButton = new CustomButton(
                actions.get("make-tester").getAsJsonObject().get("text").getAsString(),
                "admin-command-make-tester-" + json.get("discord_id").getAsLong(),
                ButtonStyle.valueOf(actions.get("make-tester").getAsJsonObject().get("style").getAsString())
        );

        CustomButton revokeTesterButton = new CustomButton(
                actions.get("revoke-tester").getAsJsonObject().get("text").getAsString(),
                "admin-command-revoke-tester-" + json.get("discord_id").getAsLong(),
                ButtonStyle.valueOf(actions.get("revoke-tester").getAsJsonObject().get("style").getAsString())
        );

        CustomButton setTierButton = new CustomButton(
                actions.get("set-tier").getAsJsonObject().get("text").getAsString(),
                "admin-command-set-tier-" + json.get("discord_id").getAsLong(),
                ButtonStyle.valueOf(actions.get("set-tier").getAsJsonObject().get("style").getAsString())
        );

        CustomButton removeTierButton = new CustomButton(
                actions.get("remove-tier").getAsJsonObject().get("text").getAsString(),
                "admin-command-remove-tier-" + json.get("discord_id").getAsLong(),
                ButtonStyle.valueOf(actions.get("remove-tier").getAsJsonObject().get("style").getAsString())
        );

        CustomButton setRetiredButton = new CustomButton(
                actions.get("set-retired").getAsJsonObject().get("text").getAsString(),
                "admin-command-set-retired-" + json.get("discord_id").getAsLong(),
                ButtonStyle.valueOf(actions.get("set-retired").getAsJsonObject().get("style").getAsString())
        );

        CustomButton setUnRetiredButton = new CustomButton(
                actions.get("set-unretired").getAsJsonObject().get("text").getAsString(),
                "admin-command-remove-retired-" + json.get("discord_id").getAsLong(),
                ButtonStyle.valueOf(actions.get("set-unretired").getAsJsonObject().get("style").getAsString())
        );

        CustomButton setDefenseButton = new CustomButton(
                actions.get("set-defense").getAsJsonObject().get("text").getAsString(),
                "admin-command-set-defense-" + json.get("discord_id").getAsLong(),
                ButtonStyle.valueOf(actions.get("set-defense").getAsJsonObject().get("style").getAsString())
        );

        event.getHook().sendMessageEmbeds(
                embed.build()
        ).addComponents(
                ActionRow.of(
                        removeCooldownButton.getButton()
                ),
                ActionRow.of(
                        unBanButton.getButton(),
                        banButton.getButton()
                ),
                ActionRow.of(
                        addTesterButton.getButton(),
                        revokeTesterButton.getButton(),
                        setRetiredButton.getButton(),
                        setUnRetiredButton.getButton()
                ),
                ActionRow.of(
                        setTierButton.getButton(),
                        removeTierButton.getButton(),
                        setDefenseButton.getButton()
                ),
                ActionRow.of(
                        gamemodeSelector.getMenu()
                )
        ).queue(

                message -> {

                    adminJson.put(message.getId(), json);

                }

        );

    }

    public static EmbedBuilder buildProfileEmbed(JsonObject json) {

        JsonObject commands = lang.get("commands").getAsJsonObject();
        JsonObject profile = commands.get("admin-profile").getAsJsonObject();
        JsonObject profileEmbed = profile.get("embed").getAsJsonObject();

        EmbedBuilder embed = new EmbedBuilder();
        StringBuilder descriptionBuilder = new StringBuilder();

        for (JsonElement line : profileEmbed.get("description").getAsJsonArray()) {

            descriptionBuilder.append(line.getAsString() + "\n");

        }
        String description = descriptionBuilder
                .toString()
                .replace("%discord%", "<@" + json.get("discord_id").getAsString() + ">")
                .replace("%position%", json.get("position").getAsString())
                .replace("%score%", json.get("points").getAsString());

        JsonArray alertColors = profileEmbed.getAsJsonObject().get("color").getAsJsonArray();
        Color alertColor = new Color(
                alertColors.get(0).getAsInt(),
                alertColors.get(1).getAsInt(),
                alertColors.get(2).getAsInt()
        );

        embed.setTitle(profileEmbed.get("title").getAsString().replace("%player%", json.get("name").getAsString()));
        embed.setDescription(description);

        int gamemodeId = 0;
        JsonObject tiers = data.get("tiers").getAsJsonObject();

        for (JsonElement emoji : data.get("gamemodes").getAsJsonArray()) {

            int storedId = gamemodes.get(gamemodeId).getAsJsonObject().get("stored").getAsInt();
            JsonObject obj = emoji.getAsJsonObject();
            String tier = json.get("tiers").getAsJsonObject().get(String.valueOf(gamemodeId + 1)).getAsString();

            boolean retired;
            int defense;
            boolean isTester;

            if (json.get("retired").getAsJsonObject().get(String.valueOf(storedId)) != null) retired = json.get("retired").getAsJsonObject().get(String.valueOf(storedId)).getAsBoolean();
            else retired = false;

            if (json.get("defense").getAsJsonObject().get(String.valueOf(gamemodeId)) != null) defense = json.get("defense").getAsJsonObject().get(String.valueOf(gamemodeId)).getAsInt();
            else defense = 0;

            if (json.get("tester").getAsJsonObject().get(String.valueOf(gamemodeId)) != null) isTester = json.get("tester").getAsJsonObject().get(String.valueOf(gamemodeId)).getAsBoolean();
            else isTester = false;

            if (tier.isEmpty()) tier = "unranked";

            JsonArray tierArray = tiers.get((retired ? "r" : "") + tier.toLowerCase()).getAsJsonArray();

            String fieldValue = Emoji.fromCustom(
                    tierArray.get(0).getAsJsonObject().get("name").getAsString(),
                    tierArray.get(0).getAsJsonObject().get("id").getAsLong(),
                    false
            ).getAsMention().concat(Emoji.fromCustom(
                    tierArray.get(1).getAsJsonObject().get("name").getAsString(),
                    tierArray.get(1).getAsJsonObject().get("id").getAsLong(),
                    false
            ).getAsMention() + " " + "(" + String.valueOf(defense) + ") (" + isTester + ")");

            embed.addField(
                    Emoji.fromCustom(obj.get("name").getAsString(), obj.get("id").getAsLong(), false).getAsMention()
                            + " " +
                            gamemodes.getAsJsonArray().get(gamemodeId).getAsJsonObject().get("html").getAsString(),
                    fieldValue,
                    true
            );

            gamemodeId++;

        }

        while (embed.getFields().size() % 3 != 0) {

            embed.addField("", "", true);

        }

        embed.setColor(alertColor);
        embed.setFooter(profileEmbed.get("footer").getAsString());
        embed.setThumbnail("https://nmsr.jgj52.dev/bust/" + json.get("uuid").getAsString() + "?w=128");

        return embed;

    }

}
