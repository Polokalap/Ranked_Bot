package mel.Polokalap.Bot.Listeners.Profile;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mel.Polokalap.Bot.Utils.Database;
import mel.Polokalap.Bot.Utils.QueueUtil;
import mel.Polokalap.Bot.Utils.TestResult;
import mel.Polokalap.Bot.Utils.Tiers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.modals.Modal;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static mel.Polokalap.Bot.Listeners.Profile.AdminProfileCommandListener.buildProfileEmbed;
import static mel.Polokalap.Bot.Main.*;
import static mel.Polokalap.Bot.Utils.QueueUtil.*;

public class AdminProfilSelectorButtonListener extends ListenerAdapter {

    public static HashMap<Message, Integer> adminGamemode = new HashMap<>();
    public static HashMap<String, JsonObject> adminJson = new HashMap<>();

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {

        JsonObject commands = lang.get("commands").getAsJsonObject();
        JsonObject profile = commands.get("admin-profile").getAsJsonObject();
        String componentId = event.getComponentId();

        if (!componentId.equals("admin-command-gamemode-selector")) return;

        Member member = event.getMember();

        event.deferReply(true).queue();

        String selected = event.getValues().get(0);
        int actualId = Integer.parseInt(selected.replace("admin-command-gamemode-", "")) - 1;

        String gamemodeName = gamemodes.get(actualId).getAsJsonObject().get("html").getAsString();
        String emoji = Emoji.fromCustom(
                data.get("gamemodes").getAsJsonArray().get(actualId).getAsJsonObject().get("name").getAsString(),
                data.get("gamemodes").getAsJsonArray().get(actualId).getAsJsonObject().get("id").getAsLong(),
                false
        ).getAsMention();

        adminGamemode.put(event.getMessage(), actualId);

        event.getHook().sendMessage(
                profile.get("gamemode-selected").getAsString()
                    .replace("%gamemode%", emoji + " " + gamemodeName)
        ).setEphemeral(true).queue();

    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {

        String id = event.getComponentId();
        JsonObject commands = lang.get("commands").getAsJsonObject();
        JsonObject profile = commands.get("admin-profile").getAsJsonObject();
        Member member = event.getMember();
        Message message = event.getMessage();
        Guild guild = event.getGuild();

        if (
                !id.startsWith("admin-command-remove-cooldown-") &&
                !id.startsWith("admin-command-unban-") &&
                !id.startsWith("admin-command-ban-") &&
                !id.startsWith("admin-command-make-tester-") &&
                !id.startsWith("admin-command-revoke-tester-") &&
                !id.startsWith("admin-command-set-tier-") &&
                !id.startsWith("admin-command-remove-tier-") &&
                !id.startsWith("admin-command-set-retired-") &&
                !id.startsWith("admin-command-remove-retired-")
        ) return;

        if (id.startsWith("admin-command-unban-")) {

            event.deferReply(true).queue();

            long userId = Long.parseLong(id.replace("admin-command-unban-", ""));

            Database.execute(
                    "UPDATE players SET banned = ? WHERE discord_id = ?;",
                    false,
                    String.valueOf(userId)
            );

            event.getHook().sendMessage(
                    profile.get("unbanned").getAsString()
                            .replace("%player%", "<@" + userId + ">")
            ).setEphemeral(true).queue();

        }

        if (id.startsWith("admin-command-ban-")) {

            event.deferReply(true).queue();

            long userId = Long.parseLong(id.replace("admin-command-unban-", ""));

            Database.execute(
                    "UPDATE players SET banned = ? WHERE discord_id = ?;",
                    false,
                    String.valueOf(userId)
            );

            event.getHook().sendMessage(
                    profile.get("banned").getAsString()
                            .replace("%player%", "<@" + userId + ">")
            ).setEphemeral(true).queue();

        }

        int actualId = adminGamemode.getOrDefault(message, -1);

        if (actualId == -1) {

            event.getHook().sendMessage(profile.get("no-gamemode").getAsString()).setEphemeral(true).queue();
            return;

        }

        int storedId = gamemodes.get(actualId).getAsJsonObject().get("stored").getAsInt();

        String gamemodeName = gamemodes.get(actualId).getAsJsonObject().get("html").getAsString();
        String emoji = Emoji.fromCustom(
                data.get("gamemodes").getAsJsonArray().get(actualId).getAsJsonObject().get("name").getAsString(),
                data.get("gamemodes").getAsJsonArray().get(actualId).getAsJsonObject().get("id").getAsLong(),
                false
        ).getAsMention();

        if (id.startsWith("admin-command-remove-cooldown-")) {

            event.deferReply(true).queue();

            long userId = Long.parseLong(id.replace("admin-command-remove-cooldown-", ""));

            Database.execute(
                    "UPDATE players SET last_tested = jsonb_set(COALESCE(last_tested, '{}'::jsonb), ARRAY[?]::text[], to_jsonb(?::bigint), true) WHERE discord_id = ?;",
                    String.valueOf(actualId),
                    0L,
                    String.valueOf(userId)
            );

            event.getHook().sendMessage(
                    profile.get("removed-cooldown").getAsString()
                            .replace("%gamemode%", emoji + " " + gamemodeName)
                            .replace("%player%", "<@" + userId + ">")
            ).setEphemeral(true).queue();

        }

        if (id.startsWith("admin-command-make-tester-")) {

            event.deferReply(true).queue();

            long userId = Long.parseLong(id.replace("admin-command-make-tester-", ""));

            Database.execute(
                    "UPDATE players SET tester = jsonb_set(COALESCE(tester, '{}'::jsonb), ARRAY[?]::text[], to_jsonb(?::boolean), true) WHERE discord_id = ?;",
                    String.valueOf(actualId),
                    true,
                    String.valueOf(userId)
            );

            event.getHook().sendMessage(
                    profile.get("made-tester").getAsString()
                            .replace("%gamemode%", emoji + " " + gamemodeName)
                            .replace("%player%", "<@" + userId + ">")
            ).setEphemeral(true).queue();

        }

        if (id.startsWith("admin-command-revoke-tester-")) {

            event.deferReply(true).queue();

            long userId = Long.parseLong(id.replace("admin-command-revoke-tester-", ""));

            Database.execute(
                    "UPDATE players SET tester = jsonb_set(COALESCE(tester, '{}'::jsonb), ARRAY[?]::text[], to_jsonb(?::boolean), true) WHERE discord_id = ?;",
                    String.valueOf(actualId),
                    false,
                    String.valueOf(userId)
            );

            event.getHook().sendMessage(
                    profile.get("revoked-tester").getAsString()
                            .replace("%gamemode%", emoji + " " + gamemodeName)
                            .replace("%player%", "<@" + userId + ">")
            ).setEphemeral(true).queue();

        }

        if (id.startsWith("admin-command-set-tier-")) {

            long userId = Long.parseLong(id.replace("admin-command-set-tier-", ""));
            JsonObject modalJson = profile.get("modal").getAsJsonObject();

            TextInput tierInput = TextInput.create("tier", TextInputStyle.SHORT)
                    .setMaxLength(3)
                    .setPlaceholder(modalJson.get("placeholder").getAsString())
                    .build();

            Modal modal = Modal.create(
                            "admin-command-set-tier-modal-" + userId,
                            modalJson.get("title").getAsString()
                    )
                    .addComponents(
                            Label.of(modalJson.get("text").getAsString(), tierInput)
                    )
                    .build();

            event.replyModal(modal).queue();

        }
        if (id.startsWith("admin-command-revoke-tester-")) {

            event.deferReply(true).queue();

            long userId = Long.parseLong(id.replace("admin-command-revoke-tester-", ""));

            Database.execute(
                    "UPDATE players SET tester = jsonb_set(COALESCE(tester, '{}'::jsonb), ARRAY[?]::text[], to_jsonb(?::boolean), true) WHERE discord_id = ?;",
                    String.valueOf(actualId),
                    false,
                    String.valueOf(userId)
            );

            event.getHook().sendMessage(
                    profile.get("revoked-tester").getAsString()
                            .replace("%gamemode%", emoji + " " + gamemodeName)
                            .replace("%player%", "<@" + userId + ">")
            ).setEphemeral(true).queue();

        }

        if (id.startsWith("admin-command-remove-tier-")) {

            event.deferReply(true).queue();

            long userId = Long.parseLong(id.replace("admin-command-remove-tier-", ""));

            Database.execute(
                    "UPDATE players SET elos = jsonb_delete_path(elos, ARRAY[?::text]) WHERE discord_id = ?",
                    actualId + 1,
                    String.valueOf(userId)
            );

            event.getHook().sendMessage(
                    profile.get("removed-tier").getAsString()
                            .replace("%gamemode%", emoji + " " + gamemodeName)
                            .replace("%player%", "<@" + userId + ">")
            ).setEphemeral(true).queue();

        }

        if (id.startsWith("admin-command-set-retired-")) {

            event.deferReply(true).queue();

            long userId = Long.parseLong(id.replace("admin-command-set-retired-", ""));

            Database.execute(
                    "UPDATE players SET retired = jsonb_set(COALESCE(tester, '{}'::jsonb), ARRAY[?]::text[], to_jsonb(?::boolean), true) WHERE discord_id = ?;",
                    String.valueOf(storedId),
                    true,
                    String.valueOf(userId)
            );

            event.getHook().sendMessage(
                    profile.get("set-retired").getAsString()
                            .replace("%gamemode%", emoji + " " + gamemodeName)
                            .replace("%player%", "<@" + userId + ">")
            ).setEphemeral(true).queue();

        }

        if (id.startsWith("admin-command-remove-retired-")) {

            event.deferReply(true).queue();

            long userId = Long.parseLong(id.replace("admin-command-remove-retired-", ""));

            Database.execute(
                    "UPDATE players SET retired = jsonb_set(COALESCE(tester, '{}'::jsonb), ARRAY[?]::text[], to_jsonb(?::boolean), true) WHERE discord_id = ?;",
                    String.valueOf(storedId),
                    false,
                    String.valueOf(userId)
            );

            event.getHook().sendMessage(
                    profile.get("set-unretired").getAsString()
                            .replace("%gamemode%", emoji + " " + gamemodeName)
                            .replace("%player%", "<@" + userId + ">")
            ).setEphemeral(true).queue();

        }

    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {

        JsonObject commands = lang.get("commands").getAsJsonObject();
        JsonObject profile = commands.get("admin-profile").getAsJsonObject();
        String id = event.getModalId();
        Guild guild = event.getGuild();
        Member member = event.getMember();
        Message message = event.getMessage();

        if (!id.startsWith("admin-command-set-tier-modal-")) return;

        int actualId = adminGamemode.getOrDefault(message, -1);
        long userId = Long.parseLong(id.replace("admin-command-set-tier-modal-", ""));
        String tier = event.getValue("tier").getAsString().toUpperCase();

        String gamemodeName = gamemodes.get(actualId).getAsJsonObject().get("html").getAsString();
        String emoji = Emoji.fromCustom(
                data.get("gamemodes").getAsJsonArray().get(actualId).getAsJsonObject().get("name").getAsString(),
                data.get("gamemodes").getAsJsonArray().get(actualId).getAsJsonObject().get("id").getAsLong(),
                false
        ).getAsMention();

        if (!List.of("LT5", "HT5", "LT4", "HT4", "LT3", "HT3", "LT2", "HT2", "LT1", "HT1").contains(tier)) {

            event.getHook().sendMessage(profile.get("invalid-tier").getAsString()).queue();
            return;

        }

        Database.execute(
                "UPDATE players SET elos = jsonb_set(elos, ARRAY[?::text], to_jsonb(?::text)) WHERE discord_id = ?",
                actualId + 1,
                tierToElo(Tiers.valueOf(tier), MAX),
                String.valueOf(userId)
        );

        event.deferEdit().queue();

        guild.retrieveMemberById(userId).queue(

                player -> {

                    event.getHook().sendMessage(
                            profile.get("set-tier").getAsString()
                                    .replace("%gamemode%", emoji + " " + gamemodeName)
                                    .replace("%player%", "<@" + userId + ">")
                    ).setEphemeral(true).queue();

                }

        );

    }

}
