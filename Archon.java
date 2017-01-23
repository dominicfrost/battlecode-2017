package battlecode2017;

import battlecode.common.*;

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
    }

    protected void doTurn() throws GameActionException {
        postPeskyTrees();
        postPeskyAttackers();
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
        if (!rc.hasRobotBuildRequirements(RobotType.GARDENER)) return false;
        return (rc.getRoundNum() + ROUNDS_PER_GARDENER) / buildCount >= ROUNDS_PER_GARDENER;
    }

    private Direction getHireDirection() {
        Direction dir;
        for (int i = 0; i < 6; i++) {
            dir = Direction.getNorth().rotateRightDegrees(i * 60);
            if (rc.canHireGardener(dir)) {
                return dir;
            }
        }
        return null;
    }

    private void staySafeAroundMyGardener() throws GameActionException {
        float avgX = 0;
        float avgY = 0;
        int count = 0;
        for (RobotInfo e: nearbyEnemies) {
            avgX += e.location.x;
            avgY += e.location.y;
            count++;
        }
        if (count == 0) {
            stayAwayFromAllies();
            return;
        }
        randomSafeMove(location.directionTo(new MapLocation(avgX / count, avgY / count)).opposite());
    }

    private boolean locInGarden(MapLocation loc) {
        return loc.distanceSquaredTo(myGardener.location) <= myType.bodyRadius + RobotType.GARDENER.bodyRadius + GameConstants.BULLET_TREE_RADIUS;
    }
}
