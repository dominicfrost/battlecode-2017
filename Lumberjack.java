package battlecode2017;
import battlecode.common.*;

public class Lumberjack extends Robot {
    Lumberjack(RobotController _rc) {
        super(_rc);
    }

    protected void doTurn() throws GameActionException {
        if (!tryDodge()) {
            // move to desired location
        }
    }
}