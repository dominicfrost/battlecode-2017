package battlecode2017;
import battlecode.common.*;

public class Lumberjack extends Robot {

    private Bug bugger;
    private RobotInfo myLumberJack;

    Lumberjack(RobotController _rc) {
        super(_rc);
    }

    @Override
    protected void initRobotState() throws GameActionException {
        super.initRobotState();
        bugger = new Bug(rc);
    }

    @Override
    protected void initRoundState() {
        super.initRoundState();
    }

    protected void doTurn() throws GameActionException {
        if (!tryDodge()) {
            randomSafeMove();
            if (!tryShakeOrCutTrees()){
                strike();
            }
        }
    }

    protected boolean tryShakeOrCutTrees() throws GameActionException {
        TreeInfo[] nearbyTrees = rc.senseNearbyTrees(2.0f);
        TreeInfo firstNonFriendlyTree = nearbyTrees[0];
        boolean canChop = false;

        if (nearbyTrees.length < 1) {
            return false;
        }

        for (int i = 0; i < nearbyTrees.length; i++) {
            TreeInfo t = nearbyTrees[i];
            if (rc.canShake(t.location) && (t.getContainedBullets() > 0 || t.getContainedRobot() != null)) {
                rc.shake(t.getLocation());
                return true;
            }
            if (t.getTeam() != rc.getTeam()){
                firstNonFriendlyTree = t;
                canChop = true;
            }
        }

        if (canChop){
            rc.chop(firstNonFriendlyTree.location);
            return true;
        }
        return false;
    }

    protected void strike() throws GameActionException{
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(2.0f);
        int friendlies = 0;
        int baddies = 0;

        for (int i = 0; i < nearbyRobots.length; i ++){
            if (nearbyRobots[i].getTeam() == rc.getTeam()){
                friendlies++;
            } else {
                baddies++;
            }
        }
        if (baddies > friendlies){
            rc.strike();
        }
    }
}