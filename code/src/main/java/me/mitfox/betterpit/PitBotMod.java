package me.mitfox.betterpit;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

@Mod(modid = "betterpit", version = "1.0")
public class PitBotMod
{
    public static Bot bot;
    public static FakeGui Gui = new FakeGui();
    public static Minecraft mc = Minecraft.getMinecraft();
    public static boolean botenabled = false;
    
    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        MinecraftForge.EVENT_BUS.register(this);
    }


    //toggling
    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent e) {
        if (Utils.inGame()) {
            if (Keyboard.isKeyDown(Keyboard.KEY_H)) {
                if (!botenabled) {
                    toggleBot(true);
                }
            }
        }
    }

    public static void toggleBot(boolean s) {
        if (s) {
            bot = new Bot();
            MinecraftForge.EVENT_BUS.register(bot);
            mc.displayGuiScreen(Gui);
        } else {
            if (bot != null) {
                bot.stopwalking();
                MinecraftForge.EVENT_BUS.unregister(bot);
                bot = null;
            }
        }
        botenabled = s;
        Utils.sendMessage("Bot toggled "+ (s ? "ON" : "OFF"));
    }
}