package battlecode2017;
import battlecode.common.*;

import java.util.Arrays;
import java.util.HashMap;

public class Scout extends Circle {
    private TreeInfo[] neutralTrees;
    private RobotInfo harassee;
    private ScoutState state;
    private Direction scoutingDirection;

    private enum ScoutState {
        SHAKING_TREES,
        HARASSING,
        SCOUTING,
    }

    Scout(RobotController _rc) {
        super(_rc);
    }

    @Override
    protected void initRobotState() throws GameActionException {
        super.initRobotState();
        setScouting();
        scoutingDirection = rc.getLocation().directionTo(enemyArchonLocs[rc.getID() % enemyArchonLocs.length]);
    }

    @Override
    protected void initRoundState() throws GameActionException {
        super.initRoundState();
        setNeutralTrees();
    }

    private void setNeutralTrees() {
        neutralTrees = Arrays.stream(nearbyTrees).filter(t -> t.containedBullets > 0).toArray(TreeInfo[]::new);
    }

    protected void doTurn() throws GameActionException {
        determineAreasOfInterest();
        attackIfWayClose();
        tryShakeTrees();
        tryDodge();
        switch (state) {
            case SCOUTING:
                scout();
                break;
            case HARASSING:
                harass();
                break;
            case SHAKING_TREES:
                moveToShakableTree();
                break;
        }
        tryShakeTrees();
        attackIfWayClose();
        pieCountAttack();
    }

    private void setScouting() {
        state = ScoutState.SCOUTING;
        harassee = null;
    }

    private void scout() throws GameActionException {
        if (hasMoved) return;

        RobotInfo newHarasee = findHarasee();
        if (newHarasee != null) {
            setHarasing(newHarasee);
            harass();
            return;

        }

        if (neutralTrees.length > 0) {
            setShaking();
            moveToShakableTree();
            return;

        }

        if (!rc.onTheMap(location.add(scoutingDirection, myType.sensorRadius - 1))) {
            scoutingDirection = randomDirection();
        }
        randomSafeMove(scoutingDirection);
    }

    private void setHarasing(RobotInfo newHarassee) throws GameActionException {
        state = ScoutState.HARASSING;
        harassee = newHarassee;
        rc.setIndicatorLine(location, harassee.location, 255, 0,0);
    }

    private void harass() throws GameActionException {
        RobotInfo newHarasee = findHarasee();
        if (newHarasee != null) {
            setHarasing(newHarasee);
        } else {
            setScouting();
            scout();
            return;
        }

        moveCirclingLocation(harassee.location, harassee.getRadius());
        attackCircleGoal(harassee.location, harassee.getRadius());
    }

    private void setShaking() throws GameActionException {
        state = ScoutState.SHAKING_TREES;
        harassee = null;
    }

    private void moveToShakableTree() throws GameActionException {
        RobotInfo newHarasee = findHarasee();
        if (newHarasee != null) {
            setHarasing(newHarasee);
            harass();
            return;

        }

        if (neutralTrees.length == 0) {
            setScouting();
            scout();
            return;
        }

        rc.setIndicatorLine(location, neutralTrees[0].location, 0, 255,0);
        TreeInfo closestNeutralTree = getClosestNeutralTree();
        randomSafeMove(location.directionTo(closestNeutralTree.location));
    }

    private TreeInfo getClosestNeutralTree() {
        float minDist = Float.MAX_VALUE;
        TreeInfo closest = null;
        float nextDist;

        for (TreeInfo ti : neutralTrees) {

            nextDist = location.distanceSquaredTo(ti.location);
            if (nextDist < minDist) {
                minDist = nextDist;
                closest = ti;
            }
        }

        return closest;
    }

    private RobotInfo findHarasee() {
        RobotInfo next = getClosestBotOfType(RobotType.GARDENER);
        if (next != null) return next;
        return getClosestBotOfType(RobotType.ARCHON);
    }

    private RobotInfo getClosestBotOfType(RobotType robotType) {
        float minDist = Float.MAX_VALUE;
        RobotInfo closest = null;
        float nextDist;

        for (RobotInfo ri : nearbyEnemies) {
            if (!ri.type.equals(robotType)) continue;
            nextDist = location.distanceSquaredTo(ri.location);
            if (nextDist < minDist) {
                minDist = nextDist;
                closest = ri;
            }
        }

        return closest;
    }

    @Override
    protected void tryShakeTrees() throws GameActionException {
        if (hasShakenTree) return;
        for (TreeInfo ti: neutralTrees) {
            if (!closeEnoughToShake(ti)) return;
            if (tryShakeTree(ti)) return;
        }
    }

    private boolean closeEnoughToShake(TreeInfo ti) {
        return location.distanceSquaredTo(ti.location) < ti.radius + myType.bodyRadius + 1F;
    }

    @Override
    protected boolean tryShakeTree(TreeInfo ti) throws GameActionException {
        if (super.tryShakeTree(ti)) {
            return true;
        }

        return false;
    }

    private void determineAreasOfInterest() throws GameActionException {
        if (nearbyEnemies.length > 3) {
            MapLocation aoi = avgEnemyLoc();
            int currentRoundNum = rc.getRoundNum();
            // write to the first AOI channel that is stale ( > 100 rounds old)
            DecodedLocation loc = Coms.decodeLocation(rc.readBroadcast(Coms.AREA_OF_INTEREST_1));
            if (loc == null || currentRoundNum - loc.roundNum > 100) {
                rc.broadcast(Coms.AREA_OF_INTEREST_1, Coms.encodeLocation(aoi, currentRoundNum));
                return;
            }

            loc = Coms.decodeLocation(rc.readBroadcast(Coms.AREA_OF_INTEREST_2));
            if (loc == null || currentRoundNum - loc.roundNum > 100) {
                rc.broadcast(Coms.AREA_OF_INTEREST_2, Coms.encodeLocation(aoi, currentRoundNum));
                return;
            }

            loc = Coms.decodeLocation(rc.readBroadcast(Coms.AREA_OF_INTEREST_3));
            if (loc == null || currentRoundNum - loc.roundNum > 100) {
                rc.broadcast(Coms.AREA_OF_INTEREST_3, Coms.encodeLocation(aoi, currentRoundNum));
            }
        }
    }

    private MapLocation avgEnemyLoc() throws GameActionException {
        float avgX = 0;
        float avgY = 0;
        int count = 0;
        for (RobotInfo e: nearbyEnemies) {
            avgX += e.location.x;
            avgY += e.location.y;
            count++;
        }
        return new MapLocation(avgX / count, avgY / count);
    }
}