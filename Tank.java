package battlecode2017;

import battlecode.common.*;

public class Tank extends Robot {
    private MapLocation destination = null;

    Tank(RobotController _rc) {
        super(_rc);
    }

    protected void doTurn() throws GameActionException {
        if (!tryDodge()) {

            if (destination == null) {
                destination = acquireDestination();
            }

            Direction moveDir;
            if (destination != null) {
                moveDir = location.directionTo(destination);
            } else {
                moveDir = randomDirection();
            }

            if (attackAndFleeIfWayClose()) return;
            tryMove(moveDir);
        }
        if (attackIfWayClose()) return;
        pieCountAttack();
    }

    private MapLocation acquireDestination() throws GameActionException {
        int currentRound = rc.getRoundNum();

        // look for an AOI
        DecodedLocation dl = Coms.decodeLocation(rc.readBroadcast(Coms.AREA_OF_INTEREST_1));
        if (dl != null && dl.roundNum - currentRound < 100) {
            return dl.location;
        }

        dl = Coms.decodeLocation(rc.readBroadcast(Coms.AREA_OF_INTEREST_2));
        if (dl != null && dl.roundNum - currentRound < 100) {
            return dl.location;
        }

        dl = Coms.decodeLocation(rc.readBroadcast(Coms.AREA_OF_INTEREST_3));
        if (dl != null && dl.roundNum - currentRound < 100) {
            return dl.location;
        }

        return null;
    }
}
