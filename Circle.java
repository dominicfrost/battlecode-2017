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
        debug("moveCirclingLocation " + location.distanceSquaredTo(goal) + "  "+ sqrFloat(goalRadius + myType.strideRadius + myType.bodyRadius));
        if (location.distanceSquaredTo(goal) > sqrFloat(goalRadius + myType.strideRadius + myType.bodyRadius)) {
            debug("MOVEWITHBUGG");
            moveWithBugger(goal, 0);
            return;
        }

        moveInOnGoal(goal, goalRadius);
    }

    protected void attackCircleGoal(MapLocation goal, float goalRadius) throws GameActionException {
        if (hasAttacked) return;
        if (!atCircleGoal(goal, goalRadius)) return;
        spray(location.directionTo(goal));
    }

    protected boolean atCircleGoal(MapLocation goal, float goalRadius) {
        float distanceToCenterSquared = sqrFloat(goalRadius + myType.bodyRadius);
        return location.distanceSquaredTo(goal) <= distanceToCenterSquared + .001F;
    }

    private void moveInOnGoal(MapLocation goal, float goalRadius) throws GameActionException {
        Direction fromGoal = goal.directionTo(location);
        Direction right, left;
        MapLocation next;
        debug("moveInOnGoal");

        int count = 0;

        next = nextLoc(goal, goalRadius, fromGoal);
        if (rc.canMove(next)) {
            move(next);
            return;
        }

        while (true) {
            right = fromGoal.rotateRightDegrees(MOVE_IN_GRANULARITY * count);
            next = nextLoc(goal, goalRadius, right);
            if (location.distanceSquaredTo(next) > myType.strideRadius) break;
            if (rc.canMove(next)) {
                move(next);
                return;
            }

            left = fromGoal.rotateLeftDegrees(MOVE_IN_GRANULARITY * count);
            next = nextLoc(goal, goalRadius, left);
            if (location.distanceSquaredTo(next) > myType.strideRadius) break;
            if (rc.canMove(next)) {
                move(next);
                return;
            }

            count++;
        }
        debug("moveWithBugger");
        tryMove(location.directionTo(goal));
    }

    private MapLocation nextLoc(MapLocation goal, float goalRadius, Direction fromGoal) {
        return goal.add(fromGoal, goalRadius + myType.bodyRadius);
    }
}

