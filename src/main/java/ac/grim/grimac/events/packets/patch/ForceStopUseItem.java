package ac.grim.grimac.events.packets.patch;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.util.reflection.Reflection;
import io.github.retrooper.packetevents.util.FoliaCompatUtil;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public class ForceStopUseItem {
    public static Method clearActiveHandNMS = null;

    public static boolean initMethod() {
        switch (PacketEvents.getAPI().getServerManager().getVersion()) {
            case V_1_7_10:
                return getMethodWithoutException(SpigotReflectionUtil.ENTITY_HUMAN_CLASS, "bB");

            case V_1_8:
                return getMethodWithoutException(SpigotReflectionUtil.ENTITY_HUMAN_CLASS, "bU");
            case V_1_8_3:
            case V_1_8_8:
                return getMethodWithoutException(SpigotReflectionUtil.ENTITY_HUMAN_CLASS, "bV");

            case V_1_9:
            case V_1_9_2:
                return getLegacyMethod("EntityLiving", "cz");
            case V_1_9_4:
                return getLegacyMethod("EntityLiving", "cA");

            case V_1_10:
            case V_1_10_1:
            case V_1_10_2:
                return getLegacyMethod("EntityLiving", "cE");

            case V_1_11:
            case V_1_11_2:
                return getLegacyMethod("EntityLiving", "cF");

            case V_1_12:
            case V_1_12_1:
            case V_1_12_2:
                return getLegacyMethod("EntityLiving", "cN");

            case V_1_13:
            case V_1_13_1:
            case V_1_13_2:
                return getLegacyMethod("EntityLiving", "da");

            case V_1_14:
            case V_1_14_1:
            case V_1_14_2:
            case V_1_14_3:
            case V_1_14_4:
                return getLegacyMethod("EntityLiving", "dp");

            case V_1_15:
            case V_1_15_1:
            case V_1_15_2:
                return getLegacyMethod("EntityLiving", "dH");

            default:
                return getMethodWithoutException(SpigotReflectionUtil.getServerClass("world.entity.EntityLiving", "EntityLiving"), "clearActiveItem");
        }
    }

    // This should runs on netty thread
    public static boolean handleSlowStateChange(GrimPlayer player) {
        if (clearActiveHandNMS == null) {
            if (!initMethod()) return false;
        }
        Player bukkitPlayer = player.bukkitPlayer;
        if (bukkitPlayer != null) {
            FoliaCompatUtil.runTaskForEntity(bukkitPlayer, GrimAPI.INSTANCE.getPlugin(), () -> {
                try {
                    clearActiveHandNMS.invoke(SpigotReflectionUtil.getEntityPlayer(bukkitPlayer));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, null, 0);
            return true;
        }
        return false;
    }

    private static boolean getLegacyMethod(String className, String methodName) {
        return getMethodWithoutException(Reflection.getClassByNameWithoutException(SpigotReflectionUtil.LEGACY_NMS_PACKAGE + className), methodName);
    }

    private static boolean getMethodWithoutException(Class<?> clazz, String methodName) {
        try {
            Method m = clazz.getDeclaredMethod(methodName);
            m.setAccessible(true);
            clearActiveHandNMS = m;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
