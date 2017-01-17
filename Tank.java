package battlecode2017;

import battlecode.common.*;

public class Tank extends Robot {
    Tank(RobotController _rc) {
        super(_rc);
    }

    protected void doTurn() throws GameActionException {
        if (!tryDodge()) {
            // move to desired location
        }
    }
}
