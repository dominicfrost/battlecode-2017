package battlecode2017;
import battlecode.common.*;
import jdk.nashorn.internal.ir.annotations.Ignore;

import java.util.*;

enum SocialClass {
    LOWER,
    MIDDLE,
    UPPER
}

public class Gardener extends Robot {

    // HOW RICH WE ARE !!!!!
    private final int MIDDLE_CLASS_THRESHOLD = 20;
    private final int UPPER_CLASS_THRESHOLD = 40;


    private final int BUILD_TO_PLANT_MODULUS = 3;
    private final int MAX_PLANT_REMAINDER = 3;
    private final float STOP_PLANTING_BULLET_COUNT = 100000F;
    private final float PLANTING_DISTANCE = GameConstants.BULLET_TREE_RADIUS * 2.1F;
    private final int IM_HOME = 100;
    private final int SPAWN_SEARCH_DEGREE = 30;
    private final float SPAWN_SEARCH_DEGREE_INTERVAL = 360.0F / SPAWN_SEARCH_DEGREE;

    private int buildCount;
    private boolean isBuildReady;
    private boolean plantVertically;
    private String state;

    // MOVING_TO_PLANTING_LOCATION cache
    private MapLocation destinationPlantCenter;
    private MapLocation destinationPlanterCenter;

    // Gardener states
    private final String TENDING = "TENDING";
    private final String MOVING_TO_PLANTING_LOCATION = "MOVING_TO_PLANTING_LOCATION";
    private final String GOING_HOME = "GOING_HOME";
    private final String SPAWNING= "SPAWNING";

    private Bug bugger;

    private SocialClass socialClass;
    private float numBullets;

    Gardener(RobotController _rc) {
        super(_rc);
    }

    @Override
    protected void initRobotState() throws GameActionException {
        super.initRobotState();
        buildCount = 0;
        plantVertically = true;
//        state = GOING_HOME;
        state = SPAWNING;
        bugger = new Bug(rc);
        bugger.setGoal(rc.getLocation(), home, 20);
    }

    @Override
    protected void initRoundState() {
        super.initRoundState();

        int treeCount = rc.getTreeCount();
        if (treeCount < MIDDLE_CLASS_THRESHOLD) {
            socialClass = SocialClass.LOWER;
        } else if (treeCount < UPPER_CLASS_THRESHOLD) {
            socialClass = SocialClass.MIDDLE;
        } else {
            socialClass = SocialClass.UPPER;
        }

        numBullets = rc.getTeamBullets();
        isBuildReady = rc.isBuildReady();
        Arrays.sort(nearbyTrees, new TreeComparator(location));
    }

    private void spawnUnits() throws GameActionException {
        switch(socialClass) {
            case LOWER:
                if (numBullets < 200) return;
                spawnUnitsWithThresholds(30, 60, 100, 0);
                break;
            case MIDDLE:
                if (numBullets < 300) return;
                spawnUnitsWithThresholds(10, 20, 100, 0);
                break;
            case UPPER:
                if (numBullets < 500) return;
                spawnUnitsWithThresholds(10, 20, 50, 100);
                break;
        }
    }

    private void spawnUnitsWithThresholds(float scoutThreshold, float lumberjackThreshold, float soldierThreshold, float tankThreshold) throws GameActionException {
        double r = Math.random();
        if (r < scoutThreshold) {
            trySpawn(RobotType.SCOUT, Direction.getNorth());
        } else if (r < lumberjackThreshold) {
            trySpawn(RobotType.LUMBERJACK, Direction.getNorth());
        } else if (r < soldierThreshold) {
            trySpawn(RobotType.SOLDIER, Direction.getNorth());
        } else if (r < tankThreshold) {
            trySpawn(RobotType.TANK, Direction.getNorth());
        }
    }

    private boolean trySpawn(RobotType type, Direction dir) throws GameActionException {
        if (rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        }
        return false;
    }


    protected void doTurn() throws GameActionException {
        if (tryDodge()) return; // don't try and plant or build if i'm dodgin mfkas
        saveThePoorTrees();

        debug("Gardener state: " + state);
        switch(state) {
            case SPAWNING:
                spawnUnits();
            case GOING_HOME:
                goHome();
                break;
            case MOVING_TO_PLANTING_LOCATION:
                moveToOrPlant(destinationPlantCenter, destinationPlanterCenter);
                break;
            case TENDING:
                if (tryPlantTree()) return;
                if (tryBuildBot()) return;
        }
//
//        if (!bugger.hasGoal()) {
//            bugger.setGoal(location, home, 20);
//        }
//        Direction d = bugger.nextStride(location, nearbyTrees);
//        if (d != null && rc.canMove(d)) {
//            rc.move(d);
//        }
    }

    private void saveThePoorTrees() throws GameActionException {
        for (TreeInfo t : nearbyTrees) {
            if (t.team == myTeam && t.health < 45 && rc.canWater(t.location)) rc.water(t.location);
        }
    }

    private void goHome() throws GameActionException {
        Direction next = bugger.nextStride(location, nearbyTrees);
        if (next != null && rc.canMove(next)) rc.move(next);
        if (location.distanceSquaredTo(home) <= IM_HOME) setTending();
    }

    private boolean shouldPlantTree() {
        if (bulletCount >= STOP_PLANTING_BULLET_COUNT) return false;
        debug("A" + rc.isBuildReady());
        if (!isBuildReady) return false;debug("B");
        if (!rc.hasTreeBuildRequirements()) return false;debug("C");
        if (buildCount % BUILD_TO_PLANT_MODULUS > MAX_PLANT_REMAINDER) return false;debug("D");
        return true;
    }

    private boolean tryPlantTree() throws GameActionException {
        MapLocation[] goal;
        if (shouldPlantTree()) {
            debug("Should plant tree");
            TreeInfo[] myTrees = Arrays.stream(nearbyTrees).filter(t -> t.team == myTeam).toArray(TreeInfo[]::new);

            for (TreeInfo ti : myTrees) {
                goal = plantLocationForTree(ti);
                if (goal != null) {
                    debug("Found planting locations " + goal.toString());
                    setMovingToPlantingLocation(goal[0], goal[1]);
                    moveToOrPlant(goal[0], goal[1]);
                    return true;
                }
            }

            if (location.distanceSquaredTo(home) < 4 && myTrees.length == 0) {
                Direction dir = getPlantDirection();
                if (dir != null) {
                    rc.plantTree(dir);
                    return true;
                }
            }

            if (location.directionTo(home) != null) tryMove(location.directionTo(home));
            return true;
        }
        return false;
    }

    private void setMovingToPlantingLocation(MapLocation plantCenter, MapLocation planterCenter) {
        state = MOVING_TO_PLANTING_LOCATION;
        destinationPlantCenter = plantCenter;
        destinationPlanterCenter = planterCenter;
    }

    private void setTending() {
        state = TENDING;
    }

    private void moveToOrPlant(MapLocation plantCenter, MapLocation planterCenter) throws GameActionException {
        debug("moveToOrPlant");
        debug("plantCenter " + plantCenter.toString());
        debug("planterCenter " + planterCenter.toString());
        debug("rc.hasTreeBuildRequirements() " + rc.hasTreeBuildRequirements());
        if (!rc.hasTreeBuildRequirements() || (rc.canSenseAllOfCircle(plantCenter, GameConstants.BULLET_TREE_RADIUS) && rc.isCircleOccupiedExceptByThisRobot(plantCenter, GameConstants.BULLET_TREE_RADIUS))) {

            debug("moveToOrPlant D");
            setTending();
            return;
        }

        Direction plantDir = planterCenter.directionTo(plantCenter);
        if (location.equals(planterCenter)) {
            debug("moveToOrPlant A");
            rc.plantTree(plantDir);
            setTending();
            return;
        }
        if (location.distanceSquaredTo(planterCenter) <= myType.strideRadius && rc.canMove(planterCenter)) {
            rc.move(planterCenter);
            debug("moveToOrPlant B");
            location = plantCenter;
            if (rc.canPlantTree(plantDir)) rc.plantTree(plantDir);
            setTending();
            return;
        }
        tryMove(location.directionTo(planterCenter));
    }
    private MapLocation[] plantLocationForATree(TreeInfo ti, boolean up, boolean add) throws GameActionException {
        MapLocation side;

        if (plantVertically) {
            if (up) {
                if (add) {
                    side = new MapLocation(ti.location.x, ti.location.y + PLANTING_DISTANCE);
                } else {
                    side = new MapLocation(ti.location.x, ti.location.y - PLANTING_DISTANCE);
                }
            } else {
                if (add) {
                    side = new MapLocation(ti.location.x + 2 * PLANTING_DISTANCE, ti.location.y);
                } else {
                    side = new MapLocation(ti.location.x - 2 * PLANTING_DISTANCE, ti.location.y);
                }
            }
        } else {
            if (up) {
                if (add) {
                    side = new MapLocation(ti.location.x + PLANTING_DISTANCE, ti.location.y);
                } else {
                    side = new MapLocation(ti.location.x - PLANTING_DISTANCE, ti.location.y);
                }
            } else {
                if (add) {
                    side = new MapLocation(ti.location.x, ti.location.y + 2 * PLANTING_DISTANCE);
                } else {
                    side = new MapLocation(ti.location.x, ti.location.y - 2 * PLANTING_DISTANCE);
                }
            }
        }


        MapLocation plantLoc = null;
        debug("Trying to find planting location around " + side.toString());
        if (rc.canSenseAllOfCircle(side, GameConstants.BULLET_TREE_RADIUS) && rc.onTheMap(side) && !rc.isCircleOccupiedExceptByThisRobot(side, GameConstants.BULLET_TREE_RADIUS)) {
            plantLoc = findSpotAroundCircle(side, GameConstants.BULLET_TREE_RADIUS, myType.bodyRadius);
        }

        if (plantLoc != null) return new MapLocation[]{side, plantLoc};
        return null;
    }


    private MapLocation[] plantLocationForTree(TreeInfo ti) throws GameActionException {
        MapLocation[] goal;
        int nextDir;
        Direction toHome = location.directionTo(home);
        for (int i = 0; i < 4; i++) {

            if (i == 0) {
                nextDir = (((int) toHome.getAngleDegrees()) + 360) % 360;
            } else if (i == 1) {
                nextDir = (((int) toHome.rotateRightDegrees(90F).getAngleDegrees()) + 360) % 360;
            } else if (i == 2) {
                nextDir = (((int) toHome.rotateLeftDegrees(90F).getAngleDegrees()) + 360) % 360;
            } else {
                nextDir = (((int) toHome.rotateLeftDegrees(180F).getAngleDegrees()) + 360) % 360;
            }

            if (nextDir > 45 && nextDir < 135) {
                goal = plantLocationForATree(ti, true, true);
                if (goal != null) return goal;
            } else if (nextDir < 225) {
                goal = plantLocationForATree(ti, false, false);
                if (goal != null) return goal;
            } else if (nextDir < 315) {
                goal = plantLocationForATree(ti, true, false);
                if (goal != null) return goal;
            } else {
                goal = plantLocationForATree(ti, false, true);
                if (goal != null) return goal;
            }
        }

//        boolean a = randInt == 1;
//        boolean b = randInt == 0;
//
//        goal = plantLocationForATree(ti, up, add);
//        if (goal != null) return goal;
//        goal = plantLocationForATree(ti, a, b);
//        if (goal != null) return goal;
//        goal = plantLocationForATree(ti, b, a);
//        if (goal != null) return goal;
//        goal = plantLocationForATree(ti, b, b);
//        if (goal != null) return goal;

        return null;
    }

    protected Direction getPlantDirection() {
        Direction dir;
        for (int i = 0; i < SPAWN_SEARCH_DEGREE_INTERVAL; i++) {
            dir = Direction.getNorth().rotateRightDegrees(SPAWN_SEARCH_DEGREE * i);
            if (rc.canPlantTree(dir)) return dir;
        }
        return null;
    }

    private boolean shouldBuildBot(RobotType t) {
        if (!isBuildReady) return false;
        if (!rc.hasRobotBuildRequirements(t)) return false;
        return true;
    }

    private boolean tryBuildBot() throws GameActionException {
        RobotType toBuild = getBuildType();

        if (shouldBuildBot(toBuild)) {
            Direction buildDir = getBuildDirection(toBuild);
            if (buildDir == null) {
                rc.buildRobot(toBuild, buildDir);
            }
            tryMove(location.directionTo(home).opposite());
            return true;
        }
        return false;
    }

    private RobotType getBuildType() {
        return RobotType.SCOUT;
    }

}
