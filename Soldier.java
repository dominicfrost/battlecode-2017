package battlecode2017;
import battlecode.common.*;

public class Soldier extends Robot {
    MapLocation destination;
    Bug destinationBugger;

    Soldier(RobotController _rc) {
        super(_rc);
    }


    protected void doTurn() throws GameActionException {
        if (!tryDodge()) {

            if (destination == null) {
                destination = acquireDestination();
                destinationBugger = new Bug(rc);
                destinationBugger.setGoal(location, destination, 5);
            }

            Direction moveDir;
            if (destination != null) {
                moveDir = destinationBugger.nextStride(location, nearbyTrees);
            } else {
                moveDir = randomDirection();
            }

            if (moveDir == null) {
                moveDir = location.directionTo(destination);
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
        if (dl.roundNum != 0 && dl.roundNum - currentRound < 100) {
            return dl.location;
        }

        dl = Coms.decodeLocation(rc.readBroadcast(Coms.AREA_OF_INTEREST_2));
        if (dl.roundNum != 0 && dl.roundNum - currentRound < 100) {
            return dl.location;
        }

        dl = Coms.decodeLocation(rc.readBroadcast(Coms.AREA_OF_INTEREST_3));
        if (dl.roundNum != 0 && dl.roundNum - currentRound < 100) {
            return dl.location;
        }

        return null;
    }
}