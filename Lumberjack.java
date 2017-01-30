package battlecode2017;
import battlecode.common.*;

public class Lumberjack extends Circle {

    Lumberjack(RobotController _rc) {
        super(_rc);
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
            circleInOnTree(nearestTree);
        } else {
            circleInOnEnemy(nearestAttacker);
        }

        tryShakeTrees();
        if (shouldStrike()) strike();
        chopTreeWithRobot();
    }

    private void circleInOnTree(MapLocation nearestTree) throws GameActionException {
        TreeInfo treeInTheWay = rc.senseTreeAtLocation(location.add(location.directionTo(nearestTree), myType.strideRadius + myType.bodyRadius));
        if (treeInTheWay != null && !treeInTheWay.team.equals(myTeam)) {
            moveCirclingLocation(treeInTheWay.location, treeInTheWay.radius);
            attackCircleGoal(treeInTheWay.location, treeInTheWay.radius);
            return;
        }

        if (rc.canSenseLocation(nearestTree)) {
            TreeInfo taloc = rc.senseTreeAtLocation(nearestTree);
            if (taloc == null)
                taloc = getActualNearestOppTree(nearestTree);

            if (taloc != null) {
                rc.setIndicatorLine(location, taloc.location, 0,0,255);
                moveCirclingLocation(taloc.location, taloc.radius);
                attackCircleGoal(taloc.location, taloc.radius);
            }
        } else {
            rc.setIndicatorLine(location, nearestTree, 0,0,255);
            moveCirclingLocation(nearestTree, 0);
            attackCircleGoal(nearestTree, 0);
        }
    }

    private void circleInOnEnemy(MapLocation nearestAttacker) throws GameActionException {
        if (rc.canSenseLocation(nearestAttacker)) {
            RobotInfo raloc = rc.senseRobotAtLocation(nearestAttacker);
            if (raloc == null)
                raloc = getActualNearestEnemy(nearestAttacker);

            if (raloc != null) {
                rc.setIndicatorLine(location, raloc.location, 0, 0, 255);
                moveCirclingLocation(raloc.location, raloc.type.bodyRadius);
                if (atCircleGoal(raloc.location, raloc.type.bodyRadius) && !hasAttacked) {
                    strike();
                }
            }
        } else {
            rc.setIndicatorLine(location, nearestAttacker, 0,0,255);
            moveCirclingLocation(nearestAttacker, 0);
        }
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
            circleInOnTree(closestLoc);
            return;
        }

        for (TreeInfo ti: nearbyTrees) {
            if (ti.team.equals(myTeam)) continue;
            nextDist = location.distanceSquaredTo(ti.location);
            if (nextDist < minDist) {
                minDist = nextDist;
                closestLoc = ti.location;
            }
        }

        if (closestLoc != null) {
            circleInOnTree(closestLoc);
            return;
        }

        stayAwayFromAllies();
    }

    @Override
    protected void attackCircleGoal(MapLocation nearestTree, float goalRadius) throws GameActionException {
        if (hasAttacked) return;
        chopAnyOppTree();
    }


    protected void chopTreeWithRobot() throws GameActionException {
        if (hasAttacked) return;
        for (TreeInfo ti: nearbyTrees) {
            if (ti.containedRobot != null && rc.canChop(ti.getID())) {
                chop(ti.getID());
                return;
            }
        }
    }

    protected void chopAnyOppTree() throws GameActionException {
        if (hasAttacked) return;
        for (TreeInfo ti: nearbyTrees) {
            if (!ti.team.equals(myTeam) && rc.canChop(ti.getID())) {
                chop(ti.getID());
                return;
            }
        }
    }

    private boolean shouldStrike() {
        if (hasAttacked) return false;

        int count = 0;
        for (RobotInfo e: nearbyEnemies) {
            if (location.distanceSquaredTo(e.location) <= sqrFloat(2F)) count++;
        }
        for (RobotInfo a: nearbyAllies) {
            if (location.distanceSquaredTo(a.location) <= sqrFloat(2F)) count--;
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