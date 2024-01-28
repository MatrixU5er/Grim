package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import com.viaversion.viaversion.api.Via;
import io.github.retrooper.packetevents.util.viaversion.ViaVersionUtil;


public class LegacyEntityCollisionHandler extends Check implements PacketCheck, PostPredictionCheck {
    public boolean pushable = true;
    private boolean disablePushableNextTick = false;

    public LegacyEntityCollisionHandler(GrimPlayer playerData) {
        super(playerData);
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (disablePushableNextTick) {
            disablePushableNextTick = false;
            pushable = false;
        }
        if (!ViaVersionUtil.isAvailable() || !Via.getConfig().isAutoTeam() || !Via.getConfig().isPreventCollision()) {
            pushable = true;
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_9)) {
            if (event.getPacketType() == PacketType.Play.Server.JOIN_GAME) {
                if (ViaVersionUtil.isAvailable() && Via.getConfig().isAutoTeam() && Via.getConfig().isPreventCollision()) {
                    player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                        pushable = true;
                        disablePushableNextTick = false;
                    });
                    player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> {
                        pushable = true;
                        disablePushableNextTick = true;
                    });
                }
            }

            if (event.getPacketType() == PacketType.Play.Server.TEAMS) {
                if (ViaVersionUtil.isAvailable() && Via.getConfig().isAutoTeam() && Via.getConfig().isPreventCollision()) {
                    WrapperPlayServerTeams teams = new WrapperPlayServerTeams(event);
                    if (teams.getTeamMode() != WrapperPlayServerTeams.TeamMode.UPDATE) {
                        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                            pushable = true;
                            disablePushableNextTick = false;
                        });
                        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> {
                            pushable = true;
                            disablePushableNextTick = true;
                        });
                    }
                }
            }

        }
    }
    
}
