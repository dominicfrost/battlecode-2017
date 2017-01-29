package battlecode2017;

import battlecode.common.*;

public class Archon extends Robot {
//    private RobotInfo myGardener;
    private int buildCount;
    private int buildCountCache;
    private int skipFirstRounds;

    Archon(RobotController _rc) {
        super(_rc);
    }

    @Override
    protected void initRobotState() throws GameActionException {
        super.initRobotState();
        buildCount = 0;
        buildCountCache = 0;
        skipFirstRounds = !allyArchonLocs[0].equals(rc.getLocation()) ? 40 : 0;
    }

    protected void doTurn() throws GameActionException {
        if (shouldSkipTurn()) return;
        updateGardenerCount();
        postPeskyTrees();
        postPeskyAttackers();
        trySpawnGardener();
        tryDodge();
//        if (tryDodge()) return; // think about immediate health
//        moveToSafestLocation(); // think about long term health
    }

    private boolean shouldSkipTurn() throws GameActionException {
        if (skipFirstRounds > 0) {
            tryDodge();
            skipFirstRounds--;
            return true;
        }
        return false;
    }

    private void updateGardenerCount() throws GameActionException {
        int buildCountTotal = rc.readBroadcast(Coms.GARNDENER_COUNT);
        buildCount = buildCountTotal - buildCountCache;
        buildCountCache = buildCountTotal;
    }

//    private void moveToSafestLocation() throws GameActionException {
//        if (myGardener == null || !rc.canSenseRobot(myGardener.ID)) lookForGardener();
//        if (myGardener != null) staySafeAroundMyGardener();
//    }
//
//    private void lookForGardener() {
//        for (RobotInfo r : nearbyAllies) {
//            if (r.type == RobotType.GARDENER) {
//                myGardener = r;
//            }
//        }
//    }

    private void trySpawnGardener() throws GameActionException {
        if (shouldHireGardener()) {
            Direction dir = getHireDirection();
            if (dir != null) {
                rc.hireGardener(dir);
            }
        }
    }

    private boolean shouldHireGardener() {
        return rc.hasRobotBuildRequirements(RobotType.GARDENER) && (rc.getTreeCount() + 6) / (buildCount + 1) >= 6;
    }

    private Direction getHireDirection() {
        Direction dir;
        for (int i = 0; i < 12; i++) {
            dir = Direction.getNorth().rotateRightDegrees(i * 30);
            if (rc.canHireGardener(dir)) {
                return dir;
            }
        }
        return null;
    }

//    private void staySafeAroundMyGardener() throws GameActionException {
//        float avgX = 0;
//        float avgY = 0;
//        int count = 0;
//        for (RobotInfo e: nearbyEnemies) {
//            avgX += e.location.x;
//            avgY += e.location.y;
//            count++;
//        }
//        if (count == 0) {
//            stayAwayFromAllies();
//            return;
//        }
//        randomSafeMove(location.directionTo(new MapLocation(avgX / count, avgY / count)).opposite());
//    }
//
//    private boolean locInGarden(MapLocation loc) {
//        return loc.distanceSquaredTo(myGardener.location) <= myType.bodyRadius + RobotType.GARDENER.bodyRadius + GameConstants.BULLET_TREE_RADIUS;
//    }
}
