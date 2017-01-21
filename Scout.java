package battlecode2017;
import battlecode.common.*;

import java.util.Arrays;
import java.util.HashMap;

public class Scout extends Robot {
    private TreeInfo[] neutralTrees;
    private boolean hasShakenTree;
    private RobotInfo harassee;
    private HashMap<Integer, Boolean> shakenTrees;
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

    protected void initRobotState() throws GameActionException {
        super.initRobotState();
        setScouting();
        shakenTrees = new HashMap<>();
        scoutingDirection = rc.getLocation().directionTo(enemyArchonLocs[rc.getID() % enemyArchonLocs.length]);
    }

    protected void initRoundState() throws GameActionException {
        super.initRoundState();
        setNeutralTrees();
        sortEnemies();
        hasShakenTree = false;
    }

    private void setNeutralTrees() {
        neutralTrees = Arrays.stream(nearbyTrees).filter(t -> t.team.equals(Team.NEUTRAL) && t.containedBullets > 0 && shakenTrees.get(t.getID()) == null).toArray(TreeInfo[]::new);
        TreeComparator comp = new TreeComparator(location);
        Arrays.sort(neutralTrees, comp);
    }

    private void sortEnemies() {
        BotComparator comp = new BotComparator(location);
        Arrays.sort(nearbyEnemies, comp);
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
        tryMove(scoutingDirection);
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

        MapLocation dest = findSpotAroundHarassee();
        if (dest == null) {
            tryMove(location.directionTo(harassee.location));
            return;
        }

        if (canAttackHarasee()) {
            spray(location.directionTo(harassee.location));
            return;
        }

        tryMove(location.directionTo(dest));
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
        tryMove(location.directionTo(neutralTrees[0].location));
    }

    private RobotInfo findHarasee() {
        for (RobotInfo ri : nearbyEnemies) {
            if (ri.type.equals(RobotType.GARDENER) || ri.type.equals(RobotType.ARCHON)) {
                return ri;
            }
        }
        return null;
    }

    private void tryShakeTrees() throws GameActionException {
        if (hasShakenTree) return;
        for (TreeInfo ti: neutralTrees) {
            if (!closeEnoughToShake(ti)) return;
            if (tryShakeTree(ti)) return;
        }
    }

    private boolean closeEnoughToShake(TreeInfo ti) {
        return location.distanceSquaredTo(ti.location) < ti.radius + myType.bodyRadius + 1F;
    }

    private boolean tryShakeTree(TreeInfo ti) throws GameActionException {
        if (rc.canShake(ti.getID())) {
            rc.shake(ti.getID());
            shakenTrees.put(ti.getID(), true);
            hasShakenTree = true;
            return true;
        }
        return false;
    }

    private void determineAreasOfInterest() throws GameActionException {
        if (nearbyEnemies.length > 3) {
            int currentRoundNum = rc.getRoundNum();
            // write to the first AOI channel that is stale ( > 100 rounds old)
            DecodedLocation loc = Coms.decodeLocation(rc.readBroadcast(Coms.AREA_OF_INTEREST_1));
            if (loc.roundNum == 0 || currentRoundNum - loc.roundNum > 100) {
                rc.broadcast(Coms.AREA_OF_INTEREST_1, Coms.encodeLocation(location, currentRoundNum));
                return;
            }

            loc = Coms.decodeLocation(rc.readBroadcast(Coms.AREA_OF_INTEREST_2));
            if (loc.roundNum == 0 || currentRoundNum - loc.roundNum > 100) {
                rc.broadcast(Coms.AREA_OF_INTEREST_2, Coms.encodeLocation(location, currentRoundNum));
                return;
            }

            loc = Coms.decodeLocation(rc.readBroadcast(Coms.AREA_OF_INTEREST_3));
            if (loc.roundNum == 0 || currentRoundNum - loc.roundNum > 100) {
                rc.broadcast(Coms.AREA_OF_INTEREST_3, Coms.encodeLocation(location, currentRoundNum));
            }
        }
    }

    private boolean canAttackHarasee() {
        float distanceToCenterSquared = (float) Math.pow(harassee.type.bodyRadius + myType.bodyRadius, 2);
        return location.distanceSquaredTo(harassee.location) <= distanceToCenterSquared;
    }

    private MapLocation findSpotAroundHarassee() throws GameActionException {
        float distanceToCenter = harassee.type.bodyRadius + myType.bodyRadius;

        MapLocation nextLoc;
        Direction nextDir = Direction.getNorth();
        float dist;
        MapLocation closestLoc = null;
        float closestDist = Float.MAX_VALUE;

        for (int i = 0; i < 6; i++) {
            nextDir = nextDir.rotateLeftDegrees(60 * i);
            nextLoc = harassee.location.add(nextDir, distanceToCenter);
            if (rc.canSenseAllOfCircle(nextLoc, myType.bodyRadius) && rc.onTheMap(nextLoc) && rc.isLocationOccupiedByTree(nextLoc)) {
                nextLoc = rc.senseTreeAtLocation(nextLoc).location;
                dist = location.distanceSquaredTo(nextLoc);
                if (dist < closestDist) {
                    closestLoc = nextLoc;
                    closestDist = dist;
                }
            }
        }

        return closestLoc;
    }
}