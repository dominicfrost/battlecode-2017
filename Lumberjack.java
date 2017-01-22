package battlecode2017;
import battlecode.common.*;

public class Lumberjack extends Robot {

    private Bug bugger;

    Lumberjack(RobotController _rc) {
        super(_rc);
    }

    @Override
    protected void initRobotState() throws GameActionException {
        super.initRobotState();
        bugger = new Bug(rc);

    }

    @Override
    protected void initRoundState() throws GameActionException {
        super.initRoundState();
    }

    protected void doTurn() throws GameActionException {
        tryDodge();

        MapLocation nearestTree = nearestPeskyTree();
        MapLocation nearestAttacker = nearestPeskyAttacker();

        tryShakeTrees();
        if (shouldStrike()) strike();
        chopTreeWithRobot();

        if (nearestTree == null && nearestAttacker == null) {
            findNextSpot();
        } else if (nearestAttacker == null || (nearestTree != null && location.distanceSquaredTo(nearestTree) < location.distanceSquaredTo(nearestAttacker))) {
            rc.setIndicatorLine(location, nearestTree, 0,0,255);
            moveToNearest(nearestTree);
            tryChop(nearestTree);
        } else {
            rc.setIndicatorLine(location, nearestAttacker, 0,0,255);
            moveToNearest(nearestAttacker);
        }

        tryShakeTrees();
        if (shouldStrike()) strike();
        chopTreeWithRobot();
    }


    private void findNextSpot() throws GameActionException  {
        float minDist = Float.MAX_VALUE;
        MapLocation closestLoc = null;
        float nextDist;

        for (TreeInfo ti: nearbyTrees) {
            nextDist = location.distanceSquaredTo(ti.location);
            if (ti.containedRobot != null && nextDist < minDist) {
                minDist = nextDist;
                closestLoc = ti.location;
            }
        }

        if (closestLoc != null) {
            randomSafeMove(location.directionTo(closestLoc));
            return;
        }

        randomSafeMove(randomDirection());
    }

    private void moveToNearest(MapLocation newGoal) throws GameActionException {
//        if (bugger.goal() == null || bugger.goal().equals(newGoal)) {
//            bugger.setGoal(location, newGoal, 0);
//        }
//
//        Direction next = bugger.nextStride(location, nearbyTrees);
//        if (next == null) {
//            next = randomDirection();
//        }
        Direction next = location.directionTo(newGoal);
        randomSafeMove(next);
    }

    private void tryChop(MapLocation nearestTree) throws GameActionException {
        if (hasAttacked) return;
        for (TreeInfo ti : nearbyTrees) {
            if (ti.location.distanceSquaredTo(nearestTree) < 1F) {
                if (rc.canChop(ti.getID())) {
                    chop(ti.getID());
                    return;
                }
            }
        }
    }

    protected void chopTreeWithRobot() throws GameActionException {
        if (hasAttacked) return;
        for (TreeInfo ti: nearbyTrees) {
            if (ti.containedRobot != null && rc.canChop(ti.getID())) {
                rc.chop(ti.getID());
                return;
            }
        }
    }

    private boolean shouldStrike() {
        if (hasAttacked) return false;

        int count = 0;
        for (RobotInfo e: nearbyEnemies) {
            if (location.distanceSquaredTo(e.location) <= sqrFloat(2F + e.type.bodyRadius)) count++;
        }
        for (RobotInfo a: nearbyAllies) {
            if (location.distanceSquaredTo(a.location) <= sqrFloat(2F + a.type.bodyRadius)) count--;
        }
        return count > 0;
    }

    private void strike() throws GameActionException {
        rc.strike();
        hasAttacked = true;
    }

    private void chop(int id) throws GameActionException {
        rc.chop(id);
        hasAttacked = true;
    }

    private void chop(MapLocation l) throws GameActionException {
        rc.chop(l);
        hasAttacked = true;
    }
}