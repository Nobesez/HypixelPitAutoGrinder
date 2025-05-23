package me.mitfox.betterpit;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import org.lwjgl.opengl.Display;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static me.mitfox.betterpit.PitBotMod.mc;

public class Utils {
    public static boolean inGame() {
        return mc.field_71439_g!=null&&mc.field_71441_e!=null;
    }
    public static void sendMessage(String message) {
        if (inGame()) {
            mc.field_71439_g.func_145747_a(new ChatComponentText("§c[PB] - §8" + message));
        }
    }
    public static float[] getAimRotations(BlockPos blockPos) {
        if (blockPos != null&&mc.field_71441_e!=null&&mc.field_71439_g!=null) {
            double xDiff = blockPos.func_177958_n() + 0.5 - mc.field_71439_g.field_70165_t;
            double yDiff = blockPos.func_177956_o() + 0.5 - mc.field_71439_g.field_70163_u - mc.field_71439_g.func_70047_e();
            double zDiff = blockPos.func_177952_p() + 0.5 - mc.field_71439_g.field_70161_v;
            //double distance = Math.sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff);
            float yaw = (float) Math.toDegrees(Math.atan2(zDiff, xDiff)) - 90;
            float pitch = (float) -Math.toDegrees(Math.atan2(yDiff, Math.sqrt(xDiff * xDiff + zDiff * zDiff)));

            return new float[]{yaw, pitch};
        }
        return null;
    }
    public static EntityPlayer GetTarget(double range, double attackrange,EntityPlayer skipentity) {
        List<EntityPlayer> targets = mc.field_71441_e.field_73010_i;
        targets = targets.stream().filter(entity -> mc.thePlayer.getDistanceToEntity(entity) <= range && entity!=skipentity && entity.posY < mc.thePlayer.posY+8 && entity.posY > mc.thePlayer.posY-8 && entity != mc.thePlayer && !entity.isDead && !entity.isInvisible()).collect(Collectors.toList());
        targets.sort(Comparator.comparingDouble(entity -> mc.thePlayer.getDistanceToEntity(entity)));

        List<EntityPlayer> attacktargets = targets.stream().filter(entity -> mc.thePlayer.getDistanceToEntity(entity) <= attackrange).collect(Collectors.toList());
        attacktargets.sort(Comparator.comparingDouble(entity -> Utils.angledifference(Utils.getTargetRotations(entity)[0],mc.thePlayer.rotationYaw)));

        if (!attacktargets.isEmpty()) {
            return attacktargets.get(0);
        } else if (!targets.isEmpty()) {
            return targets.get(0);
        }

        return null;
    }
    public static boolean hasDiamondChestplate(EntityPlayer player) {
        for (ItemStack itemStack : player.field_71071_by.field_70460_b) {
            if (itemStack != null && itemStack.func_77973_b() instanceof ItemArmor) {
                ItemArmor armor = (ItemArmor) itemStack.func_77973_b();
                if (armor.func_82812_d() == ItemArmor.ArmorMaterial.DIAMOND && armor.field_77881_a == 1) {
                    return true;
                }
            }
        }
        return false;
    }
    public static void aim(EntityPlayer target, double[] i) {
        if (target==null) {
            return;
        }

        float[] t = smoothRotations(getTargetRotations(target), i[0]/10, i[1]/10);

        final float[] rotations = new float[]{t[0], t[1]};
        final float[] lastRotations = new float[]{mc.field_71439_g.field_70177_z, mc.field_71439_g.field_70125_A};

        final float[] fixedRotations = getFixedRotation(rotations, lastRotations);

        mc.field_71439_g.field_70177_z = fixedRotations[0];
        mc.field_71439_g.field_70125_A = fixedRotations[1];
    }
    public static float[] getTargetRotations(Entity q) {
        double diffX = q.field_70165_t - mc.field_71439_g.field_70165_t;
        double diffY;
        if (q instanceof EntityLivingBase) {
            EntityLivingBase en = (EntityLivingBase) q;
            diffY = en.field_70163_u + (double) en.func_70047_e() * 0.9D - (mc.field_71439_g.field_70163_u + (double) mc.field_71439_g.func_70047_e());
        } else {
            diffY = (q.func_174813_aQ().field_72338_b + q.func_174813_aQ().field_72337_e) / 2.0D - (mc.field_71439_g.field_70163_u + (double) mc.field_71439_g.func_70047_e());
        }

        double diffZ = q.field_70161_v - mc.field_71439_g.field_70161_v;
        double dist = MathHelper.func_76133_a(diffX * diffX + diffZ * diffZ);
        float yaw = (float) (Math.atan2(diffZ, diffX) * 180.0D / 3.141592653589793D) - 90.0F;
        float pitch = (float) (-(Math.atan2(diffY, dist) * 180.0D / 3.141592653589793D));
        float o = pitch+11;

        return new float[]{yaw, o};
    }
    public static float[] smoothRotations(float[] rotations, double smooth, double smooth2) {
        float angleDifference = angledifference(rotations[0],mc.field_71439_g.field_70177_z);
        float angleDifference2 = angledifference(rotations[1], mc.field_71439_g.field_70125_A);

        double YawCalculation = (1 - smooth) * (1.0 + 0.1 * Math.abs(angleDifference) / 180.0);
        double PitchCalculation = (1 - smooth2) * (1.0 + 0.1 * Math.abs(angleDifference2) / 180.0);

        return new float[]{(float) (mc.field_71439_g.field_70177_z + MathHelper.func_76142_g(rotations[0] - mc.field_71439_g.field_70177_z) * YawCalculation), (float) (mc.field_71439_g.field_70125_A + MathHelper.func_76142_g(rotations[1] - mc.field_71439_g.field_70125_A) * PitchCalculation)};

    }
    public static float angledifference(float rotation1, float rotation2) {
        return MathHelper.func_76142_g(rotation1 - rotation2);
    }
    public static float[] getFixedRotation(final float[] rotations, final float[] lastRotations) {
        final Minecraft mc = Minecraft.func_71410_x();

        final float yaw = rotations[0];
        final float pitch = rotations[1];

        final float lastYaw = lastRotations[0];
        final float lastPitch = lastRotations[1];

        final float f = mc.field_71474_y.field_74341_c * 0.6F + 0.2F;
        final float gcd = f * f * f * 1.2F;

        final float deltaYaw = yaw - lastYaw;
        final float deltaPitch = pitch - lastPitch;

        final float fixedDeltaYaw = deltaYaw - (deltaYaw % gcd);
        final float fixedDeltaPitch = deltaPitch - (deltaPitch % gcd);

        final float fixedYaw = lastYaw + fixedDeltaYaw;
        final float fixedPitch = lastPitch + fixedDeltaPitch;

        return new float[]{fixedYaw, fixedPitch};
    }
    public static boolean isGoldenApple(ItemStack is){
        return is.func_77973_b().equals(Item.func_111206_d("golden_apple"));
    }
    public static boolean isGoldenHead(ItemStack is){
        return is.func_77973_b().equals(Item.func_111206_d("skull"));
    }
    public static boolean isBakedPotato(ItemStack is){
        return is.func_77973_b().equals(Item.func_111206_d("baked_potato"));
    }
    public static boolean isOlympusPotion(ItemStack is){
        return is.func_77973_b().equals(Item.func_111206_d("potion"));
    }
    public static boolean isSoup(ItemStack is){
        return is.func_77973_b().equals(Item.func_111206_d("mushroom_stew"));
    }
    static double getRandom(double minValue, double maxValue) {
        Random random = new Random();
        return minValue + (maxValue - minValue) * random.nextDouble();
    }
    public static String formatTime(int seconds) {
        int minutes = minutes(seconds);
        int remainingSeconds = seconds % 60;
        return minutes + (remainingSeconds<10 ? ":0"+remainingSeconds : ":"+remainingSeconds);
    }
    public static int minutes(int seconds){
        return seconds / 60;
    }
    public static void takeMinecraftScreenshot() {
        try {
            int screenWidth = Display.getWidth();
            int screenHeight = Display.getHeight();

            ScreenShotHelper.func_148259_a(null, null, screenWidth, screenHeight, mc.func_147110_a());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}