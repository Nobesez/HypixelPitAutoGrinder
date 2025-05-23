package me.mitfox.betterpit;

import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

public class FakeGui extends GuiScreen {

    public void keyTyped(char t, int k) {
        if (k == Keyboard.KEY_H) {
            this.mc.displayGuiScreen(null);
            PitBotMod.toggleBot(false);
        }
    }
}