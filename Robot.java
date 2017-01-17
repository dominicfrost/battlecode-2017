package battlecode2017;
import java.util.Arrays;
import java.util.Random;
import battlecode.common.*;

abstract public class Robot {
    // DEBUG CONSTANTS
    private final int BULLETS_TO_WIN = 10000;
    private final int ROBOT_ID = 12314;
    private final int MIN_ROUND = 1304;
    private final int MAX_ROUND = 1305;

    private final int CIRCLING_GRANULARITY = 8;
    private final float CIRCLING_DEGREE_INTERVAL = 360.0F / CIRCLING_GRANULARITY;
    private final int TRY_MOVE_DEGREE_OFFSET = 10;
    private final int RANDOM_MOVE_GRANULARITY = 72;

    protected RobotController rc;
    protected Random rand;
    protected Team myTeam;
    protected Team enemyTeam;
    protected RobotType myType;

    protected MapLocation location;
    protected BulletInfo[] nearbyBullets;
    protected BulletInfo[] nextRoundBullets;
    protected RobotInfo[] nearbyBots;
    protected RobotInfo[] nearbyEnemies;
    protected RobotInfo[] nearbyAllies;
    protected TreeInfo[] nearbyTrees;
    protected MapLocation home;
    protected float bulletCount;


    Robot(RobotController _rc) {
        rc = _rc;
    }

    abstract protected void doTurn() throws GameActionException;

    public void run() throws GameActionException {
        initRobotState();
        while (true) {
            try {
                initRoundState();
                doTurn();

                Clock.yield();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void initRobotState() throws GameActionException {
        rand = new Random();
        myTeam = rc.getTeam();
        enemyTeam = myTeam.opponent();
        myType = rc.getType();
    }

    protected void initRoundState() throws GameActionException {
        location = rc.getLocation();
        nearbyBullets = rc.senseNearbyBullets();
        nearbyBots = rc.senseNearbyRobots();
        nearbyAllies = filterNearbyBots(myTeam);
        nearbyEnemies = filterNearbyBots(enemyTeam);
        nearbyTrees = rc.senseNearbyTrees();
        bulletCount = rc.getTeamBullets();
        nextRoundBullets = advanceBullets(nearbyBullets);
        if (bulletCount >= BULLETS_TO_WIN) {
            rc.donate(BULLETS_TO_WIN);
        }
    }


    protected void debug(String msg) {
        if (debugCheck()) System.out.println(msg);
    }

    protected void debugIndicator(MapLocation l) throws GameActionException {
        if (debugCheck() && rc.onTheMap(l)) rc.setIndicatorDot(l, 0, 0, 0);
    }

    protected boolean debugCheck() {
        return rc.getID() == ROBOT_ID && rc.getRoundNum() <= MAX_ROUND && rc.getRoundNum() >= MIN_ROUND;
    }

    protected boolean tryMove(Direction dir) throws GameActionException {
        // First, try intended direction
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }

        int checksPerSide = (int) (240.0F / TRY_MOVE_DEGREE_OFFSET);

        // Now try a bunch of similar angles
        int currentCheck = 1;
        while (currentCheck <= checksPerSide) {
            // Try the offset of the left side
            if (rc.canMove(dir.rotateLeftDegrees(TRY_MOVE_DEGREE_OFFSET * currentCheck))) {
                rc.move(dir.rotateLeftDegrees(TRY_MOVE_DEGREE_OFFSET * currentCheck));
                return true;
            }
            // Try the offset on the right side
            if (rc.canMove(dir.rotateRightDegrees(TRY_MOVE_DEGREE_OFFSET * currentCheck))) {
                rc.move(dir.rotateRightDegrees(TRY_MOVE_DEGREE_OFFSET * currentCheck));
                return true;
            }
            // No move performed, try slightly further
            currentCheck++;
        }

        // A move never happened, so return false.
        return false;
    }


    protected boolean randomSafeMove() throws GameActionException {
        Direction toMove = null;
        Direction next;
        float nextHealth;
        float minHealth = Float.MAX_VALUE;

        Direction startDir = randomDirection();
        for (int i = 0; i < 360; i += RANDOM_MOVE_GRANULARITY) {
            next = startDir.rotateRightDegrees(i);
            if (rc.canMove(next)) {
                nextHealth = damageAtLocation(location.add(next));
                if (nextHealth < minHealth) {
                    toMove = next;
                    minHealth = nextHealth;
                }
            }
        }

        if (toMove != null && rc.canMove(toMove)) {
            rc.move(toMove);
            return true;
        }
        return false;
    }

    protected float damageAtLocation(MapLocation loc) {
        float damage = 0F;
        for (BulletInfo b : nearbyBullets) {
            if (willCollideLocation(b, loc)) {
                damage += b.damage;
            }
        }

        for (BulletInfo b : nextRoundBullets) {
            if (willCollideLocation(b, loc)) {
                damage += b.damage;
            }
        }

        return damage;
    }

    protected BulletInfo[] advanceBullets(BulletInfo[] bulletsToAdvance) {
        BulletInfo[] bullets = new BulletInfo[bulletsToAdvance.length];
        for (int i = 0; i < bulletsToAdvance.length; i++) {
            bullets[i] = advanceBullet(bulletsToAdvance[i]);
        }
        return bullets;
    }

    protected BulletInfo advanceBullet(BulletInfo b) {
        return new BulletInfo(
            b.ID,
            b.location.add(b.dir, b.speed),
            b.dir,
            b.speed,
            b.damage
        );
    }

    protected boolean tryDodge() throws GameActionException {
        return willAnyBulletsCollideWithMe() && randomSafeMove();
    }

    protected boolean willAnyBulletsCollideWithMe() {
        for (BulletInfo b : nearbyBullets) {
            if (willCollideWithMe(b)) return true;
        }
        return false;
    }

    protected boolean willAnyBulletsCollideWithLocation(MapLocation loc) {
        for (BulletInfo b : nearbyBullets) {
            if (willCollideLocation(b, loc)) return true;
        }
        for (BulletInfo b : nextRoundBullets) {
            if (willCollideLocation(b, loc)) return true;
        }
        return false;
    }

    protected boolean willCollideWithMe(BulletInfo bullet) {
        return willCollideLocation(bullet, location.add(bullet.dir, bullet.speed));
    }

    protected boolean willCollideLocation(BulletInfo bullet, MapLocation loc) {
        // Get relevant bullet information
        Direction propagationDirection = bullet.dir;
        MapLocation bulletLocation = bullet.location;

        // Calculate bullet relations to this robot
        Direction directionToRobot = bulletLocation.directionTo(loc);
        float distToRobot = bulletLocation.distanceTo(loc);
        float theta = propagationDirection.radiansBetween(directionToRobot);

        // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
        if (Math.abs(theta) > Math.PI / 2) {
            return false;
        }

        // distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
        // This is the distance of a line that goes from location and intersects perpendicularly with propagationDirection.
        // This corresponds to the smallest radius circle centered at our location that would intersect with the
        // line that is the path of the bullet.
        float perpendicularDist = (float) Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)

        return (perpendicularDist <= rc.getType().bodyRadius);
    }


    protected static Direction randomDirection() {
        return new Direction((float) Math.random() * 2 * (float) Math.PI);
    }

    private RobotInfo[] filterNearbyBots(Team team) {
        return Arrays.stream(nearbyBots).filter(b -> b.team.equals(team)).toArray(RobotInfo[]::new);
    }

    private MapLocation[] potentialDodgeLocations(double granularity) throws GameActionException {
        double strideRadius = myType.strideRadius;
        int steps = (int) (strideRadius / granularity);
        MapLocation[] locs = new MapLocation[steps * steps];

        for (int i = 0; i < steps; i++) {
            for (int j = 0; j < steps; j++) {
                MapLocation loc = new MapLocation(location.x + i, location.y + j);
                locs[(i * steps) + j] = rc.onTheMap(loc) && willAnyBulletsCollideWithLocation(loc) ? loc : null;
            }
        }

        // filter any null values out. TODO: check byte code used by this
        return Arrays.stream(locs).filter(l -> l != null).toArray(MapLocation[]::new);
    }

    // Used to find a location to stand if you want to build something at location center
    protected MapLocation findSpotAroundCircle(MapLocation center, float centerRadius, float revolverRadius) throws GameActionException {
        float distanceToCenter = centerRadius + revolverRadius;

        MapLocation nextLoc;
        Direction nextDir = randomDirection();
        for (int i = 0; i < CIRCLING_GRANULARITY; i++) {
            nextDir = nextDir.rotateLeftDegrees(CIRCLING_DEGREE_INTERVAL * i);
            nextLoc = center.add(nextDir, distanceToCenter);
            if (rc.canSenseAllOfCircle(nextLoc, revolverRadius) &&
                    rc.onTheMap(nextLoc) &&
                    !rc.isCircleOccupied(nextLoc, revolverRadius)) return nextLoc;
        }

        return null;
    }

    protected void attackNearestEnemy() throws GameActionException {
        if (nearbyEnemies.length > 0) {
            // get nearest enemy
            RobotInfo closestEnemy = nearbyEnemies[0];
            float closestEnemyDistance = Integer.MAX_VALUE;
            for (int i = nearbyEnemies.length -1; i >= 0; i--) {
                float dist = location.distanceSquaredTo(nearbyEnemies[i].location);
                if (dist < closestEnemyDistance) {
                    closestEnemyDistance = dist;
                    closestEnemy = nearbyEnemies[i];
                }
            }

            if (rc.canFirePentadShot()) {
                rc.firePentadShot(location.directionTo(closestEnemy.location));
            } else if (rc.canFireTriadShot()) {
                rc.fireTriadShot(location.directionTo(closestEnemy.location));
            } else if (rc.canFireSingleShot()) {
                rc.fireSingleShot(location.directionTo(closestEnemy.location));
            }
        }
    }

    protected boolean checkForGoodies(TreeInfo tree) throws GameActionException{
        if (rc.canShake(tree.location) && tree.containedBullets > 0 || tree.containedRobot != null){
            return true;
        }
        return false;
    }
}
