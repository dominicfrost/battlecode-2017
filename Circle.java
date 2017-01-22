package battlecode2017;

import battlecode.common.*;


// Circle decorates robot with the ability to do sweet circling
abstract public class Circle extends Bugger {
    private static int MOVE_IN_GRANULARITY = 10;

    Circle(RobotController _rc) {
        super(_rc);
    }

    protected void moveCirclingLocation(MapLocation goal, float goalRadius) throws GameActionException {
        if (hasMoved) return;
        if (atCircleGoal(goal, goalRadius)) return;

        // If i'm not close enough
        if (location.distanceSquaredTo(goal) > sqrFloat(goalRadius + myType.strideRadius + myType.bodyRadius)) {
            moveWithBugger(goal, 0);
            return;
        }

        moveInOnGoal(goal, goalRadius);
    }

    public void attackCircleGoal(MapLocation goal, float goalRadius) throws GameActionException {
        debug("HasATt " + hasAttacked + " !atCircleGoal" + !atCircleGoal(goal, goalRadius));
        if (hasAttacked) return;
        if (!atCircleGoal(goal, goalRadius)) return;
        spray(location.directionTo(goal));
    }

    private boolean atCircleGoal(MapLocation goal, float goalRadius) {
        float distanceToCenterSquared = sqrFloat(goalRadius + myType.bodyRadius);
        return location.distanceSquaredTo(goal) <= distanceToCenterSquared;
    }

    private void moveInOnGoal(MapLocation goal, float goalRadius) throws GameActionException {
        Direction fromGoal = goal.directionTo(location);
        Direction right, left;
        MapLocation next;

        int count = 0;

        next = nextLoc(goal, goalRadius, fromGoal);
        if (rc.canMove(next)) {
            move(next);
            return;
        }

        while (true) {
            right = fromGoal.rotateRightDegrees(MOVE_IN_GRANULARITY * count);
            next = nextLoc(goal, goalRadius, right);
            if (location.distanceSquaredTo(next) > myType.strideRadius) return;
            debug(location.distanceSquaredTo(next) + " " + fromGoal + " " + location + " " + next);
            if (rc.canMove(next)) {
                move(next);
                return;
            }

            left = fromGoal.rotateLeftDegrees(MOVE_IN_GRANULARITY * count);
            next = nextLoc(goal, goalRadius, left);
            if (location.distanceSquaredTo(next) > myType.strideRadius) return;
            if (rc.canMove(next)) {
                move(next);
                return;
            }

            count++;
        }
    }

    private MapLocation nextLoc(MapLocation goal, float goalRadius, Direction fromGoal) {
        return goal.add(fromGoal, goalRadius + myType.bodyRadius);
    }
}

