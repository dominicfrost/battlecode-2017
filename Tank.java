package battlecode2017;

import battlecode.common.*;

public class Tank extends Robot {
    private MapLocation destination = null;

    Tank(RobotController _rc) {
        super(_rc);
    }

    protected void doTurn() throws GameActionException {
        tryDodge();
        destination = getDestination(destination);

        Direction moveDir;
        if (destination != null) {
            tryMove(location.directionTo(destination));
        } else {
            stayAwayFromAllies();
        }
        if (attackIfWayClose()) return;
        pieCountAttack();
    }
}
