package battlecode2017;

import battlecode.common.*;
import java.lang.Math;

public class Archon extends Robot {
    private final int ROUNDS_PER_GARDENER = 250;

    private int buildCount;
    private RobotInfo myGardener;

    Archon(RobotController _rc) {
        super(_rc);
    }

    @Override
    protected void initRobotState() throws GameActionException {
        super.initRobotState();
        buildCount = 1;
        postPeskyTrees();
    }

    @Override
    protected void initRoundState() throws GameActionException {
        super.initRoundState();
    }

    protected void doTurn() throws GameActionException {
        postPeskyTrees();
        trySpawnGardener();
        if (tryDodge()) return; // think about immediate health
        moveToSafestLocation(); // think about long term health
    }

    private void moveToSafestLocation() throws GameActionException {
        if (myGardener == null || !rc.canSenseRobot(myGardener.ID)) lookForGardener();
        if (myGardener != null) staySafeAroundMyGardener();
    }

    private void lookForGardener() {
        for (RobotInfo r : nearbyAllies) {
            if (r.type == RobotType.GARDENER) {
                myGardener = r;
            }
        }
    }

    private void trySpawnGardener() throws GameActionException {
        if (shouldHireGardener()) {
            Direction dir = getHireDirection();
            if (dir != null) {
                rc.hireGardener(dir);
                buildCount++;
            }
        }
    }

    private boolean shouldHireGardener() {
        return (rc.getRoundNum() + ROUNDS_PER_GARDENER) / buildCount >= ROUNDS_PER_GARDENER && rc.hasRobotBuildRequirements(RobotType.GARDENER);
    }

    private Direction getHireDirection() {
        Direction dir;
        for (int i = 0; i < 5; i++) {
            dir = Direction.getNorth().rotateRightDegrees(i * 72);
            if (rc.canHireGardener(dir)) {
                return dir;
            }
        }
        return null;
    }

    private void staySafeAroundMyGardener() throws GameActionException {
        Direction toMove = null;
        Direction startDir = location.directionTo(myGardener.location);
        float nextHealth;
        float minHealth = damageAtLocation(location);

        Direction next;
        for (int i = 0; i < 4; i++) {
            next = startDir.rotateRightDegrees(i * 30);
            if (!locInGarden(location.add(next)) && rc.canMove(next)) {
                nextHealth = damageAtLocation(location.add(next));
                if (nextHealth < minHealth) {
                    toMove = next;
                    minHealth = nextHealth;
                }
            }

            next = startDir.rotateLeftDegrees(i * 30);
            if (!locInGarden(location.add(next)) && rc.canMove(next)) {
                nextHealth = damageAtLocation(location.add(next));
                if (nextHealth < minHealth) {
                    toMove = next;
                    minHealth = nextHealth;
                }
            }
        }

        if (toMove != null) {
            rc.move(toMove);
        }
    }

    private boolean locInGarden(MapLocation loc) {
        return loc.distanceSquaredTo(myGardener.location) <= myType.bodyRadius + RobotType.GARDENER.bodyRadius + GameConstants.BULLET_TREE_RADIUS;
    }

    private void postPeskyTrees() throws GameActionException {
        if (nearbyTrees != null) {
            int broadcastChannel = Coms.PESKY_TREES;
            for (int i = 0; i < nearbyTrees.length; i ++){
                if (nearbyTrees[i].getTeam() != rc.getTeam()){
                    MapLocation treeLoc = nearbyTrees[i].getLocation();
                    rc.broadcast(broadcastChannel, Math.round(treeLoc.x));
                    rc.broadcast(broadcastChannel + 1, Math.round(treeLoc.y));
                    broadcastChannel += 2;
                }
            }
            rc.broadcast(broadcastChannel, 0);
            rc.broadcast(broadcastChannel + 1, 0);

        }
    }
}
