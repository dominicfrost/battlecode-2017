package battlecode2017;

import battlecode.common.*;

abstract public class Bugger extends Robot {
    private final int ROTATION_GRANULARITY = 45;
    private final int FOLLOW_WALL_GRANULARITY = 45 / 2;

    private int quitThresh;
    private MapLocation goal;

    private Direction myDirection;
    private boolean movedRight;
    private BugState state;

    public enum BugState {
        TO_GOAL,
        ON_WALL,
    }

    Bugger(RobotController _rc) {
        super(_rc);
    }

    @Override
    protected void initRobotState() throws GameActionException {
        super.initRobotState();
        quitThresh = 0;
        goal = null;
    }

    public void moveWithBugger(MapLocation nextGoal, int newQuitThresh) throws GameActionException {
        setGoal(nextGoal, newQuitThresh);
        if (hasMoved) return;
        Direction next = bugNextMove();
        if (next != null) move(next);
    }

    public Direction nextStride(MapLocation nextGoal, int newQuitThresh) throws GameActionException {
        setGoal(nextGoal, newQuitThresh);
        if (hasMoved) return null;
        return bugNextMove();
    }

    private void setGoal(MapLocation newGoal, int newQuitThresh) {
        if (goal == null || !goal.equals(newGoal)) {
            state = BugState.TO_GOAL;
            goal = newGoal;
        }
        quitThresh = newQuitThresh;
    }

    private Direction bugNextMove() throws GameActionException {
        switch (state) {
            case TO_GOAL:
                return moveToGoal();
            case ON_WALL:
                return followWall();
        }

        return null;
    }

    //get the next location on the mLine and try to move there
    private Direction moveToGoal() throws GameActionException {
        Direction toGoal = location.directionTo(goal);
        if (rc.canMove(toGoal)) {
            return toGoal;
        } else {
            return getHandOnWall();
        }
    }

    private Direction getHandOnWall() throws GameActionException {
        Direction rightDir;
        Direction leftDir;

        Direction toGoal = location.directionTo(goal);
        for (int i = 0; i < 180; i += ROTATION_GRANULARITY) {
            rightDir = toGoal.rotateRightDegrees(i);
            leftDir = toGoal.rotateLeftDegrees(i);

            if (rc.canMove(rightDir)) {
                state = BugState.ON_WALL;
                movedRight = true;
                myDirection = rightDir;
                return rightDir;
            }

            if (rc.canMove(leftDir)) {
                state = BugState.ON_WALL;
                movedRight = false;
                myDirection = leftDir;
                return leftDir;
            }
        }

        return null;
    }

    private Direction followWall() throws GameActionException {
        Direction toGoal = location.directionTo(goal);
        if (rc.canMove(toGoal)) {
            state = BugState.TO_GOAL;
            return toGoal;
        }

        Direction next;
        for (int i = -90; i < 180; i += FOLLOW_WALL_GRANULARITY) {
            next = rotateInDir(myDirection, !movedRight, i);
            if (rc.canMove(next)) {
                myDirection = next;
                return next;
            }
        }

        return null;
    }



    private Direction rotateInDir(Direction startDir, boolean rotateLeft, float degrees) throws GameActionException {
        if (rotateLeft) {
            return startDir.rotateLeftDegrees(degrees);
        } else {
            return startDir.rotateRightDegrees(degrees);
        }
    }
}
