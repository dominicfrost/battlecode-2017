package battlecode2017;
import battlecode.common.*;

import java.util.Arrays;
import java.util.HashMap;

public class Scout extends Circle {
    private TreeInfo[] neutralTrees;
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

    @Override
    protected void initRobotState() throws GameActionException {
        super.initRobotState();
        setScouting();
        shakenTrees = new HashMap<>();
        scoutingDirection = rc.getLocation().directionTo(enemyArchonLocs[rc.getID() % enemyArchonLocs.length]);
    }

    @Override
    protected void initRoundState() throws GameActionException {
        super.initRoundState();
        setNeutralTrees();
        sortEnemies();
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
        randomSafeMove(location.directionTo(neutralTrees[0].location));
    }

    private RobotInfo findHarasee() {
        for (RobotInfo ri : nearbyEnemies) {
            if (ri.type.equals(RobotType.GARDENER)) {
                return ri;
            }
        }
        for (RobotInfo ri : nearbyEnemies) {
            if (ri.type.equals(RobotType.ARCHON)) {
                return ri;
            }
        }
        return null;
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
            shakenTrees.put(ti.getID(), true);
            return true;
        }

        return false;
    }

    private void determineAreasOfInterest() throws GameActionException {
        if (nearbyEnemies.length > 3) {
            int currentRoundNum = rc.getRoundNum();
            // write to the first AOI channel that is stale ( > 100 rounds old)
            DecodedLocation loc = Coms.decodeLocation(rc.readBroadcast(Coms.AREA_OF_INTEREST_1));
            if (loc == null || currentRoundNum - loc.roundNum > 100) {
                rc.broadcast(Coms.AREA_OF_INTEREST_1, Coms.encodeLocation(location, currentRoundNum));
                return;
            }

            loc = Coms.decodeLocation(rc.readBroadcast(Coms.AREA_OF_INTEREST_2));
            if (loc == null || currentRoundNum - loc.roundNum > 100) {
                rc.broadcast(Coms.AREA_OF_INTEREST_2, Coms.encodeLocation(location, currentRoundNum));
                return;
            }

            loc = Coms.decodeLocation(rc.readBroadcast(Coms.AREA_OF_INTEREST_3));
            if (loc == null || currentRoundNum - loc.roundNum > 100) {
                rc.broadcast(Coms.AREA_OF_INTEREST_3, Coms.encodeLocation(location, currentRoundNum));
            }
        }
    }
}