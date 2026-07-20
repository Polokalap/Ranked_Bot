package mel.Polokalap.Bot.Listeners.Queue.Panel;

import com.google.gson.JsonObject;
import mel.Polokalap.Bot.Utils.QueueUtil;
import mel.Polokalap.Bot.Utils.TestResult;
import mel.Polokalap.Bot.Utils.Tiers;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.modals.Modal;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static mel.Polokalap.Bot.Main.*;
import static mel.Polokalap.Bot.Utils.QueueUtil.*;

public class QueueTestButtonListener extends ListenerAdapter {

    private static HashMap<Member, Integer> modalId = new HashMap<>();

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {

        String id = event.getComponentId();
        JsonObject queue = lang.get("queue").getAsJsonObject().get("give-tier-menu").getAsJsonObject();
        Member member = event.getMember();
        Guild guild = event.getGuild();

        if (id.startsWith("queue-give-tier-")) {

            int actualId = Integer.parseInt(id.replace("queue-give-tier-", ""));
            Member tester = getTester.get(actualId);

            if (!tester.equals(member) && !member.getPermissions().contains(Permission.MANAGE_SERVER)) {

                event.reply(queue.get("permission").getAsString()).setEphemeral(true).queue();
                return;

            }

            String gamemodeName = gamemodes.get(actualId).getAsJsonObject().get("html").getAsString();
            String emoji = Emoji.fromCustom(
                    data.get("gamemodes").getAsJsonArray().get(actualId).getAsJsonObject().get("name").getAsString(),
                    data.get("gamemodes").getAsJsonArray().get(actualId).getAsJsonObject().get("id").getAsLong(),
                    false
            ).getAsMention();

            TextInput tierInput = TextInput.create("tier", TextInputStyle.SHORT)
                    .setMaxLength(3)
                    .setPlaceholder(queue.get("placeholder").getAsString())
                    .build();

            Modal modal = Modal.create(
                            "tier-setting-menu-" + testing.get(member).getId(),
                            queue.get("title").getAsString()
                    )
                    .addComponents(
                            Label.of(queue.get("text").getAsString(), tierInput)
                    )
                    .build();

            modalId.put(member, actualId);

            event.replyModal(modal).queue();

        } else if (id.startsWith("queue-close-ticket-")) {

            int actualId = Integer.parseInt(id.replace("queue-close-ticket-", ""));

            QueueUtil.addCooldown(testing.get(getTester.get(actualId)), actualId);
            testing.remove(member);

            event.reply(queue.get("closed").getAsString()).setEphemeral(true).queue(

                    interactionHook -> {

                        event.getChannel().delete().queueAfter(3L, TimeUnit.SECONDS);

                    }

            );

        }

    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {

        JsonObject queue = lang.get("queue").getAsJsonObject().get("give-tier-menu").getAsJsonObject();
        String id = event.getModalId();
        Guild guild = event.getGuild();
        Member member = event.getMember();

        if (!id.startsWith("tier-setting-menu-")) return;

        long userId = Long.parseLong(id.replace("tier-setting-menu-", ""));
        String tier = event.getValue("tier").getAsString().toUpperCase();

        event.deferReply(true).queue();

        if (!List.of("LT5", "HT5", "LT4", "HT4", "LT3").contains(tier)) {

            event.getHook().sendMessage(queue.get("invalid-tier").getAsString()).queue();
            return;

        }

        String emoji = Emoji.fromCustom(
                data.get("gamemodes").getAsJsonArray().get(modalId.get(member)).getAsJsonObject().get("name").getAsString(),
                data.get("gamemodes").getAsJsonArray().get(modalId.get(member)).getAsJsonObject().get("id").getAsLong(),
                false
        ).getAsMention();

        guild.retrieveMemberById(userId).queue(

                player -> {

                    long channelId = data.get("logs-channel").getAsLong();
                    member.getGuild().getTextChannelById(channelId)
                            .sendMessage(
                                    lang.get("log").getAsJsonObject().get("message").getAsString()
                                            .replace("%admin%", member.getAsMention())
                                            .replace("%player%", player.getAsMention())
                                            .replace("%gamemode%", emoji)
                                            .replace("%tier%", tier)
                            ).queue();

                    TestResult.anounceTest(member, player, guild, modalId.get(member), Tiers.valueOf(tier));
                    QueueUtil.setTier(player, modalId.get(member), Tiers.valueOf(tier));
                    QueueUtil.addCooldown(player, modalId.get(member));
                    testing.remove(member);
                    event.getHook().sendMessage(queue.get("tier-given").getAsString()).setEphemeral(true).queue();
                    event.getChannel().delete().queueAfter(3L, TimeUnit.SECONDS);

                },

                error -> {

                    event.getHook().sendMessage(queue.get("player-not-found").getAsString()).setEphemeral(true).queue();

                }

        );

    }

}
