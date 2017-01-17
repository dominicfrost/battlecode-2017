package battlecode2017;
import battlecode.common.*;

public class Lumberjack extends Robot {

    private Bug bugger;
    private RobotInfo myLumberJack;
    private MapLocation targetTreeLoc;

    Lumberjack(RobotController _rc) {
        super(_rc);
    }

    @Override
    protected void initRobotState() throws GameActionException {
        super.initRobotState();
        bugger = new Bug(rc);
        targetTreeLoc = getTargetTree();
        bugger.setGoal(rc.getLocation(), targetTreeLoc, 2);
    }

    protected void doTurn() throws GameActionException {
        if (targetTreeLoc == null) {
            targetTreeLoc = getTargetTree();
            bugger.setGoal(rc.getLocation(), targetTreeLoc, 2);
        }

        if (!tryDodge()) {
            if (!shakeOrCutTrees()){
                strike();
            }
            lumberJackMove();
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
        if (nearbyTrees.length < 1){
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
        if (hasTarget() && rc.getLocation().distanceSquaredTo(targetTreeLoc) < 2.0f){
            Direction moveDir = bugger.nextStride(rc.getLocation(), rc.senseNearbyTrees());
            if (moveDir != null) {
                rc.move(moveDir);
            }
        }
    }

    protected boolean shakeOrCutTrees() throws GameActionException {
        if (hasTarget() && rc.getLocation().distanceSquaredTo(targetTreeLoc) < 2.0f){
            TreeInfo targetTree = rc.senseTreeAtLocation(targetTreeLoc);
            if (checkForGoodies(targetTree)){
                rc.shake(targetTreeLoc);
                return true;
            } else if (rc.canChop(targetTree.location)){
                if (targetTree.getHealth() < 5.0f){
                    targetTreeLoc = null;
                }
                rc.chop(targetTree.location);
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

    protected boolean hasTarget() throws GameActionException{
        return targetTreeLoc != null;
    }
}