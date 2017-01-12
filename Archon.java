package battlecode2017;
import battlecode.common.*;

public class Archon extends Robot {
    Archon(RobotController _rc) {
        super(_rc);
    }

    protected void doTurn() throws GameActionException {
        if (tryDodge()) return;

        if (trySpawnGardener()) return;

        Direction d = location.directionTo(home);
        if (d == null) {
            d = Direction.getNorth();
        }
        tryMove(d.opposite());
    }

    private boolean trySpawnGardener() throws GameActionException {
        Direction dir = getHireDirection();
        if (shouldHireGardener(dir)) {
            rc.hireGardener(dir);
            return true;
        }

        return false;
    }

    private boolean shouldHireGardener(Direction dir) {
        return rc.canHireGardener(dir);
    }

    private Direction getHireDirection() {
        return Direction.getNorth();
    }
}
