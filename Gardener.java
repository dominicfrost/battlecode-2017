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
        if (state == GOING_HOME) {
            goHome();
        } else if (state == MOVING_TO_PLANTING_LOCATION) {
            moveToOrPlant(destinationPlantCenter, destinationPlanterCenter);
        } else {
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
                // TODO: state machine where this is cached between rounds
//                if (ti.team != myTeam) continue;
                if (rc.getID() == 13337 && rc.getRoundNum() < 66) System.out.println("GOAL2");
                goal = plantLocationForTree(ti);

                if (rc.getID() == 13337 && rc.getRoundNum() < 66) System.out.println("FOUND");
                if (rc.getID() == 13337 && rc.getRoundNum() < 66) System.out.println(goal);
                if (goal != null) {

                    setMovingToPlantingLocation(goal[0], goal[1]);
                    if (rc.getID() == 13337 && rc.getRoundNum() < 66) System.out.println("moveToOrPlant");
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
        Direction plantDir = planterCenter.directionTo(plantCenter);
        if (rc.getID() == 13337 && rc.getRoundNum() < 66) System.out.println("plantCenter");
        if (rc.getID() == 13337 && rc.getRoundNum() < 66) System.out.println(plantCenter);
        if (rc.getID() == 13337 && rc.getRoundNum() < 66) System.out.println("planterCenter");
        if (rc.getID() == 13337 && rc.getRoundNum() < 66) System.out.println(planterCenter);

        if (location.equals(planterCenter)) {
            setTending();
            rc.plantTree(plantDir);
            return;
        }
        if (rc.canMove(planterCenter)) {
            if (rc.getID() == 13337 && rc.getRoundNum() < 66) System.out.println("loc");
            if (rc.getID() == 13337 && rc.getRoundNum() < 66) System.out.println(location);
            rc.move(planterCenter);
            location = plantCenter;
            if (rc.getID() == 13337 && rc.getRoundNum() < 66) System.out.println("getLocation");
            if (rc.getID() == 13337 && rc.getRoundNum() < 66) System.out.println(rc.getLocation());
//            Clock.yield();
            if (rc.getID() == 13337 && rc.getRoundNum() < 66) System.out.println("plantDir");
            if (rc.getID() == 13337 && rc.getRoundNum() < 66) System.out.println(plantDir);
            try {
                if (rc.getID() == 13337 && rc.getRoundNum() < 66) System.out.println("canPlantTree");
                if (rc.getID() == 13337 && rc.getRoundNum() < 66) System.out.println(rc.canPlantTree(plantDir));
                if (rc.getID() == 13337 && rc.getRoundNum() < 66) System.out.println("isoccupied");
                if (rc.getID() == 13337 && rc.getRoundNum() < 66) System.out.println(rc.isCircleOccupied(plantCenter, GameConstants.BULLET_TREE_RADIUS));
                if (rc.getID() == 13337 && rc.getRoundNum() < 66) System.out.println("isCircleOccupiedExceptByThisRobot");
                if (rc.getID() == 13337 && rc.getRoundNum() < 66) System.out.println(rc.isCircleOccupiedExceptByThisRobot(plantCenter, GameConstants.BULLET_TREE_RADIUS));
                if (rc.getID() == 13337 && rc.getRoundNum() < 66) System.out.println("isLocationOccupiedByTree");
                if (rc.getID() == 13337 && rc.getRoundNum() < 66) System.out.println(rc.isLocationOccupiedByTree(plantCenter));
                rc.plantTree(plantDir);
                setTending();
            } catch (Exception e) {
                if (rc.getID() == 13337 && rc.getRoundNum() < 66) System.out.println("e");
                if (rc.getID() == 13337 && rc.getRoundNum() < 66) System.out.println(e);
            }
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
        if (side != null && rc.onTheMap(side) && rc.canSenseAllOfCircle(side, GameConstants.BULLET_TREE_RADIUS)) {
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
