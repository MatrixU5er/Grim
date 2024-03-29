package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.anticheat.update.PostBlockPlace;
import ac.grim.grimac.utils.collisions.HitboxData;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.Pair;
import ac.grim.grimac.utils.nmsutil.Materials;
import ac.grim.grimac.utils.nmsutil.Ray;
import ac.grim.grimac.utils.nmsutil.ReachUtils;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.util.Vector3i;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@CheckData(name = "RotationPlace")
public class RotationPlace extends BlockPlaceCheck {
    double flagBuffer = 0; // If the player flags once, force them to play legit, or we will cancel the tick before.
    boolean ignorePost = false;

    // how much 1.11- server threshold safe?
    double cursorThreshold = PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_11) ? 0.0626 : 0.0001;
    boolean shouldSkipCheckCursor = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_11) && player.getClientVersion().isOlderThan(ClientVersion.V_1_11);

    public RotationPlace(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        if (place.getMaterial() == StateTypes.SCAFFOLDING) return;

        if (!shouldSkipCheckCursor) {
            Vector3i clicked = place.getPlacedAgainstBlockLocation();

            // use this until grim fix block boxes
            StateType type = player.compensatedWorld.getWrappedBlockStateAt(clicked).getType();
            boolean ableCheckFullBlock = type.isBlocking() && type.isSolid() && !type.exceedsCube();

            CollisionBox placedOn = HitboxData.getBlockHitbox(player, place.getMaterial(), player.getClientVersion(), player.compensatedWorld.getWrappedBlockStateAt(clicked), clicked.getX(), clicked.getY(), clicked.getZ());

            if (ableCheckFullBlock && placedOn instanceof SimpleCollisionBox && placedOn.isFullBlock()) {
                boolean flag = false;
                switch (place.getDirection()) {
                    case SOUTH:
                        flag = place.getCursor().getZ() != 1f;
                        break;
                    case NORTH:
                        flag = place.getCursor().getZ() != 0f;
                        break;
                    case EAST:
                        flag = place.getCursor().getX() != 1f;
                        break;
                    case WEST:
                        flag = place.getCursor().getX() != 0f;
                        break;
                    case UP:
                        flag = place.getCursor().getY() != 1f;
                        break;
                    case DOWN:
                        flag = place.getCursor().getY() != 0f;
                }
                if (flag) {
                    flagBuffer = 1;
                    if (flagAndAlert("pre-flying-impossible-cursor "+place.getCursor()) && shouldModifyPackets() && shouldCancel()) {
                        place.resync();
                    }
                }
            }
        }

        if (flagBuffer > 0) {
            // check it like the player sent a transaction
            PostBlockPlace postPlace = new PostBlockPlace(player, place);
            postPlace.setCursor(place.getCursor());

            // don't check cursor even player flagged
            if (!didRayTraceHit(postPlace, true)) {
                // If the player hit and has flagged this check recently
                if (flagAndAlert("pre-flying")) {
                    if (shouldModifyPackets() && shouldCancel()) {
                        place.resync();  // Deny the block placement.
                    } else {
                        ignorePost = true;
                    }
                }
            }
        }

    }

    // Use post flying because it has the correct rotation, and can't false easily.
    @Override
    public void onPostFlyingBlockPlace(PostBlockPlace place) {
        if (place.getMaterial() == StateTypes.SCAFFOLDING) return;

        // Don't flag twice
        if (ignorePost) {
            ignorePost = false;
            return;
        }

        // This can false with rapidly moving yaw in 1.8+ clients
        // wait didn't FabricatedPlace check work?
        if (!isCursorValid(place.getCursor(), Materials.isShapeExceedsCube(place.getPlacedAgainstMaterial()) || place.getPlacedAgainstMaterial() == StateTypes.LECTERN ? 1.5 : 1)) {
            flagBuffer = 1;
            flagAndAlert("invalid-cursor "+place.getCursor());
        } else if (!didRayTraceHit(place, shouldSkipCheckCursor)) { // cursor check may false behind ViaRewind, exempt
            flagBuffer = 1;
            flagAndAlert("post-flying");
        } else {
            flagBuffer = Math.max(0, flagBuffer - 0.1);
        }
    }

    // the player must raytrace the block and the cursor
    private boolean didRayTraceHit(PostBlockPlace place, boolean skipCheckCursor) {
        Vector3i placeLocation = place.getPlacedAgainstBlockLocation();

        SimpleCollisionBox blockBox = new SimpleCollisionBox(placeLocation);
        blockBox.expand(player.getClientVersion().isOlderThan(ClientVersion.V_1_9) ? 0.05 : player.getMovementThreshold());

        Vector3f cursor = place.getCursor();
        Vector3d clickLocation = new Vector3d(placeLocation.getX() + cursor.getX(), placeLocation.getY() + cursor.getY(), placeLocation.getZ() + cursor.getZ());

        SimpleCollisionBox cursorBox = new SimpleCollisionBox(clickLocation, clickLocation).expand(cursorThreshold);
        cursorBox.expand(player.getClientVersion().isOlderThan(ClientVersion.V_1_9) ? 0.05 : player.getMovementThreshold());


        // xRot and yRot may false because of code elsewhere
        float yaw = place.hasLook() ? place.getYaw() : player.xRot;
        float pitch = place.hasLook() ? place.getPitch() : player.yRot;

        List<Vector3f> possibleLookDirs = new ArrayList<>(Arrays.asList(
                new Vector3f(player.lastXRot, pitch, 0),
                new Vector3f(yaw, pitch, 0)
        ));

        // 1.9+ players could be a tick behind because we don't get skipped ticks
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
            possibleLookDirs.add(new Vector3f(player.lastXRot, player.lastYRot, 0));
        }

        // 1.7 players do not have any of these issues! They are always on the latest look vector
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_8)) {
            possibleLookDirs = Collections.singletonList(new Vector3f(yaw, pitch, 0));
        }

        // player's rotation didn't change, don't use lastRot
        if (!place.isFlying() || !place.hasLook()) {
            possibleLookDirs = Collections.singletonList(new Vector3f(yaw, pitch, 0));
        }

        for (double d : player.getPossibleEyeHeights()) {
            for (Vector3f lookDir : possibleLookDirs) {
                // x, y, z are correct for the block placement even after post tick because of code elsewhere
                Vector3d starting = new Vector3d(player.x, player.y + d, player.z);
                Ray trace = new Ray(player, starting.getX(), starting.getY(), starting.getZ(), lookDir.getX(), lookDir.getY());

                if (isEyeInBox(blockBox, d)) {
                    // use the cursor recheck
                    if (skipCheckCursor || isEyeInBox(cursorBox, d))
                        return true;
                    Pair<Vector, BlockFace> cursorIntercept = ReachUtils.calculateIntercept(cursorBox, trace.getOrigin(), trace.getPointAtDistance(6));
                    if (cursorIntercept.getFirst() != null)
                        return true;
                    // end blockBox check
                    continue;
                }

                Pair<Vector, BlockFace> blockIntercept = ReachUtils.calculateIntercept(blockBox, trace.getOrigin(), trace.getPointAtDistance(6));

                if (blockIntercept.getFirst() != null) {
                    // use the cursor recheck
                    if (skipCheckCursor || isEyeInBox(cursorBox, d))
                        return true;
                    Pair<Vector, BlockFace> cursorIntercept = ReachUtils.calculateIntercept(cursorBox, trace.getOrigin(), trace.getPointAtDistance(6));
                    if (cursorIntercept.getFirst() != null)
                        return true;
                }
            }
        }

        return false;
    }

    private boolean isEyeInBox(SimpleCollisionBox box, double eyeHeight) {
        SimpleCollisionBox eyePositions = new SimpleCollisionBox(player.x, player.y + eyeHeight, player.z, player.x, player.y + eyeHeight, player.z);
        return eyePositions.isIntersected(box);
    }

    private boolean isCursorValid(Vector3f cursor, double allowed) {
        if (Float.isFinite(cursor.getX()) && Float.isFinite(cursor.getY()) && Float.isFinite(cursor.getZ())) {
            double minAllowed = 1 - allowed;
            if (cursor.getX() < minAllowed || cursor.getY() < minAllowed || cursor.getZ() < minAllowed || cursor.getX() > allowed || cursor.getY() > allowed || cursor.getZ() > allowed) {
                return false;
            }
            return true;
        }
        return false;
    }

}
