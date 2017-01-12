package battlecode2017;
import battlecode.common.*;

public strictfp class RobotPlayer {

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // Here, we've separated the controls into a different method for each RobotType.
        // You can add the missing ones or rewrite this into your own control structure.
        switch (rc.getType()) {
            case ARCHON:
                new Archon(rc).run();
                break;
            case GARDENER:
                new Gardener(rc).run();
                break;
            case SOLDIER:
                new Soldier(rc).run();
                break;
            case LUMBERJACK:
                new Lumberjack(rc).run();
                break;

            case SCOUT:
                new Scout(rc).run();
                break;
        }
    }
}
