package mel.Polokalap.Bot.Utils;

import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

public class CustomButton {

    private String text;
    private String id;
    private ButtonStyle style;

    public CustomButton(String text, String id, ButtonStyle style) {

        this.text = text;
        this.id = id;
        this.style = style;

    }

    public Button getButton() {

        return Button.of(style, id, text);

    }

}
