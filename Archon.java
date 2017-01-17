package battlecode2017;

import battlecode.common.*;
import java.lang.Math;

public class Archon extends Robot {
    private final int ROUNDS_PER_GARDENER = 500;

    private Bug bugger;
    private int buildCount;
    private RobotInfo myGardener;

    Archon(RobotController _rc) {
        super(_rc);
    }

    @Override
    protected void initRobotState() throws GameActionException {
        super.initRobotState();
        bugger = new Bug(rc);
        buildCount = 1;
        postPeskyTrees();
    }

    @Override
    protected void initRoundState() {
        super.initRoundState();
    }

    protected void doTurn() throws GameActionException {
        trySpawnGardener();
        moveToSafestLocation();
    }

    private void moveToSafestLocation() throws GameActionException {
//        if (myGardener == null || !rc.canSenseRobot(myGardener.ID)) lookForGardener();
//        if (myGardener == null) {
//            randomSafeMove();
//        } else {
//            staySafeAroundMyGardener();
//        }
        randomSafeMove();
    }

    private void staySafeAroundMyGardener() {
//        Direction toMove = safestLocationAroundMyGarden();
    }

    private Direction safestLocationAroundMyGarden(BulletInfo[] bullets, MapLocation startLocation) {
//        Direction toGardener = location.directionTo(myGardener);
//        for ()
        return null;
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

    private void postPeskyTrees() throws GameActionException {
        TreeInfo[] peskyTrees = rc.senseNearbyTrees(10.0f);
        int broadcastChannel = PESKYTREES;
        for (int i = 0; i < peskyTrees.length; i ++){
            if (peskyTrees[i].getTeam() != rc.getTeam()){
                MapLocation treeLoc = peskyTrees[i].getLocation();
                rc.broadcast(broadcastChannel, Math.round(treeLoc.x));
                rc.broadcast(broadcastChannel + 1, Math.round(treeLoc.y));
                broadcastChannel += 2;
            }
        }
        rc.broadcast(broadcastChannel, 0);
        rc.broadcast(broadcastChannel + 1, 0);
    }
}
