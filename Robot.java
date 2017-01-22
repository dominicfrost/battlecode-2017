package battlecode2017;
import java.util.Arrays;
import java.util.Random;
import battlecode.common.*;

abstract public class Robot {
    // DEBUG CONSTANTS
    protected final float WAY_CLOSE_DISTANCE = 1F;
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

    protected boolean hasMoved;
    protected boolean hasAttacked;
    protected boolean hasShakenTree;

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
        hasMoved = false;
        hasAttacked = false;
        hasShakenTree = false;
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
        hasMoved = true;
    }

    protected void move(Direction dir, float dist) throws GameActionException {
        rc.move(dir, dist);
        location = rc.getLocation();
        hasMoved = true;
    }

    protected void move(MapLocation dest) throws GameActionException {
        rc.move(dest);
        location = rc.getLocation();
        hasMoved = true;
    }

    protected void shake(int id) throws GameActionException {
        rc.shake(id);
        hasShakenTree = true;
    }

    protected void shake(MapLocation l) throws GameActionException {
        rc.shake(l);
        hasShakenTree = true;
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
        for (int i = 0; i <= 180; i += RANDOM_MOVE_GRANULARITY) {
            next = startDir.rotateRightDegrees(i);
            if (rc.canMove(next)) {
                nextHealth = damageAtLocation(location.add(next));
                if (nextHealth < minHealth) {
                    toMove = next;
                    minHealth = nextHealth;
                }
                if (nextHealth == 0) break;
            }

            if (i == 180 || i == 0) continue;
            next = startDir.rotateLeftDegrees(i);
            if (rc.canMove(next)) {
                nextHealth = damageAtLocation(location.add(next));
                if (nextHealth < minHealth) {
                    toMove = next;
                    minHealth = nextHealth;
                }
                if (nextHealth == 0) break;
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

        // assume they will shoot us
        for (RobotInfo e : nearbyEnemies) {
            if (location.distanceSquaredTo(e.location) <= Math.pow(myType.bodyRadius + e.type.bodyRadius, 2) + .01F) {
                damage += e.type.attackPower;
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
        return anyoneTooClose() && willAnyBulletsCollideWithMe() && randomSafeMove(randomDirection());
    }

    private boolean anyoneTooClose() {
        if (myType.equals(RobotType.LUMBERJACK)) return false;
        for (RobotInfo e : nearbyEnemies) {
            if (e.type == RobotType.ARCHON || e.type == RobotType.GARDENER) continue;
            if (location.distanceSquaredTo(e.location) <= Math.pow(myType.bodyRadius + e.type.bodyRadius, 2) + .01F) {
                return true;
            }
        }
        return false;
    }

    protected boolean willAnyBulletsCollideWithMe() {
        return willAnyBulletsCollideWithLocation(location);
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

    protected boolean attackIfWayClose() throws GameActionException {
        if (hasAttacked) return false;
        for (RobotInfo b : nearbyEnemies) {
            if (location.distanceSquaredTo(b.location) <= Math.pow(b.type.bodyRadius + myType.bodyRadius + WAY_CLOSE_DISTANCE, 2)) {
                spray(location.directionTo(b.location));
                return true;
            }
        }
        return false;
    }

    protected boolean attackAndFleeIfWayClose() throws GameActionException {
        if (hasAttacked) return false;
        for (RobotInfo b : nearbyEnemies) {
            if (location.distanceSquaredTo(b.location) <= Math.pow(b.type.bodyRadius + myType.bodyRadius + WAY_CLOSE_DISTANCE, 2)) {
                spray(location.directionTo(b.location));
                tryMove(location.directionTo(b.location).opposite());
                return true;
            }
        }
        return false;
    }

    protected boolean pieCountAttack() throws GameActionException {
        if (hasAttacked) return false;
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

    protected void spray(Direction dir) throws GameActionException {
        if (rc.canFirePentadShot()) {
            rc.firePentadShot(dir);
            hasAttacked = true;
        } else if (rc.canFireTriadShot()) {
            rc.fireTriadShot(dir);
            hasAttacked = true;
        } else if (rc.canFireSingleShot()) {
            rc.fireSingleShot(dir);
            hasAttacked = true;
        }
    }

    protected boolean doCirclesOverlap(MapLocation locA, MapLocation locB, float radiusA, float radiusB) {
        double distance = Math.sqrt(locA.distanceSquaredTo(locB));
        return radiusA + radiusB >= distance;
    }

    protected boolean pointInCircle(MapLocation point, MapLocation center, float radius) {
        return radius > Math.sqrt(center.distanceSquaredTo(point));
    }

    protected boolean checkForGoodies(TreeInfo tree) throws GameActionException {
        if (rc.canShake(tree.location) && tree.containedBullets > 0 || tree.containedRobot != null) {
            return true;
        }
        return false;
    }

    protected void postPeskyTrees() throws GameActionException {
        if (nearbyTrees == null) return;

        int roundNum = rc.getRoundNum();
        int broadcastChannel = Coms.PESKY_TREES;


        for (TreeInfo ti : nearbyTrees) {
            if (ti.getTeam().equals(rc.getTeam())) continue;
            broadcastChannel = broadcastLocToNextOpenChan(broadcastChannel, roundNum, ti.location);
        }
    }

    protected void postPeskyAttackers() throws GameActionException {
        if (nearbyEnemies == null) return;

        int roundNum = rc.getRoundNum();
        int broadcastChannel = Coms.PESKY_ATTACKERS;


        for (RobotInfo ri : nearbyEnemies) {
            broadcastChannel = broadcastLocToNextOpenChan(broadcastChannel, roundNum, ri.location);
        }
    }

    private int broadcastLocToNextOpenChan(int broadcastChannel, int roundNum, MapLocation loc) throws GameActionException {
        int roundFilter = roundNum - 1;
        DecodedLocation locInChan;
        while (true) {
            // don't reuse a chan that has been posted to recently
            locInChan = Coms.decodeLocation(rc.readBroadcast(broadcastChannel));
            if (locInChan == null || locInChan.roundNum < roundFilter) {
                rc.broadcast(broadcastChannel, Coms.encodeLocation(loc, roundNum));
                rc.setIndicatorDot(loc, 0,0,0);

//                System.out.println("WRITE TO " + broadcastChannel + " " + loc.toString());
                return ++broadcastChannel;
            } else {
//                System.out.println("DIDNT WRITE TO " + broadcastChannel + " " + loc.toString());
            }

            broadcastChannel++;
        }
    }

    protected MapLocation nearestPeskyTree() throws GameActionException {
        return nearestLocationInLinearPost(Coms.PESKY_TREES);
    }

    protected MapLocation nearestPeskyAttacker() throws GameActionException {
        return nearestLocationInLinearPost(Coms.PESKY_ATTACKERS);
    }

    private MapLocation nearestLocationInLinearPost(int broadcastChannel) throws GameActionException {
        int roundNum = rc.getRoundNum();
        int roundFilter = roundNum - 1;

        DecodedLocation locInChan;
        float minDist = Float.MAX_VALUE;
        MapLocation closestLoc = null;
        float nextDist;
        while (true) {
            locInChan = Coms.decodeLocation(rc.readBroadcast(broadcastChannel));
//            System.out.println("READ FROM " + broadcastChannel + " " + (locInChan == null));
            if (locInChan == null || locInChan.roundNum < roundFilter) return closestLoc;
//            System.out.println("READ " + locInChan.location.toString());
            nextDist = location.distanceSquaredTo(locInChan.location);
            if (nextDist < minDist) {
                minDist = nextDist;
                closestLoc = locInChan.location;
            }
            broadcastChannel++;
        }
    }

    protected void tryShakeTrees() throws GameActionException {
        if (hasShakenTree) return;
        for (TreeInfo ti: nearbyTrees) {
            if (tryShakeTree(ti)) return;
        }
    }

    protected boolean tryShakeTree(TreeInfo ti) throws GameActionException {
        if (rc.canShake(ti.getID())) {
            rc.shake(ti.getID());
            hasShakenTree = true;
            return true;
        }
        return false;
    }

    protected float sqrFloat(float a) {
        return a * a;
    }
}
