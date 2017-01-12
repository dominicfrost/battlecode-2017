package battlecode2017;
import battlecode.common.*;

public class Soldier extends Robot {
    Soldier(RobotController _rc) {
        super(_rc);
    }

    protected void doTurn() throws GameActionException {
        if (!tryDodge()) {
            // move to desired location
        }
    }
}