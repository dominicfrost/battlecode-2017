package battlecode2017;
import java.util.Arrays;
import java.util.Random;
import battlecode.common.*;

abstract public class Robot {
    // DEBUG CONSTANTS
    private final float WAY_CLOSE_DISTANCE = .01F;
    private final int BULLETS_TO_WIN = 10000;
    private final int ROBOT_ID = 10140;
    private final int MIN_ROUND = 0;
    private final int MAX_ROUND = 30;
    private final int ATTACK_CONSIDERATION_DISTANCE = 3;

    private final int CIRCLING_GRANULARITY = 8;
    private final float CIRCLING_DEGREE_INTERVAL = 360.0F / CIRCLING_GRANULARITY;
    private final int TRY_MOVE_DEGREE_OFFSET = 10;
    private final int RANDOM_MOVE_GRANULARITY = 60;

    protected RobotController rc;
    protected Random rand;
    protected Team myTeam;
    protected Team enemyTeam;
    protected RobotType myType;
    protected MapLocation[] enemyArchonLocs;

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
        enemyArchonLocs = rc.getInitialArchonLocations(enemyTeam);
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

    protected void move(Direction dir) throws GameActionException {
        rc.move(dir);
        location = rc.getLocation();
    }

    protected void move(Direction dir, float dist) throws GameActionException {
        rc.move(dir, dist);
        location = rc.getLocation();
    }

    protected void move(MapLocation dest) throws GameActionException {
        rc.move(dest);
        location = rc.getLocation();
    }

    protected boolean tryMove(Direction dir) throws GameActionException {
        // First, try intended direction
        if (rc.canMove(dir)) {
            move(dir);
            return true;
        }

        int checksPerSide = (int) (240.0F / TRY_MOVE_DEGREE_OFFSET);

        // Now try a bunch of similar angles
        int currentCheck = 1;
        while (currentCheck <= checksPerSide) {
            // Try the offset of the left side
            if (rc.canMove(dir.rotateLeftDegrees(TRY_MOVE_DEGREE_OFFSET * currentCheck))) {
                move(dir.rotateLeftDegrees(TRY_MOVE_DEGREE_OFFSET * currentCheck));
                return true;
            }
            // Try the offset on the right side
            if (rc.canMove(dir.rotateRightDegrees(TRY_MOVE_DEGREE_OFFSET * currentCheck))) {
                move(dir.rotateRightDegrees(TRY_MOVE_DEGREE_OFFSET * currentCheck));
                return true;
            }
            // No move performed, try slightly further
            currentCheck++;
        }

        // A move never happened, so return false.
        return false;
    }


    protected boolean randomSafeMove(Direction startDir) throws GameActionException {
        Direction toMove = null;
        Direction next;
        float nextHealth;
        float minHealth = Float.MAX_VALUE;

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
            move(toMove);
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
        return willAnyBulletsCollideWithMe() && randomSafeMove(randomDirection());
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

    protected boolean attackIfWayClose() throws GameActionException {
        for (RobotInfo b : nearbyEnemies) {
            if (location.distanceSquaredTo(b.location) >= b.type.bodyRadius + myType.bodyRadius + WAY_CLOSE_DISTANCE) {
                spray(location.directionTo(b.location));
                return true;
            }
        }
        return false;
    }

    protected boolean pieCountAttack() throws GameActionException {
        int numPieces = 12;
        int pieceDegrees = 360 / numPieces;

        int[] counts = new int[numPieces];

        Direction next;
        float degrees;
        int countIndex;
        for (RobotInfo ri : nearbyEnemies) {
            next = location.directionTo(ri.location);
            degrees = (next.getAngleDegrees() + 360) % 360;
            countIndex = (int) degrees / pieceDegrees;
            counts[countIndex]++;
        }
        for (RobotInfo ri : nearbyAllies) {
            next = location.directionTo(ri.location);
            degrees = (next.getAngleDegrees() + 360) % 360;
            countIndex = (int) degrees / pieceDegrees;
            counts[countIndex]--;
        }
//        for (TreeInfo ri : nearbyTrees) {
//            if (!ri.team.equals(enemyTeam)) continue;
//            next = location.directionTo(ri.location);
//            degrees = (next.getAngleDegrees() + 360) % 360;
//            countIndex = (int) degrees / pieceDegrees;
//            counts[countIndex]++;
//        }

        int maxCount = 0;
        int index = -1;
        for (int i = numPieces - 1; i >= 0; i--) {
            if (counts[i] > maxCount) {
                index = i;
                maxCount = counts[i];
            }
        }

        if (index == -1) return false;
        int toShootDeg = ( pieceDegrees * (index + 1) ) - (pieceDegrees / 2);
        Direction toShoot = new Direction((float) Math.toRadians(toShootDeg));
        spray(toShoot);
        return true;
    }

    protected void accurateAttack() throws GameActionException {
        if (nearbyEnemies.length > 0) {
            System.out.println("STart " + Clock.getBytecodesLeft());
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


            int singleDmg = 0;
            int triadDmg = 0;
            int pentadDmg = 0;
            Direction dir = location.directionTo(closestEnemy.location);
            float maxDist = ATTACK_CONSIDERATION_DISTANCE * myType.bulletSpeed;
            float maxDistSqr = maxDist * maxDist;
            for (int i = nearbyEnemies.length -1; i >= 0; i--) {
                if (Clock.getBytecodesLeft() < 2000) break;
                if (nearbyEnemies[i].location.equals(closestEnemy.location)) continue;
                if (location.distanceSquaredTo(nearbyEnemies[i].location) < maxDistSqr) continue;
                if (singleWillHit(dir, nearbyEnemies[i])) {
                    singleDmg++;
                    continue;
                }
                if (triadWillHit(dir, nearbyEnemies[i])){
                    triadDmg++;
                    continue;
                }

                if (pentadWillHit(dir, nearbyEnemies[i])) pentadDmg++;
            }

            for (int i = nearbyAllies.length -1; i >= 0; i--) {
                if (Clock.getBytecodesLeft() < 2000) break; // We may shoot our guys at the price of goin over
                if (location.distanceSquaredTo(nearbyAllies[i].location) > maxDistSqr) continue;
                if (singleWillHit(dir, nearbyAllies[i])) {
                    singleDmg--;
                    continue;
                }
                if (triadWillHit(dir, nearbyAllies[i])){
                    triadDmg--;
                    continue;
                }

                if (pentadWillHit(dir, nearbyAllies[i])) pentadDmg--;
            }

            System.out.println("End " + Clock.getBytecodesLeft());
            if (pentadDmg > singleDmg && pentadDmg > triadDmg && pentadDmg > 0 && rc.canFirePentadShot()) {
                rc.firePentadShot(dir);
            } else if (triadDmg > singleDmg && triadDmg > 0 && rc.canFireTriadShot()) {
                rc.fireTriadShot(dir);
            } else if (singleDmg > 0 && rc.canFireSingleShot()) {
                rc.fireSingleShot(dir);
            }
        }
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

    private void spray(Direction dir) throws GameActionException {
        if (rc.canFirePentadShot()) {
            rc.firePentadShot(dir);
        } else if (rc.canFireTriadShot()) {
            rc.fireTriadShot(dir);
        } else if (rc.canFireSingleShot()) {
            rc.fireSingleShot(dir);
        }
    }

    private boolean singleWillHit(Direction shotDir, RobotInfo bot) {
        MapLocation nextCenter;
        for (int i = ATTACK_CONSIDERATION_DISTANCE; i > 0; i--) {
            nextCenter = location.add(shotDir, i * myType.bulletSpeed);
            if (pointInCircle(nextCenter, bot.location, bot.type.bodyRadius)) return true;
        }
        return false;
    }

    private boolean triadWillHit(Direction shotDir, RobotInfo bot) {
        MapLocation nextCenter;
        Direction nextDir;
        for (int i = ATTACK_CONSIDERATION_DISTANCE; i > 0; i--) {
            nextDir = shotDir.rotateRightDegrees(20);
            nextCenter = location.add(nextDir, i * myType.bulletSpeed);
            if (pointInCircle(nextCenter, bot.location, bot.type.bodyRadius)) return true;

            nextDir = shotDir.rotateLeftDegrees(20);
            nextCenter = location.add(nextDir, i * myType.bulletSpeed);
            if (pointInCircle(nextCenter, bot.location, bot.type.bodyRadius)) return true;
        }
        return false;
    }

    private boolean pentadWillHit(Direction shotDir, RobotInfo bot) {
        MapLocation nextCenter;
        Direction nextDir;
        for (int i = ATTACK_CONSIDERATION_DISTANCE; i > 0; i--) {
            nextDir = shotDir.rotateRightDegrees(15);
            nextCenter = location.add(nextDir, i * myType.bulletSpeed);
            if (pointInCircle(nextCenter, bot.location, bot.type.bodyRadius)) return true;

            nextDir = shotDir.rotateLeftDegrees(15);
            nextCenter = location.add(nextDir, i * myType.bulletSpeed);
            if (pointInCircle(nextCenter, bot.location, bot.type.bodyRadius)) return true;

            nextDir = shotDir.rotateRightDegrees(30);
            nextCenter = location.add(nextDir, i * myType.bulletSpeed);
            if (pointInCircle(nextCenter, bot.location, bot.type.bodyRadius)) return true;

            nextDir = shotDir.rotateLeftDegrees(30);
            nextCenter = location.add(nextDir, i * myType.bulletSpeed);
            if (pointInCircle(nextCenter, bot.location, bot.type.bodyRadius)) return true;
        }
        return false;
    }

    private boolean willHit(Direction shotDir, RobotInfo bot, int numBullets, int separationDeg) {
        int loopLimit = (numBullets - 1) / 2;
        Direction nextDir;
        MapLocation nextCenter;
        for (int i = ATTACK_CONSIDERATION_DISTANCE; i > 0; i--) {
            for (int j = loopLimit; j >= 0; j--) {
                if (j == 0 ) {
                    nextCenter = location.add(shotDir, i * myType.bulletSpeed);
                    if (pointInCircle(nextCenter, bot.location, bot.type.bodyRadius)) return true;
                } else {
                    nextDir = shotDir.rotateRightDegrees(separationDeg * j);
                    nextCenter = location.add(nextDir, i * myType.bulletSpeed);
                    if (pointInCircle(nextCenter, bot.location, bot.type.bodyRadius)) return true;

                    nextDir = shotDir.rotateLeftDegrees(separationDeg * j);
                    nextCenter = location.add(nextDir, i * myType.bulletSpeed);
                    if (pointInCircle(nextCenter, bot.location, bot.type.bodyRadius)) return true;
                }
            }
        }
        return false;
    }

    protected boolean doCirclesOverlap(MapLocation locA, MapLocation locB, float radiusA, float radiusB) {
        double distance = Math.sqrt(locA.distanceSquaredTo(locB));
        return radiusA + radiusB >= distance;
    }

    protected boolean pointInCircle(MapLocation point, MapLocation center, float radius) {
        return radius > Math.sqrt(center.distanceSquaredTo(point));
    }

    protected boolean checkForGoodies(TreeInfo tree) throws GameActionException{
        if (rc.canShake(tree.location) && tree.containedBullets > 0 || tree.containedRobot != null){
            return true;
        }
        return false;
    }
}
