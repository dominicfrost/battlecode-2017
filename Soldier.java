package battlecode2017;
import battlecode.common.*;

public class Soldier extends Bugger {

    Soldier(RobotController _rc) {
        super(_rc);
    }

    protected void doTurn() throws GameActionException {
        tryDodge();
        destination = getDestination();

        if (destination != null) {
            moveWithBugger(destination, 5);
        } else {
            stayAwayFromAllies();
        }
        if (attackIfWayClose()) return;
        pieCountAttack();
    }
}