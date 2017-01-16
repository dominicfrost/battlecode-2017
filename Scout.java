package battlecode2017;
import battlecode.common.*;

public class Scout extends Robot {
    Direction scoutingDirection;

    Scout(RobotController _rc) {
        super(_rc);
    }

    protected void initRobotState() throws GameActionException{
        scoutingDirection = randomDirection();
        super.initRobotState();
    }

    protected void doTurn() throws GameActionException {
        if (tryDodge()) return;

        if (!rc.onTheMap(location.add(scoutingDirection, myType.sensorRadius - 1))) {
            scoutingDirection = randomDirection();
        }
        tryMove(scoutingDirection);
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

    static Direction randomDirection() {
        return new Direction((float)Math.random() * 2 * (float)Math.PI);
    }
}