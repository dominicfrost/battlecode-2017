package battlecode2017;

import battlecode.common.*;

public class Bug {
    private final int ROTATION_GRANULARITY = 45;
    private final int FOLLOW_WALL_GRANULARITY = 45 / 2;
    private int quitThresh;
    private RobotController rc;
    private MapLocation start;
    private MapLocation goal;
    private Direction mLineDir;
    private RobotType myType;

    private MapLocation myLocation;
    private MapLocation startLocation;
    private Direction myDirection;
    private boolean movedRight;
    private BugState state;

    public enum BugState {
        ON_MLINE,
        ON_WALL,
    }

    public Bug(RobotController _rc) {
        rc = _rc;
    }

    public boolean hasGoal() {
        return start != null && goal != null;
    }

    public MapLocation goal() {
        return goal;
    }

    public void setGoal(MapLocation _start, MapLocation _goal, int _quitThresh) {
        if (rc.getID() == 10140 && rc.getRoundNum() < 231 ) System.out.println("setGoal");
        state = BugState.ON_MLINE;
        quitThresh = _quitThresh;
        start = _start;
        goal = _goal;
        mLineDir = _start.directionTo(_goal);
        myDirection = null;
        startLocation = null;
        myType = rc.getType();
    }


    public Direction nextStride(MapLocation myLoc, TreeInfo[] nearbyTrees) throws GameActionException {
        myLocation = myLoc;

        if (state == BugState.ON_WALL) {
            boolean anyClose = false;
            for (TreeInfo ti : nearbyTrees) {
                if (ti.location.distanceSquaredTo(myLocation) > myType.strideRadius + myType.bodyRadius + ti.radius) {
                    anyClose = true;
                    break;
                }
            }
            if (!anyClose) setGoal(myLocation, goal, quitThresh);
        }


        if (currentMLineContains(myLocation)) {
            if (rc.getID() == 10140 && rc.getRoundNum() < 231 ) System.out.println("state" + state.toString());
            if (startLocation != null && myLocation.distanceSquaredTo(startLocation) >= 2.0 ) {
                state = BugState.ON_MLINE;
            }
        }

        //im at the goal yo!
        if(myLocation.distanceSquaredTo(goal) <= quitThresh) {
            return null;
        }

        Direction dirToMove = bugNextMove();
        if (rc.getID() == 10140 && rc.getRoundNum() < 231 && dirToMove != null) System.out.println("Dir is " + dirToMove.toString());
        if (rc.getID() == 10140 && rc.getRoundNum() < 231 && dirToMove == null) System.out.println("Dir is null");
        if (dirToMove == null) setGoal(myLocation, goal, quitThresh);
        return dirToMove;
    }

    private Direction bugNextMove() throws GameActionException {
        switch (state) {
            case ON_MLINE:
                return moveOnMLine();
            case ON_WALL:
                return followWall();
        }

        return null;
    }

//    private Direction resetWanderer() throws GameActionException {
//        setGoal(myLocation, goal, quitThresh);
//        if (rc.getID() == 10140 && rc.getRoundNum() < 231) System.out.println("resetWanderer");
//        return nextStride(myLocation);
//    }

    //get the next location on the mLine and try to move there
    private Direction moveOnMLine() throws GameActionException {
        if (!isOccupied(mLineDir)) {
            return mLineDir;
        } else {
            return getHandOnWall();
        }
    }

    private Direction getHandOnWall() throws GameActionException {
        Direction rightDir;
        Direction leftDir;

        for (int i = 0; i < 180; i += ROTATION_GRANULARITY) {

            rightDir = mLineDir.rotateRightDegrees(i);
            leftDir = mLineDir.rotateLeftDegrees(i);

            if (!isOccupied(rightDir)) {
                state = BugState.ON_WALL;
                movedRight = true;
                myDirection = rightDir;
//                lastWallSpot = myLocation;
                startLocation = myLocation;
                if (rc.getID() == 10140 && rc.getRoundNum() < 231) System.out.println("right");
                return rightDir;
            }

            if (!isOccupied(leftDir)) {
                state = BugState.ON_WALL;
                movedRight = false;
                myDirection = leftDir;
//                lastWallSpot = myLocation;
                startLocation = myLocation;
                if (rc.getID() == 10140 && rc.getRoundNum() < 231) System.out.println("left");
                return leftDir;
            }
        }

        return null;
    }

    private boolean isOccupied(Direction dir) throws GameActionException {
        MapLocation dest = myLocation.add(dir, myType.strideRadius);
        return !rc.onTheMap(dest, myType.bodyRadius) || rc.isCircleOccupiedExceptByThisRobot(dest, myType.bodyRadius);
    }

    private Direction followWall() throws GameActionException {
        if (rc.getID() == 10140 && rc.getRoundNum() < 231 ) System.out.println("followWall");
//        if (!myLocation.equals(lastWallSpot) && !lastWallSpot.add(myDirection, myType.strideRadius).equals(myLocation)) return resetWanderer();

        Direction next;
        for (int i = -90; i < 180; i += FOLLOW_WALL_GRANULARITY) {
            next = rotateInDir(myDirection, !movedRight, i);
            if (!isOccupied(next)) {
                myDirection = next;
//                lastWallSpot = myLocation;

                if (rc.getID() == 10140 && rc.getRoundNum() < 231) System.out.println("followed");
                return next;
            }

//            if (!rc.onTheMap(myLocation.add(next))) return null;
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


    private boolean currentMLineContains(MapLocation location) {

        float dxl = goal.x - start.x;
        float dyl = goal.y - start.y;

        float dist = Math.abs(dyl * location.x - dxl * location.y + goal.x * start.y - goal.y * start.x) / (float) Math.sqrt( Math.pow(dyl, 2) + Math.pow(dxl, 2));


        return dist < 1.0F && Math.abs(start.x - location.x) <= Math.abs(start.x - goal.x) && Math.abs(start.y - location.y) <= Math.abs(start.y - goal.y);
    }

}
