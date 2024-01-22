package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;

@CheckData(name = "UseHit")
public class UseHit extends Check implements PacketCheck {

    public UseHit(final GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            // https://github.com/GrimAnticheat/Grim/blob/2.0/src/main/java/ac/grim/grimac/events/packets/PacketPlayerAttack.java#L39
            if (player.getClientVersion().isOlderThan(ClientVersion.V_1_9)) return;

            if (player.packetStateData.slowedByUsingItem) {
                if (flagAndAlert("hit entity") && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }
        } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_9)) {
                if (player.gamemode == GameMode.SPECTATOR || player.gamemode == GameMode.ADVENTURE) return;

                WrapperPlayClientPlayerBlockPlacement place = new WrapperPlayClientPlayerBlockPlacement(event);
                if (place.getFace() == BlockFace.OTHER) return;
            }

            if (player.packetStateData.slowedByUsingItem) {
                if (flagAndAlert("block place") && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }
        } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging dig = new WrapperPlayClientPlayerDigging(event);
            if (dig.getAction() != DiggingAction.START_DIGGING && dig.getAction() != DiggingAction.FINISHED_DIGGING)
                return;

            if (player.packetStateData.slowedByUsingItem) {
                if (flagAndAlert("block break") && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }
        }

    }
}
