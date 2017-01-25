package battlecode2017;
import battlecode.common.*;
import davebot.*;


public class SparseGardener extends Circle {

    // HOW RICH WE ARE !!!!!
    private final int MIDDLE_CLASS_THRESHOLD = 20;
    private final int UPPER_CLASS_THRESHOLD = 40;

    private int buildCount;
    private boolean isBuildReady;
    private boolean hasWatered;

    private MapLocation gardenLocation;
    private MapLocation buildingLocation;
    private RobotType buildingType;
    private Direction scoutingDirection;

    private GardenerState state;

    private enum GardenerState {
        FINDING_PLANT_SPOT,
        WATERING,
        FINDING_SPAWN_SPOT,
    }

    private SocialClass socialClass;
    private float numBullets;

    SparseGardener(RobotController _rc) {
        super(_rc);
    }

    @Override
    protected void initRobotState() throws GameActionException {
        super.initRobotState();
        setFindingPlantSpot();
        buildCount = 0;

        gardenLocation = null;
        buildingLocation = null;
        buildingType = null;
    }

    @Override
    protected void initRoundState() throws GameActionException {
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
        hasWatered = false;
    }

    private void setState(GardenerState newState) {
        buildingLocation = null;
        buildingType = null;
        gardenLocation = null;
        scoutingDirection = null;
        state = newState;
    }

    protected void doTurn() throws GameActionException {
        postPeskyTrees();
        postPeskyAttackers();
        waterTree();
        switch (state) {
            case FINDING_PLANT_SPOT:
                findingPlantSpot();
                break;
            case WATERING:
                watering();
                break;
            case FINDING_SPAWN_SPOT:
                findingSpawnSpot();
        }
        waterTree();
    }

    private RobotType getBuildType() {
        switch(socialClass) {
            case LOWER:
                if (numBullets < 300) return null;
                return spawnUnitsWithThresholds(0, 0, 0, 100);
            case MIDDLE:
                if (numBullets < 300) return null;
                return spawnUnitsWithThresholds(0, 0, 0, 100);
            default: // UPPER
                if (numBullets < 500) return null;
                return spawnUnitsWithThresholds(0, 0, 0, 100);
        }
    }

    private RobotType spawnUnitsWithThresholds(float scoutThreshold, float lumberjackThreshold, float soldierThreshold, float tankThreshold) {
        double r = Math.random() * 100;
        if (r < scoutThreshold) {
            return RobotType.SCOUT;
        } else if (r < lumberjackThreshold) {
            return RobotType.LUMBERJACK;
        } else if (r < soldierThreshold) {
            return RobotType.SOLDIER;
        } else { // if (r < tankThreshold) {
            return RobotType.TANK;
        }
    }


    /*
     *
     *  Planting Logic
     *
     */

    private void setFindingPlantSpot() throws GameActionException {
        setState(GardenerState.FINDING_PLANT_SPOT);
    }

    private void findingPlantSpot() throws GameActionException {
        if (!shouldPlantTree()) {
            gardenLocation = null;
            setWatering();
            watering();
            return;
        }

        if (shouldBuildBot()) {
            gardenLocation = null;
            setFindingSpawnSpot();
            findingSpawnSpot();
            return;
        }

        if (gardenLocation == null )
            gardenLocation = findPlantSpot();
        else invalidateGardenLocationIfOccupied();

        if (gardenLocation == null) {
            stayAwayFromAllieGardenerAndArchons();
            return;
        }
        rc.setIndicatorDot(gardenLocation, 0,0,0);
        moveCirclingLocationWhileStayingOutOfGoal(gardenLocation, GameConstants.BULLET_TREE_RADIUS);
        if (atButNotOnCircleGoal(gardenLocation, GameConstants.BULLET_TREE_RADIUS) && isBuildReady && rc.hasTreeBuildRequirements()) {
            plantTree(location.directionTo(gardenLocation));
        }
    }

    private void invalidateGardenLocationIfOccupied() throws GameActionException {
        if (rc.canSenseAllOfCircle(gardenLocation, GameConstants.BULLET_TREE_RADIUS) &&
                rc.isCircleOccupiedExceptByThisRobot(gardenLocation, GameConstants.BULLET_TREE_RADIUS)) {
            gardenLocation = null;
        }
    }

    private MapLocation findPlantSpot() throws GameActionException {
        MapLocation spot;
        if (nearbyTrees.length == 0) {
            Direction r = randomPlantDir();
            if (r == null) return null;
            return location.add(r, myType.bodyRadius + GameConstants.BULLET_TREE_RADIUS);
        }
        for (TreeInfo ti : nearbyTrees) {
            if (!ti.team.equals(myTeam)) continue;
            spot = checkTreeForPlantSpot(ti);
            if (spot != null) return spot;
        }
        return null;
    }

    private MapLocation checkTreeForPlantSpot(TreeInfo ti) throws GameActionException {
        MapLocation spot;
        Direction toTree = ti.location.directionTo(location);
        float deg = ((int) toTree.getAngleDegrees() + 360) % 360;
        float startDir = ((deg - (deg % 90 )) + 45) * radian;

        spot = checkDirectionForPlantSpot(ti, new Direction(startDir));
        if (spot != null) return spot;
        spot = checkDirectionForPlantSpot(ti, new Direction(startDir + pi / 2));
        if (spot != null) return spot;
        spot = checkDirectionForPlantSpot(ti, new Direction(startDir - pi / 2));
        if (spot != null) return spot;
        spot = checkDirectionForPlantSpot(ti, new Direction(startDir + pi));
        return spot;
    }

    private MapLocation checkDirectionForPlantSpot(TreeInfo ti, Direction dir) throws GameActionException {
        MapLocation spot = ti.location.add(dir, ti.radius + 2.1F + GameConstants.BULLET_TREE_RADIUS);
        rc.setIndicatorDot(spot, 155,0,0);
        if (rc.canSenseAllOfCircle(spot, GameConstants.BULLET_TREE_RADIUS) &&
            rc.onTheMap(spot, GameConstants.BULLET_TREE_RADIUS) &&
            !rc.isCircleOccupiedExceptByThisRobot(spot, GameConstants.BULLET_TREE_RADIUS)) return spot;
        return null;
    }


    // TODO: this could be better
    protected void stayAwayFromAllieGardenerAndArchons() throws GameActionException {
        for (RobotInfo ri : nearbyAllies) {
            if ((ri.type.equals(RobotType.ARCHON) || ri.type.equals(RobotType.GARDENER)) &&
                    location.distanceSquaredTo(ri.location) < sqrFloat(ri.type.bodyRadius + 4F + myType.bodyRadius)) {
                randomSafeMove(ri.location.directionTo(location));
                return;
            }
        }
    }

    private boolean shouldPlantTree() {
        return rc.getRoundNum() / (rc.getTreeCount() + 1) > 100 && rc.hasTreeBuildRequirements();
    }

    /*
     *
     *  Building Logic
     *
     */
    private void setFindingSpawnSpot() {
        setState(GardenerState.FINDING_SPAWN_SPOT);
    }

    private void findingSpawnSpot() throws GameActionException {
        if (!shouldBuildBot()) {
            setWatering();
            watering();
            return;
        }

        if (buildingType == null)
            buildingType = getBuildType();
        else invalidateBuildTypeIfCantBuild();

        if (buildingType == null) {
            // Should never happen, but just in case
            setWatering();
            watering();
            return;
        }

        if (buildingLocation == null )
            buildingLocation = findBuildSpot(buildingType);
        else invalidateBuildLocationIfOccupied();

        if (buildingLocation == null) {
            if (!hasMoved) stayAwayFromAllieGardenerAndArchons();
            return;
        }
        rc.setIndicatorDot(buildingLocation, 0,55,233);
        moveCirclingLocationWhileStayingOutOfGoal(buildingLocation, buildingType.bodyRadius);
        if (atButNotOnCircleGoal(buildingLocation, buildingType.bodyRadius) && isBuildReady && rc.hasRobotBuildRequirements(buildingType)) {
            build(buildingType, location.directionTo(buildingLocation));
            setWatering();
        }
    }

    private void invalidateBuildLocationIfOccupied() throws GameActionException {
        if (rc.canSenseAllOfCircle(buildingLocation, myType.bodyRadius) &&
                rc.isCircleOccupiedExceptByThisRobot(buildingLocation, myType.bodyRadius)) {
            buildingLocation = null;
        }
    }

    private void invalidateBuildTypeIfCantBuild() throws GameActionException {
        if (!rc.hasRobotBuildRequirements(buildingType)) {
            buildingType = getBuildType();
        }
    }

    private MapLocation findBuildSpot(RobotType rt) throws GameActionException {
        if (rt.equals(RobotType.TANK)) return findBuildSpotForTank();

        MapLocation spot;
        if (nearbyTrees.length == 0) {
            Direction r = randomSpawnDir(rt);
            if (r == null) return null;
            return location.add(r, myType.bodyRadius + rt.bodyRadius);
        }
        for (TreeInfo ti : nearbyTrees) {
            if (!ti.team.equals(myTeam)) continue;
            spot = checkForBuildSpot(rt, ti);
            if (spot != null) return spot;
        }
        return null;
    }

    private MapLocation checkForBuildSpot(RobotType rt, TreeInfo ti) throws GameActionException {
        MapLocation spot;
        spot = checkDirectionForBuildSpot(rt, ti, Direction.getNorth());
        if (spot != null) return spot;
        spot = checkDirectionForBuildSpot(rt, ti, Direction.getEast());
        if (spot != null) return spot;
        spot = checkDirectionForBuildSpot(rt, ti, Direction.getSouth());
        if (spot != null) return spot;
        spot = checkDirectionForBuildSpot(rt, ti, Direction.getWest());
        return spot;
    }



    private MapLocation checkDirectionForBuildSpot(RobotType rt, TreeInfo ti, Direction dir) throws GameActionException {
        MapLocation spot = ti.location.add(dir, ti.radius + 2.0F + .05F);
        rc.setIndicatorDot(spot, 155,255,0);
        if (rc.canSenseAllOfCircle(spot, rt.bodyRadius) &&
            rc.onTheMap(spot, rt.bodyRadius) &&
            !rc.isCircleOccupiedExceptByThisRobot(spot, rt.bodyRadius)) return spot;
        return null;
    }

    private MapLocation findBuildSpotForTank() throws GameActionException {
        Direction toSpawn = randomSpawnDir(RobotType.TANK);
        if (toSpawn != null) return location.add(toSpawn, myType.bodyRadius + RobotType.TANK.bodyRadius);

        if (scoutingDirection == null)
            scoutingDirection = randomDirection();

        if (!rc.onTheMap(location.add(scoutingDirection, myType.sensorRadius - 1))) {
            scoutingDirection = randomDirection();
        }
        randomSafeMove(scoutingDirection);
        return null;
    }


    private boolean shouldBuildBot() {
        return getBuildType() != null;
    }

    /*
     *
     *  Watering Logic
     *
     */

    private void setWatering() {
        setState(GardenerState.WATERING);
    }

    private void watering() throws GameActionException {
        if (shouldBuildBot()) {
            setFindingSpawnSpot();
            findingSpawnSpot();
            return;
        }

        if (shouldPlantTree()) {
            setFindingPlantSpot();
            findingPlantSpot();
            return;
        }

        MapLocation needsWatered = findWhoNeedsWater();
        if (needsWatered != null)
            moveWithBugger(needsWatered, 0);
        else randomSafeMove(randomDirection());
    }

    private MapLocation findWhoNeedsWater() {
        MapLocation waterLoc = null;
        float waterLocHealth = Float.MAX_VALUE;

        for (TreeInfo ti : nearbyTrees) {
            if (!ti.team.equals(myTeam)) continue;
            if (ti.health < waterLocHealth) {
                waterLocHealth = ti.health;
                waterLoc = ti.location;
            }
        }

        return waterLoc;
    }

    private void waterTree() throws GameActionException {
        if (hasWatered) return;
        MapLocation waterLoc = null;
        float waterLocHealth = Float.MAX_VALUE;

        for (TreeInfo ti : nearbyTrees) {
            if (ti.team.equals(myTeam) && ti.health < waterLocHealth && rc.canWater(ti.location)) {
                waterLocHealth = ti.health;
                waterLoc = ti.location;
            }
        }

        if (waterLoc != null) water(waterLoc);
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

    private Direction randomPlantDir() {
        Direction nextDir;
        for (int i = 0; i < 12; i++) {
            nextDir = Direction.getNorth().rotateRightDegrees(i * 30);
            if (rc.canPlantTree(nextDir)) return nextDir;
        }
        return null;
    }

    private void plantTree(Direction dir) throws GameActionException {
        rc.plantTree(dir);
        isBuildReady = false;
    }

    private void build(RobotType rt, Direction dir) throws GameActionException {
        rc.buildRobot(rt, dir);
        isBuildReady = false;
    }

    private void water(MapLocation loc) throws GameActionException {
        rc.water(loc);
        hasWatered = true;
    }
}

