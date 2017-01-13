package battlecode2017;
import battlecode.common.*;

public class Archon extends Robot {
    private Bug bugger;

    Archon(RobotController _rc) {
        super(_rc);
    }

    @Override
    protected void initRobotState() throws GameActionException {
        super.initRobotState();
        bugger = new Bug(rc);
    }

    protected void doTurn() throws GameActionException {
//        if (tryDodge()) return;

        if (trySpawnGardener()) return;

        Direction d = location.directionTo(home);
        if (d == null) {
            d = Direction.getNorth();
        }
        tryMove(d.opposite());
    }

    private boolean trySpawnGardener() throws GameActionException {
        if (shouldHireGardener()) {
            Direction dir = getHireDirection();
            if (dir != null) rc.hireGardener(dir);
            return true;
        }

        return false;
    }

    private boolean shouldHireGardener() {
        return rc.hasRobotBuildRequirements(RobotType.GARDENER);
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
