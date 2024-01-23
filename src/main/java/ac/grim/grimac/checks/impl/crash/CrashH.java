package ac.grim.grimac.checks.impl.crash;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;

@CheckData(name = "CrashH")
public class CrashH extends Check implements PacketCheck {

    public CrashH(GrimPlayer playerData) {
        super(playerData);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            WrapperPlayClientClickWindow click = new WrapperPlayClientClickWindow(event);

            // Action number always 0 with >=1.17 client and <1.17 server because of via
            // Maybe we should change our max transaction packet id to -1?
            if (player.getClientVersion().isOlderThan(ClientVersion.V_1_17)) {
                click.getActionNumber().ifPresent(actionNumber -> {
                    if (actionNumber <= 0 && flagAndAlert()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                });
            }

        }
    }

}
