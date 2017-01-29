package battlecode2017;
import battlecode.common.*;

enum SocialClass {
    LOWER,
    MIDDLE,
    UPPER
}

public class Gardener extends Bugger {

    // HOW RICH WE ARE !!!!!
    private final int MIDDLE_CLASS_THRESHOLD = 20;
    private final int UPPER_CLASS_THRESHOLD = 40;

    private int ljCount;
    private int soldierCount;
    private int tankCount;
    private int scoutCount;

    private final float GARDEN_SPACE = sqrFloat(GameConstants.BULLET_TREE_RADIUS * 4 + RobotType.GARDENER.bodyRadius) + .001F;
    private int buildCount;
    private boolean isBuildReady;

    private MapLocation gardenLocation;

    private GardenerState state;

    private enum GardenerState {
        FINDING_GARDEN,
        AT_GARDEN,
        //        MOVING_TO_SPAWN,
        MOVING_TO_GARDEN,
    }

    private SocialClass socialClass;
    private float numBullets;

    Gardener(RobotController _rc) {
        super(_rc);
    }

    @Override
    protected void initRobotState() throws GameActionException {
        super.initRobotState();
        setFindingGarden();
        buildCount = 0;
        ljCount = 0;
        soldierCount = 0;
        tankCount = 0;
        scoutCount = 0;
    }

    @Override
    protected void initRoundState() throws GameActionException {
        updateBotCounts();
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
    }

    private void updateBotCounts() throws GameActionException {
        ljCount = rc.readBroadcast(Coms.LUMBERJACK_COUNT) - ljCount;
        soldierCount = rc.readBroadcast(Coms.SOLDIER_COUNT) - soldierCount;
        tankCount = rc.readBroadcast(Coms.TANK_COUNT) - tankCount;
        scoutCount = rc.readBroadcast(Coms.SCOUT_COUNT) - scoutCount;
    }

    protected void doTurn() throws GameActionException {
        postPeskyTrees();
        postPeskyAttackers();
        spawnOrBuild();
        switch (state) {
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

    private void spawnOrBuild() throws GameActionException {
        if (buildCount == 0) {
            trySpawn(RobotType.LUMBERJACK, randomSpawnDir(RobotType.LUMBERJACK));
            return;
        }
        if (buildCount == 2) {
            trySpawn(RobotType.SCOUT, randomSpawnDir(RobotType.SCOUT));
            return;
        }
        if (buildCount % 2 == 1 && gardenLocation != null && gardenLocation.equals(location) && shouldPlantTree() && plantTree()) return;
        spawnUnits();
    }

    private void spawnUnits() throws GameActionException {
        switch(socialClass) {
            case LOWER:
                if (numBullets < 200) return;
                // scout, lumberjack, soldier, tank
                spawnUnitsWithThresholds(33, 66, 100, 0);
                break;
            case MIDDLE:
                if (numBullets < 300) return;
                spawnUnitsWithThresholds(20, 30, 75, 100);
                break;
            case UPPER:
                if (numBullets < 500) return;
                spawnUnitsWithThresholds(10, 10, 20, 100);
                break;
        }
    }

    private void spawnUnitsWithThresholds(float scoutThreshold, float lumberjackThreshold, float soldierThreshold, float tankThreshold) throws GameActionException {
        double r = Math.random() * 100;
        if (r < scoutThreshold) {
            trySpawn(RobotType.SCOUT, spawnLocationFromGarden());
        } else if (r < lumberjackThreshold) {
            trySpawn(RobotType.LUMBERJACK, spawnLocationFromGarden());
        } else if (r < soldierThreshold) {
            trySpawn(RobotType.SOLDIER, spawnLocationFromGarden());
        } else if (r < tankThreshold) {
            trySpawn(RobotType.TANK, spawnLocationFromGarden());
        }
    }

    private boolean trySpawn(RobotType type, Direction dir) throws GameActionException {
        if (dir == null) return false;
        if (rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            buildCount++;
            return true;
        }
        return false;
    }


    private void setFindingGarden() throws GameActionException {
        state = GardenerState.FINDING_GARDEN;
    }

    private void findingGarden() throws GameActionException {
        gardenLocation = findGarden();
        if (gardenLocation == null) {
            stayAwayFromAllieGardenerAndArchons();
            return;
        }
        setMovingToGarden();
        moveToGarden();
    }

    protected void stayAwayFromAllieGardenerAndArchons() throws GameActionException {
        for (RobotInfo ri : nearbyAllies) {
            if ((ri.type.equals(RobotType.ARCHON) || ri.type.equals(RobotType.GARDENER)) &&
                    location.distanceSquaredTo(ri.location) < sqrFloat(ri.type.bodyRadius + 4F + myType.bodyRadius)) {
                safeMove(ri.location.directionTo(location));
                return;
            }
        }
    }

    private void setAtGarden() {
        state = GardenerState.AT_GARDEN;
    }

    private void atGarden() throws GameActionException {
        waterTree();
    }

    private void setMovingToGarden() {
        state = GardenerState.MOVING_TO_GARDEN;
    }

    private void moveToGarden() throws GameActionException {
        if (isLegitSpot(location)) {
            gardenLocation = location;
            setAtGarden();
            atGarden();
            return;
        }

        if (gardenLocation.equals(location)) {
            setAtGarden();
            atGarden();
            return;
        }

        if (!isLegitSpot(gardenLocation)) {
            setFindingGarden();
            findingGarden();
            return;
        }

        moveWithBugger(gardenLocation, 0);
    }

    private MapLocation findGarden() throws GameActionException {
        if (isLegitSpot(location)) return location;

        // search until we are out of bytecode or we find a legit spot
        int iterations = 0;
        int granularity;
        Direction nextDir;
        MapLocation nextLoc;
        Direction startDir = location.directionTo(enemyArchonLocs[rc.getID() % enemyArchonLocs.length]);
        while (true) {
            iterations++;
            granularity = 12 * iterations;
            for (int i = 0; i < granularity; i++) {
                if (Clock.getBytecodesLeft() < 2000) return null;
                nextDir = startDir.rotateRightDegrees(i * 30 / iterations);
                nextLoc = location.add(nextDir, myType.strideRadius * iterations);
                if (rc.canMove(nextDir, myType.strideRadius) && isLegitSpot(nextLoc)) return nextLoc;
            }
        }
    }

    private void waterTree() throws GameActionException {
        MapLocation waterLoc = null;
        float waterLocHealth = 9999F;

        MapLocation nextLoc;
        Direction nextDir;
        TreeInfo tree;
        for (int i = 0; i < 5; i++) {
            nextDir = Direction.getNorth().rotateRightDegrees(i * 60);
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
        for (int i = 0; i < 5; i++) {
            nextDir = Direction.getNorth().rotateRightDegrees(i * 60);
            if (rc.canPlantTree(nextDir)) {
                rc.plantTree(nextDir);
                buildCount++;
                return true;
            }
        }
        return false;
    }

    private boolean isLegitSpot(MapLocation loc) throws GameActionException {
        Direction nextDir;
        MapLocation nextLoc;
        for (int i = 0; i < 5; i++) {
            nextDir = Direction.getNorth().rotateRightDegrees(i * 60);
            nextLoc = loc.add(nextDir, myType.bodyRadius + GameConstants.BULLET_TREE_RADIUS);
            if (!rc.canSenseAllOfCircle(nextLoc, GameConstants.BULLET_TREE_RADIUS)) return false;
            if (!rc.onTheMap(nextLoc, GameConstants.BULLET_TREE_RADIUS)) return false;
            if (locInGardenerRange(nextLoc)) return false;
            if (!rc.canSenseAllOfCircle(nextLoc, GameConstants.BULLET_TREE_RADIUS)) return false;
            if (rc.isCircleOccupiedExceptByThisRobot(nextLoc, GameConstants.BULLET_TREE_RADIUS)) return false;
        }
        return true;
    }

    private boolean locInGardenerRange(MapLocation nextLoc) {
        for (RobotInfo ri : nearbyAllies) {
            if (ri.type == RobotType.GARDENER) {
                if (nextLoc.distanceSquaredTo(ri.location) <= GARDEN_SPACE) return true;
            }
        }
        return false;
    }

    private boolean shouldPlantTree() {
        if (!isBuildReady) return false;
        if (!rc.hasTreeBuildRequirements()) return false;
        return true;
    }

    private Direction spawnLocationFromGarden() {
        return Direction.getNorth().rotateRightDegrees(5 * 60);
    }

    private Direction randomSpawnDir(RobotType rt) {
        if (!rc.hasRobotBuildRequirements(rt)) return null;
        Direction nextDir;
        for (int i = 0; i < 6; i++) {
            nextDir = Direction.getNorth().rotateRightDegrees(i * 60);
            if (rc.canBuildRobot(rt, nextDir)) return nextDir;
        }
        return null;
    }
}

