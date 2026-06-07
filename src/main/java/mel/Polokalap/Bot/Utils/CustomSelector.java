package mel.Polokalap.Bot.Utils;

import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import java.util.ArrayList;
import java.util.List;

public class CustomSelector {

    private String id;
    private String placeholder;
    private List<SelectOption> options;

    public CustomSelector(String id, String placeholder) {

        this.id = id;
        this.placeholder = placeholder;
        this.options = new ArrayList<>();

    }

    public void addOption(String label, String value) {

        options.add(SelectOption.of(label, value));

    }

    public void addOption(String label, String value, Emoji emoji) {

        options.add(SelectOption.of(label, value).withEmoji(emoji));

    }

    public StringSelectMenu getMenu() {

        StringSelectMenu.Builder builder = StringSelectMenu.create(id)
                .setPlaceholder(placeholder);

        for (SelectOption option : options) {

            builder.addOptions(option);

        }

        return builder.build();

    }

}
