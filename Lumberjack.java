package battlecode2017;
import battlecode.common.*;

public class Lumberjack extends Robot {

    private Bug bugger;
    private RobotInfo myLumberJack;
    private MapLocation targetTreeLocation;

    Lumberjack(RobotController _rc) {
        super(_rc);
    }

    @Override
    protected void initRobotState() throws GameActionException {
        super.initRobotState();
        bugger = new Bug(rc);
        targetTreeLocation = null;

    }

    @Override
    protected void initRoundState() throws GameActionException {
        super.initRoundState();
        if (targetTreeLocation == null) {
            targetTreeLocation = getTargetTree();
            bugger.setGoal(rc.getLocation(), targetTreeLocation, 4);
        }
    }

    protected void doTurn() throws GameActionException {
        if (!tryDodge()) {
            lumberJackMove();
            if (!shakeOrCutTrees()){
                strike();
            }
        }
    }

    protected MapLocation getTargetTree() throws GameActionException {
        MapLocation targetTreeLoc = getNearestPeskyTree();
        if (targetTreeLoc == null){
            targetTreeLoc = getNearestTree();
        }
        return targetTreeLoc;
    }

    protected MapLocation getNearestPeskyTree() throws GameActionException {
        MapLocation closestPeskyTree = null;
        int broadcast = Coms.PESKY_TREES;
        float minDist = 99999999.9f;

        int xLoc = rc.readBroadcast(broadcast);
        int yLoc = rc.readBroadcast(broadcast + 1);

        while(xLoc != 0 && yLoc != 0){
            MapLocation newLoc = new MapLocation(xLoc, yLoc);

            float dist = newLoc.distanceSquaredTo(rc.getLocation());
            if (dist < minDist){
                minDist = dist;
                closestPeskyTree = newLoc;
            }
            broadcast += 2;
            xLoc = rc.readBroadcast(broadcast);
            yLoc = rc.readBroadcast(broadcast + 1);
        }
        return closestPeskyTree;
    }

    protected MapLocation getNearestTree() throws GameActionException{
        if (nearbyTrees == null){
            return null;
        }

        MapLocation nearestTreeLoc = nearbyTrees[0].getLocation();
        float minDist = nearbyTrees[0].getLocation().distanceSquaredTo(location);
        for (int i = 1; i < nearbyTrees.length; i++){
            float dist = nearbyTrees[i].getLocation().distanceSquaredTo(location);
            if (dist < minDist && nearbyTrees[i].getTeam() != rc.getTeam()){
                minDist = dist;
                nearestTreeLoc = nearbyTrees[i].getLocation();
            }
        }
        return nearestTreeLoc;
    }

    protected void lumberJackMove() throws GameActionException  {
        if (!rc.hasMoved()) {
            if (targetTreeLocation == null) {
                randomSafeMove();
            } else {
                Direction moveDir = bugger.nextStride(rc.getLocation(), rc.senseNearbyTrees());
                if (moveDir != null) {
                    rc.move(moveDir);
                }
            }
        }
    }

    protected boolean shakeOrCutTrees() throws GameActionException {
        // if there's trees nearby
        if (nearbyTrees != null && nearbyTrees.length > 0){
            TreeInfo nearestTree = nearbyTrees[0];
            float minDist = nearestTree.getLocation().distanceSquaredTo(location);

            for (int i = 0; i < nearbyTrees.length; i++){
                // shake a tree
                TreeInfo t = nearbyTrees[i];
                if (checkForGoodies(t)){
                    rc.shake(t.location);
                }

                // chop target
                float distToTarget = location.distanceSquaredTo(targetTreeLocation);
                if (distToTarget < 4.0){
                    TreeInfo targetTree = rc.senseTreeAtLocation(targetTreeLocation);
                    if (targetTree != null && targetTree.getHealth() < 5.0f){
                        targetTreeLocation = null;
                        rc.chop(targetTree.getLocation());
                        return true;
                    }
                }

                // determine closest tree
                float dist = t.getLocation().distanceSquaredTo(location);
                if (dist < minDist){
                    nearestTree = t;
                    minDist = dist;
                }
            }

            // chop random tree
            if (rc.canChop(nearestTree.getLocation())){
                rc.chop(nearestTree.getLocation());
                return true;
            }
        }
        return false;
    }

    protected void strike() throws GameActionException{
        if (nearbyEnemies.length > nearbyAllies.length){
            rc.strike();
        }
    }
}