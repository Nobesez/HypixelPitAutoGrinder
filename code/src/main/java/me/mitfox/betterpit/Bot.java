package me.mitfox.betterpit;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S40PacketDisconnect;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;
import java.util.Random;

import static me.mitfox.betterpit.Bot.Phase.*;
import static me.mitfox.betterpit.PitBotMod.*;
import static me.mitfox.betterpit.Utils.*;

public class Bot {
    public enum Phase {WALKING, FALLING, FIGHTING, GAPPING, WAITING, PANIC}
    private BlockPos center;
    private Phase phase;
    private EntityPlayer target;
    private double reach = 35;
    private double attackreach = 4;
    private double[] smoothness = {7.9,9};
    private double jumpchance = 0.02;
    private double strafechance = 0.02;
    private int phasetick = 0;
    private double cps = 7;
    private int loop = 0;
    private double[] ranvals;
    private int sprintresticks = 50;
    private double waitchance = 0.3;
    private int waitticks = 0;
    private int screenshotticks = 0;
    private int gheaddelay = 50;
    private int spawny;
    private double switchtargetrange = 1;
    private int mentions = 0;
    private int sessionstop = 20;
    private double reachskip = 1;
    private boolean quitbot = false;
    private int tickscolided = 0;
    private int maxtickscolided = 160;
    private double distancesq = 10;
    private int sametargetticks = 0;
    private boolean sessionpause;
    private int randomsprintresticks;
    private int currectsession;
    private int gheadtickdelay;
    private int fallticks;
    private long session;

    public Bot() {
        this.phase = WALKING;
        this.center = new BlockPos(0,mc.thePlayer.posY-8,0);
        this.ranvals = new double[]{new Random().nextInt(15), new Random().nextInt(15), new Random().nextInt(15), Utils.getRandom(0.5,0.95), Utils.getRandom(0.85,0.95)};
        this.session = System.currentTimeMillis();
        this.fallticks = (int) Utils.getRandom(5,30);
        this.spawny = (int) mc.thePlayer.posY;
        this.sessionpause = false;
        this.randomsprintresticks = (int) Utils.getRandom(sprintresticks - 20, sprintresticks + 40);
        loadConfig();
        this.currectsession = sessionstop;
        this.gheadtickdelay = gheaddelay;
    }

    public void walking() {
        //calculates angle to look at a center block
        //looks at center block
        //walks to the center block

        //AIMING
        if (phasetick > ranvals[1] && mc.thePlayer.getDistanceSq(center) > distancesq) {
            float[] angles = getFixedRotation(Utils.smoothRotations(Utils.getAimRotations(center), ranvals[3], ranvals[4]), new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch});

            mc.thePlayer.rotationYaw = angles[0];
            if (mc.thePlayer.onGround) {
                mc.thePlayer.rotationPitch = angles[1];
            }
        }

        //PANIC MODE
        if (phasetick > 240) {
            stopwalking();
            setPhase(PANIC);
        }



        //WALKING
        if (phasetick==15+ranvals[0]) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
        } else if (phasetick == 40+ranvals[1]) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
        } else if (phasetick == 70+ranvals[2]) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), true);
        }

        //NEXT PHASE
        if (mc.thePlayer.fallDistance>10) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
            setPhase(FALLING);
        }
    }

    public void waiting() {
        //waits

        if (iswalking()) {
            stopwalking();
        }

        //SESSIONPAUSE
        if (sessionpause) {
            if (waitticks < phasetick) {
                mc.thePlayer.sendChatMessage("/play pit");
                sessionpause = false;
                waitticks = phasetick+200;
                return;
            }
        }

        if (waitticks < phasetick) {
            setPhase(WALKING);
        }
    }

    public void fighting(TickEvent.ClientTickEvent e) {
        //switches to sword slot if needed, if have gaps switches to gap slot and goes to GAPPING
        //gets closest target
        //aims at closest target
        //attacks closest target (legit)
        //random jumps and strafes

        //SLOTS
        for (int slot = 8; slot >= 0; slot--) {
            ItemStack itemInSlot = mc.thePlayer.inventory.getStackInSlot(slot);
            if (itemInSlot != null) {
                if ((Utils.isGoldenApple(itemInSlot) || Utils.isGoldenHead(itemInSlot) || Utils.isSoup(itemInSlot) || Utils.isBakedPotato(itemInSlot)) || Utils.isOlympusPotion(itemInSlot) && mc.thePlayer.getHealth() != mc.thePlayer.getMaxHealth() && gheadtickdelay == 0) {
                    setPhase(GAPPING);
                    mc.thePlayer.inventory.currentItem = slot;
                    return;
                } else if (itemInSlot.getItem() instanceof ItemSword) {
                    if (mc.thePlayer.getHeldItem() != null) {
                        if (!(mc.thePlayer.getHeldItem().getItem() instanceof ItemSword)) {
                            mc.thePlayer.inventory.currentItem = slot;
                            return;
                        }
                    } else {
                        mc.thePlayer.inventory.currentItem = slot;
                        return;
                    }
                }
            }
        }

        //To avoid looking suspicious
        if (tickscolided == maxtickscolided) {
            target = Utils.GetTarget(reach,attackreach,target);
        } else if (tickscolided == maxtickscolided*2) {
            spawncommand();
            tickscolided = 0;
        }
        if (mc.thePlayer.isCollidedHorizontally) {
            tickscolided++;
        } else {
            tickscolided = 0;
        }


        //JUMPING
        Random random = new Random();
        if (random.nextDouble() <= jumpchance || (iswalking() && mc.thePlayer.isCollidedHorizontally)) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), true);
            javax.swing.Timer timer = new javax.swing.Timer(100, actionevent -> {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
            });
            timer.setRepeats(false);
            timer.start();

        }

        //STRAFING
        if (random.nextDouble() <= strafechance) {
            if (random.nextDouble() <= strafechance / 2) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), true);
                javax.swing.Timer timer = new javax.swing.Timer((int) getRandom(200,700), actionevent -> {
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), false);
                });
                timer.setRepeats(false);
                timer.start();
            } else {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), true);
                javax.swing.Timer timer = new javax.swing.Timer((int) getRandom(200,700), actionevent -> {
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), false);
                });
                timer.setRepeats(false);
                timer.start();
            }

        }

        //blockhit fix
        if(mc.thePlayer.isUsingItem()) {
            mc.thePlayer.stopUsingItem();
            return;
        }


        //TARGETING
        EntityPlayer newtarget = Utils.GetTarget(reach,attackreach,null);
        if (target!=null) {
            if (newtarget!=null) {
                if (target==newtarget) {
                    sametargetticks++;
                } else {
                    sametargetticks=0;
                    if (mc.thePlayer.getDistanceToEntity(target) > mc.thePlayer.getDistanceToEntity(newtarget) + switchtargetrange || target.isDead || target.isInvisible() || (mc.thePlayer.getDistanceToEntity(newtarget) < attackreach && hasDiamondChestplate(target))) {
                        target = newtarget;
                    }
                }
            }
        } else {
            target = newtarget;
        }

        //To avoid getting stuck at NPC
        if (sametargetticks>=500) {
            sametargetticks=0;
            target = Utils.GetTarget(reach,attackreach,target);
        }

        //AIMING
        if (target==null){
            stopwalking();
        } else if (mc.thePlayer.getDistanceToEntity(target) >= reachskip) {
            Utils.aim(target, new double[]{smoothness[0], smoothness[1]});
        }

        //ATTACKING
        if (e.phase == TickEvent.Phase.START) {
            if (target!=null) {
                if (mc.thePlayer.getDistanceToEntity(target) <= attackreach+2) {
                    if (random.nextDouble() < cps / 20) {
                        mc.thePlayer.swingItem();
                        if (mc.objectMouseOver!=null&&mc.objectMouseOver.entityHit !=null) {
                            mc.playerController.attackEntity(mc.thePlayer, mc.objectMouseOver.entityHit);
                        }
                    }
                }
            }

            //RESETTING SPRINT
            if (target != null) {
                if (mc.thePlayer.getDistanceToEntity(target) <= attackreach) {
                    if (phasetick % randomsprintresticks == 0) {
                        KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
                        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);

                        randomsprintresticks = (int) getRandom(sprintresticks - 20, sprintresticks + 40);

                        javax.swing.Timer timer2 = new javax.swing.Timer(20, actionevent -> {
                            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
                            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
                        });
                        timer2.setRepeats(false);
                        timer2.start();
                    }
                }
            }
        }
    }

    public void gapping(TickEvent.ClientTickEvent e) {
        //eats gaps/gheads
        //goes to FIGHTING

        if (mc.thePlayer.getHeldItem()!=null) {
            if (Utils.isGoldenApple(mc.thePlayer.getHeldItem()) || Utils.isGoldenHead(mc.thePlayer.getHeldItem()) || Utils.isSoup(mc.thePlayer.getHeldItem()) || Utils.isOlympusPotion(mc.thePlayer.getHeldItem()) || Utils.isBakedPotato(mc.thePlayer.getHeldItem())){
                if (e.phase== TickEvent.Phase.START) {
                    if (mc.gameSettings.keyBindForward.isKeyDown()||mc.gameSettings.keyBindRight.isKeyDown()||mc.gameSettings.keyBindLeft.isKeyDown()) {
                        stopwalking();
                        return;
                    }
                    if (!mc.thePlayer.isUsingItem()) {
                        //baked_potato for pit event
                        if (Utils.isGoldenHead(mc.thePlayer.getHeldItem()) || Utils.isBakedPotato(mc.thePlayer.getHeldItem()) || Utils.isSoup(mc.thePlayer.getHeldItem())) {
                            mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                            startwalking();
                            setPhase(FIGHTING);
                            gheadtickdelay = gheaddelay;
                        } else if (Utils.isGoldenApple(mc.thePlayer.getHeldItem()) || Utils.isOlympusPotion(mc.thePlayer.getHeldItem())) {
                            mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem());
                        }
                    }
                }
            } else {
                startwalking();
                setPhase(FIGHTING);
            }
        } else {
            startwalking();
            setPhase(FIGHTING);
        }
    }

    public void panicking(TickEvent.ClientTickEvent e) {
        //tries to bypass staff check while looking legit

        if (phasetick > 200 && phasetick < 300) {
            float[] angles = getFixedRotation(Utils.smoothRotations(Utils.getAimRotations(new BlockPos(center.getX()+1,center.getY()-1,center.getZ())), 0.95, 0.95), new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch});
            mc.thePlayer.rotationYaw = angles[0];
            //mc.thePlayer.rotationPitch = angles[1];
        } else if (phasetick >= 350 && phasetick <= 600) {
            float[] angles = getFixedRotation(Utils.smoothRotations(Utils.getAimRotations(new BlockPos(center.getX()-1,center.getY()-1,center.getZ())), 0.95, 0.95), new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch});
            mc.thePlayer.rotationYaw = angles[0];
            //mc.thePlayer.rotationPitch = angles[1];
            if (phasetick == 360) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
            } else if (phasetick == 600) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
            }
        } else if (phasetick > 700) {
            if (quitbot) {
                mc.thePlayer.sendQueue.handleDisconnect(new S40PacketDisconnect(new ChatComponentText("quitting lol")));
                toggleBot(false);
            } else {
                sessionpause = true;
                currectsession += sessionstop;
                PauseSession();
            }
        }
    }

    public void falling() {
        //waits while falling

        if (phasetick>=fallticks) {
            setPhase(FIGHTING);
        }
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent e) {
        if (!Utils.inGame() || mc.thePlayer.isDead) {
            return;
        }

        //Loop reset & shit
        if (e.phase == TickEvent.Phase.END) {
            onUpdate();
        } else {
            if (mc.thePlayer.posY >= this.spawny && phasetick > 10 && getPhase() != WALKING && getPhase() != WAITING && getPhase() != PANIC) {
                sendMessage("Bot died!");
                stopwalking();
                loop++;
                phasetick = 0;
                sametargetticks = 0;
                fallticks = (int) Utils.getRandom(5, 20);
                target = null;
                this.ranvals = new double[]{new Random().nextInt(20), new Random().nextInt(30), new Random().nextInt(40), Utils.getRandom(0.5,0.95), Utils.getRandom(0.85,0.95)};
                this.center = new BlockPos(Utils.getRandom(-3, 3), mc.thePlayer.posY - Utils.getRandom(1, 10), Utils.getRandom(-3, 3));
                if (getPhase() == GAPPING) {
                    mc.thePlayer.stopUsingItem();
                    mc.thePlayer.inventory.currentItem = mc.thePlayer.inventory.currentItem % 8 + 1;
                }
                if (sessionpause) {
                    PauseSession();
                    return;
                }
                if (new Random().nextDouble() <= waitchance) {
                    waitticks = (int) Utils.getRandom(250, 500);
                    setPhase(WAITING);
                } else {
                    setPhase(WALKING);
                }
                return;
            }
        }

        phasetick++;
        if (gheadtickdelay>0) {
            gheadtickdelay--;
        }


        if (getPhase() == WAITING) {
            waiting();

        } else if (getPhase() == WALKING) {
            walking();

        } else if (getPhase()==FALLING) {
            falling();

        } else if (getPhase()==FIGHTING) {
            fighting(e);

        } else if (getPhase()==GAPPING) {
            gapping(e);

        } else if (getPhase()==PANIC) {
            panicking(e);
        }
    }



    @SubscribeEvent
    public void renderTick(TickEvent.RenderTickEvent e) {
        if (!inGame()){
            return;
        }
        if (mc.currentScreen==null) {
            mc.displayGuiScreen(Gui);
        }
        String t = "[ Pit Bot stats";
        String t2 = "[ Phase: "+phase.toString();
        String t3 = "[ Loop: "+loop;
        String t4 = "[ Session: "+Utils.formatTime((int) ((System.currentTimeMillis() - session) /1000));
        String t5 = "[ Mentions: "+mentions;
        ScaledResolution res = new ScaledResolution(mc);
        int x = res.getScaledWidth() / 2 - mc.fontRendererObj.getStringWidth(t) / 2;
        int y = res.getScaledHeight() / 2 + 15;
        mc.fontRendererObj.drawString(t, (float) x - 300, (float) y, Color.red.getRGB(), true);
        mc.fontRendererObj.drawString(t2, (float) x - 300, (float) y+10, Color.red.getRGB(), true);
        mc.fontRendererObj.drawString(t3, (float) x - 300, (float) y+20, Color.red.getRGB(), true);
        mc.fontRendererObj.drawString(t4, (float) x - 300, (float) y+30, Color.red.getRGB(), true);
        mc.fontRendererObj.drawString(t5, (float) x - 300, (float) y+40, Color.red.getRGB(), true);
    }

    @SubscribeEvent
    public void chat(ClientChatReceivedEvent e) {
        if (Utils.inGame()) {
            if (e.message.getUnformattedText().contains(mc.thePlayer.getName())) {
                mentions++;
                Utils.sendMessage("You got mentioned you in the chat!");
            }
        }
    }

    public void PauseSession() {
        mc.thePlayer.sendChatMessage("/l");
        waitticks = (int) Utils.getRandom(4500, 8000);
        setPhase(WAITING);
    }

    public void spawncommand() {
        waitticks = (int) Utils.getRandom(640, 800);
        setPhase(WAITING);
        javax.swing.Timer timer555 = new javax.swing.Timer(waitticks-40, actionevent -> {
            mc.thePlayer.sendChatMessage("/spawn");
        });
        timer555.setRepeats(false);
        timer555.start();
    }

    public Phase getPhase() {
        return this.phase;
    }

    public void setPhase(Phase p) {
        this.phase = p;
        phasetick = 0;
    }

    public void stopwalking() {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
    }

    public void startwalking() {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
    }

    public boolean iswalking() {
        return mc.gameSettings.keyBindForward.isKeyDown();
    }

    public void onUpdate() {
        screenshotticks++;
        if (screenshotticks > 1200) {
            Utils.takeMinecraftScreenshot();
            Utils.sendMessage("Screenshot taken!");
            screenshotticks = 0;
        }
        if (Utils.minutes((int) ((System.currentTimeMillis() - session) /1000)) >= currectsession) {
            currectsession += sessionstop;
            sessionpause = true;
        }
    }






    //BOT CONFIG



    public void saveConfig() {
        Properties props = new Properties();
        props.setProperty("Aim_reach", String.valueOf(reach));
        props.setProperty("Attack_reach", String.valueOf(attackreach));
        props.setProperty("Aim_smoothness_(yaw,pitch)", Arrays.toString(smoothness));
        props.setProperty("Jump_chance_(when_fighting)", String.valueOf(jumpchance));
        props.setProperty("Strafe_chance_(when_fighting)", String.valueOf(strafechance));
        props.setProperty("Attack_cps", String.valueOf(cps));
        props.setProperty("Reset_sprint_every_(ticks)", String.valueOf(sprintresticks));
        props.setProperty("Waiting_phase_chance_after_respawning", String.valueOf(waitchance));
        props.setProperty("Minimal_new_target_distance_advantage_compared_blah_blah", String.valueOf(switchtargetrange));
        props.setProperty("Skip_aiming_when_target_in_range", String.valueOf(reachskip));
        props.setProperty("Stop_session_every_(minutes)", String.valueOf(sessionstop));
        props.setProperty("Quit_when_encountered_issue_(or_staff_check)_instead_of_stopping_session", String.valueOf(quitbot));
        props.setProperty("Max_ticks_collided_horizontally", String.valueOf(maxtickscolided));

        try (OutputStream out = Files.newOutputStream(Paths.get("pitbotconf"))) {
            props.store(out, "Pit Bot Config");
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void loadConfig() {
        Properties props = new Properties();
        if (!Files.exists(Paths.get("pitbotconf"))) {
            saveConfig();
        }

        try (InputStream in = Files.newInputStream(Paths.get("pitbotconf"))) {
            props.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }

        reach = Double.parseDouble(props.getProperty("Aim_reach"));
        attackreach = Double.parseDouble(props.getProperty("Attack_reach"));
        smoothness = Arrays.stream(props.getProperty("Aim_smoothness_(yaw,pitch)")
                        .substring(1, props.getProperty("Aim_smoothness_(yaw,pitch)").length() - 1)
                        .split(","))
                .mapToDouble(Double::parseDouble)
                .toArray();
        jumpchance = Double.parseDouble(props.getProperty("Jump_chance_(when_fighting)"));
        strafechance = Double.parseDouble(props.getProperty("Strafe_chance_(when_fighting)"));
        cps = Double.parseDouble(props.getProperty("Attack_cps"));
        sprintresticks = Integer.parseInt(props.getProperty("Reset_sprint_every_(ticks)"));
        waitchance = Double.parseDouble(props.getProperty("Waiting_phase_chance_after_respawning"));
        switchtargetrange = Double.parseDouble(props.getProperty("Minimal_new_target_distance_advantage_compared_blah_blah"));
        reachskip = Double.parseDouble(props.getProperty("Skip_aiming_when_target_in_range"));
        sessionstop = Integer.parseInt(props.getProperty("Stop_session_every_(minutes)"));
        quitbot = Boolean.parseBoolean(props.getProperty("Quit_when_encountered_issue_(or_staff_check)_instead_of_stopping_session"));
        maxtickscolided = Integer.parseInt(props.getProperty("Max_ticks_collided_horizontally"));
    }
}