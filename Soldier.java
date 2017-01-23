package battlecode2017;
import battlecode.common.*;

public class Soldier extends Bugger {
    MapLocation destination;

    Soldier(RobotController _rc) {
        super(_rc);
    }


    protected void doTurn() throws GameActionException {
        tryDodge();
        if (destination == null) {
            destination = acquireDestination();
        }
        if (destination != null) {
            moveWithBugger(destination, 5);
        } else {
            stayAwayFromAllies();
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