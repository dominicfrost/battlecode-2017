package battlecode2017;
import battlecode.common.*;

public class Archon extends Robot {
    private final int GARDENERS_PER_ROUND = 3;
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
        if (myGardener == null || !rc.canSenseRobot(myGardener.ID)) lookForGardener();
        if (myGardener == null) {
            randomSafeMove();
        } else {
            staySafeAroundMyGardener();
        }
    }

    private void randomSafeMove() {

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
        return rc.getRoundNum() / buildCount >= GARDENERS_PER_ROUND && rc.hasRobotBuildRequirements(RobotType.GARDENER);
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
}
