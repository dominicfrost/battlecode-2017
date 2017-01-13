package battlecode2017;
import battlecode.common.*;

import java.util.*;

public class Gardener extends Robot {
    private final int BUILD_TO_PLANT_MODULUS = 3;
    private final int MAX_PLANT_REMAINDER = 3;
    private int buildCount;
    private boolean isBuildReady;

    private MapLocation gardenLocation;

    private Bug bugger;
    private GardenerState state;

    private enum GardenerState {
        FINDING_GARDEN,
        AT_GARDEN,
//        MOVING_TO_SPAWN,
        MOVING_TO_GARDEN,
    }

    Gardener(RobotController _rc) {
        super(_rc);
    }

    @Override
    protected void initRobotState() throws GameActionException {
        super.initRobotState();
        buildCount = 0;
        setFindingGarden();
        bugger = new Bug(rc);
        bugger.setGoal(rc.getLocation(), home, 20);
    }

    @Override
    protected void initRoundState() {
        super.initRoundState();
        isBuildReady = rc.isBuildReady();
    }

    protected void doTurn() throws GameActionException {
        debug("Gardener state: " + state);
        switch(state) {
            case FINDING_GARDEN:
                findingGarden();
                break;
            case AT_GARDEN:
                atGarden();
                break;
//            case MOVING_TO_SPAWN_:
//                moveToSpawn();
//                break;
            case MOVING_TO_GARDEN:
                moveToGarden();
        }
    }

    private void setFindingGarden() throws GameActionException {
        state = GardenerState.FINDING_GARDEN;
    }

    private void findingGarden() throws GameActionException {
        gardenLocation = findGarden();
        setMovingToGarden();
        moveToGarden();
    }

    private void setAtGarden() {
        state = GardenerState.AT_GARDEN;
    }

    private void atGarden() throws GameActionException {
        waterTree();
        if (shouldPlantTree() && plantTree()) return;

        RobotType t = getBuildType();
        if (shouldBuildBot(t)) tryBuildBot(t);
    }

//    private void setMovingToSpawn() {
//        state = GardenerState.MOVING_TO_SPAWN;
//    }
//
//    private void moveToSpawn() throws GameActionException {
//        if ()
//    }

    private void setMovingToGarden() {
        state = GardenerState.MOVING_TO_GARDEN;
    }

    private void moveToGarden() throws GameActionException {
        if (gardenLocation.equals(location)) {
            setAtGarden();
            atGarden();
            return;
        }

        if (!bugger.goal().equals(gardenLocation)) bugger.setGoal(location, gardenLocation, 0);
        Direction toGarden = bugger.nextStride(location, nearbyTrees);
        if (toGarden != null && rc.canMove(toGarden)) rc.move(toGarden);
    }

    private MapLocation findGarden() {
        return location;
    }

    private void waterTree() throws GameActionException {
        MapLocation waterLoc = null;
        float waterLocHealth = 9999F;

        MapLocation nextLoc;
        Direction nextDir;
        TreeInfo tree;
        for (int i = 0; i < 4; i++) {
            nextDir = Direction.getNorth().rotateRightDegrees(i * 72);
            nextLoc = gardenLocation.add(nextDir, myType.bodyRadius + GameConstants.BULLET_TREE_RADIUS);
            tree = rc.senseTreeAtLocation(nextLoc);
            if (tree != null && tree.team.equals(myTeam) && rc.canWater(nextLoc) && tree.health < waterLocHealth) {
                waterLoc = nextLoc;
                waterLocHealth = tree.health;
            }
        }

        if (waterLoc != null) rc.water(waterLoc);
    }


    private boolean plantTree() throws GameActionException {
        Direction nextDir;
        for (int i = 0; i < 4; i++) {
            nextDir = Direction.getNorth().rotateRightDegrees(i * 72);
            if (rc.canPlantTree(nextDir)) {
                rc.plantTree(nextDir);
                return true;
            }
        }
        return false;
    }

    private boolean shouldPlantTree() {
        if (!isBuildReady) return false;
        if (!rc.hasTreeBuildRequirements()) return false;
//        if (buildCount % BUILD_TO_PLANT_MODULUS > MAX_PLANT_REMAINDER) return false;
        return true;
    }

    private boolean shouldBuildBot(RobotType t) {
        if (!isBuildReady) return false;
        if (!rc.hasRobotBuildRequirements(t)) return false;
        return true;
    }

    private boolean tryBuildBot(RobotType rt) throws GameActionException {
        Direction toBuild = Direction.getNorth().rotateRightDegrees(288);
        if (rc.canBuildRobot(rt, toBuild)) {
            rc.buildRobot(rt, toBuild);
            return true;
        }
        return false;
    }

    private RobotType getBuildType() {
        return RobotType.SCOUT;
    }

}

