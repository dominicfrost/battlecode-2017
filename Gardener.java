package battlecode2017;
import battlecode.common.*;
import java.util.*;

public class Gardener extends Robot {
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

    // MOVING_TO_PLANTING_LOCATION stuff
    private MapLocation destinationPlantCenter;
    private MapLocation destinationPlanterCenter;

    private final String TENDING = "TENDING";
    private final String MOVING_TO_PLANTING_LOCATION = "MOVING_TO_PLANTING_LOCATION";
    private final String GOING_HOME = "GOING_HOME";

//    private enum states{
//        TENDING,
//        MOVING_TO_PLANTING_LOCATION,
//    }

    Gardener(RobotController _rc) {
        super(_rc);
    }

    @Override
    protected void initRobotState() throws GameActionException {
        super.initRobotState();
        buildCount = 0;
        plantVertically = true;
        state = TENDING;
    }

    @Override
    protected void initRoundState() {
        super.initRoundState();
        isBuildReady = rc.isBuildReady();
        Arrays.sort(nearbyTrees, new TreeComparator(location));
    }

    protected void doTurn() throws GameActionException {
        if (tryDodge()) return; // don't try and plant or build if i'm dodgin mfkas
        saveThePoorTrees();

        debug("Gardener state: " + state);
        switch(state) {
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
    }

    private void saveThePoorTrees() throws GameActionException {
        for (TreeInfo t : nearbyTrees) {
            if (t.team == myTeam && t.health < 20 && rc.canWater(t.location)) rc.water(t.location);
        }
    }

    private void goHome() throws GameActionException {
        if (location.directionTo(home) != null) tryMove(location.directionTo(home));
        if (location.distanceSquaredTo(home) <= IM_HOME) setTending();
    }

    private boolean shouldPlantTree() {
        if (bulletCount >= STOP_PLANTING_BULLET_COUNT) return false;
        if (!isBuildReady) return false;
        if (!rc.hasTreeBuildRequirements()) return false;
        if (buildCount % BUILD_TO_PLANT_MODULUS > MAX_PLANT_REMAINDER) return false;
        return true;
    }

    private boolean tryPlantTree() throws GameActionException {
        MapLocation[] goal;
        if (shouldPlantTree()) {
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

        Direction plantDir = planterCenter.directionTo(plantCenter);
        if (location.equals(planterCenter)) {
            rc.plantTree(plantDir);
            setTending();
            return;
        }
        if (rc.canMove(planterCenter)) {
            rc.move(planterCenter);
            location = plantCenter;
            rc.plantTree(plantDir);
            setTending();
        }
    }
    private MapLocation[] plantLocationForATree(TreeInfo ti, boolean next, boolean add) throws GameActionException {
        MapLocation side;

        if (plantVertically) {
            if (next) {
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
            if (next) {
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
        if (rc.onTheMap(side) && rc.canSenseAllOfCircle(side, GameConstants.BULLET_TREE_RADIUS) && rc.isCircleOccupiedExceptByThisRobot(side, GameConstants.BULLET_TREE_RADIUS)) {
            plantLoc = findSpotAroundCircle(side, GameConstants.BULLET_TREE_RADIUS, myType.bodyRadius);
        }

        if (plantLoc != null) return new MapLocation[]{side, plantLoc};
        return null;
    }


    private MapLocation[] plantLocationForTree(TreeInfo ti) throws GameActionException {
        MapLocation[] goal = plantLocationForATree(ti, true, true);
        if (goal != null) return goal;
        goal = plantLocationForATree(ti, true, false);
        if (goal != null) return goal;
        goal = plantLocationForATree(ti, false, true);
        if (goal != null) return goal;
        goal = plantLocationForATree(ti, false, false);
        if (goal != null) return goal;
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
