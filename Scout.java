package battlecode2017;
import battlecode.common.*;

public class Scout extends Robot {
    Direction scoutingDirection;


    Scout(RobotController _rc) {
        super(_rc);
    }

    protected void initRobotState() throws GameActionException {
        scoutingDirection = randomDirection();
        super.initRobotState();
    }

    protected void doTurn() throws GameActionException {
        if (tryDodge()) return;

        if (!rc.onTheMap(location.add(scoutingDirection, myType.sensorRadius - 1))) {
            scoutingDirection = randomDirection();
        }

        tryMove(scoutingDirection);

        attackNearestEnemy();

        determineAreasOfInterest();
    }

    private void determineAreasOfInterest() throws GameActionException {
        if (nearbyEnemies.length > 3) {
            int currentRoundNum = rc.getRoundNum();
            // write to the first AOI channel that is stale ( > 100 rounds old)
            DecodedLocation loc = Coms.decodeLocation(rc.readBroadcast(Coms.AREA_OF_INTEREST_1));
            if (loc.roundNum == 0 || currentRoundNum - loc.roundNum > 100) {
                rc.broadcast(Coms.AREA_OF_INTEREST_1, Coms.encodeLocation(location, currentRoundNum));
                return;
            }

            loc = Coms.decodeLocation(rc.readBroadcast(Coms.AREA_OF_INTEREST_2));
            if (loc.roundNum == 0 || currentRoundNum - loc.roundNum > 100) {
                rc.broadcast(Coms.AREA_OF_INTEREST_2, Coms.encodeLocation(location, currentRoundNum));
                return;
            }

            loc = Coms.decodeLocation(rc.readBroadcast(Coms.AREA_OF_INTEREST_3));
            if (loc.roundNum == 0 || currentRoundNum - loc.roundNum > 100) {
                rc.broadcast(Coms.AREA_OF_INTEREST_3, Coms.encodeLocation(location, currentRoundNum));
            }
        }
    }
}