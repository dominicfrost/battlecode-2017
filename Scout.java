package battlecode2017;
import battlecode.common.*;

public class Scout extends Robot {
    final int SCOUTING_NE = 1;
    final int SCOUTING_NW = 2;
    final int SCOUTING_SW = 3;
    final int SCOUTING_SE = 4;

    int state = SCOUTING_NE;

    Direction NE = new Direction(1, 1);
    Direction NW = new Direction(-1, 1);
    Direction SW = new Direction(-1, -1);
    Direction SE = new Direction(1, -1);

    Scout(RobotController _rc) {
        super(_rc);
    }

    protected void doTurn() throws GameActionException {
        if (tryDodge()) return;


        switch(state) {
            case SCOUTING_NE:
                if (!rc.onTheMap(location.add(NE, myType.sensorRadius - 1))) {
                    state = SCOUTING_NW;
                    tryMove(NW);
                    break;
                }
                tryMove(NE);
                break;
            case SCOUTING_NW:
                if (!rc.onTheMap(location.add(NW, myType.sensorRadius - 1))) {
                    state = SCOUTING_SW;
                    tryMove(SW);
                    break;
                }
                tryMove(NW);
                break;
            case SCOUTING_SW:
                if (!rc.onTheMap(location.add(SW, myType.sensorRadius - 1))) {
                    state = SCOUTING_SE;
                    tryMove(SE);
                    break;
                }
                tryMove(SW);
                break;
            case SCOUTING_SE:
                if (!rc.onTheMap(location.add(SE, myType.sensorRadius - 1))) {
                    state = SCOUTING_NE;
                    tryMove(NE);
                    break;
                }
                tryMove(SE);
                break;
        }
    }

    protected boolean tryMove(Direction dir) throws GameActionException {
        if (nearbyEnemies.length > 0) {
            // get nearest enemy
            RobotInfo closestEnemy = nearbyEnemies[0];
            float closestEnemyDistance = 9999999;
            for (int i = nearbyEnemies.length -1; i >= 0; i--) {
                float dist = location.distanceSquaredTo(nearbyEnemies[i].location);
                if (dist < closestEnemyDistance) {
                    closestEnemyDistance = dist;
                    closestEnemy = nearbyEnemies[i];
                }
            }

            if (rc.canFireSingleShot()) {
                rc.fireSingleShot(location.directionTo(closestEnemy.location));
            }
        }
        return super.tryMove(dir);
    }
}