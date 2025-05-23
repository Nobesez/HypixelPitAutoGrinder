package me.mitfox.betterpit;

import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

public class FakeGui extends GuiScreen {

    public void func_73869_a(char t, int k) {
        if (k == Keyboard.KEY_H) {
            this.field_146297_k.func_147108_a(null);
            PitBotMod.toggleBot(false);
        }
    }
}