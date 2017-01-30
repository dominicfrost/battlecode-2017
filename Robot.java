package battlecode2017;
import java.util.Arrays;
import java.util.Random;
import battlecode.common.*;

abstract public class Robot {
    // DEBUG CONSTANTS
    protected final float WAY_CLOSE_DISTANCE = .1F;
    private final int ROBOT_ID = 13527;
    private final int MIN_ROUND = 60;
    private final int MAX_ROUND = 62;
    private final float MAX_BULLET_SPEED = 4F;

    protected final float radian = 0.0174533F;
    protected final float pi = (float) Math.PI;

    private final int TRY_MOVE_DEGREE_OFFSET = 10;
    private final int RANDOM_MOVE_GRANULARITY = 45;
    protected RobotController rc;
    protected Random rand;
    protected Team myTeam;
    protected Team enemyTeam;
    protected RobotType myType;
    protected MapLocation[] enemyArchonLocs;
    protected MapLocation[] allyArchonLocs;

    protected boolean hasMoved;
    protected boolean hasAttacked;
    protected boolean hasShakenTree;

    protected MapLocation location;
    protected MapLocation destination;
    protected BulletInfo[] nearbyBullets;
    protected BulletInfo[] nextRoundBullets;
    protected RobotInfo[] nearbyBots;
    protected RobotInfo[] nearbyEnemies;
    protected RobotInfo[] nearbyAllies;
    protected TreeInfo[] nearbyTrees;
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
                debug("doTurn " );
                doTurn();
                debug("doTurn/ " );
            } catch (Exception e) {
                e.printStackTrace();
            }
            Clock.yield();
        }
    }

    protected void initRobotState() throws GameActionException {
        rand = new Random();
        myTeam = rc.getTeam();
        enemyTeam = myTeam.opponent();
        myType = rc.getType();
        enemyArchonLocs = rc.getInitialArchonLocations(enemyTeam);
        allyArchonLocs = rc.getInitialArchonLocations(myTeam);
    }

    protected void initRoundState() throws GameActionException {
        hasMoved = false;
        hasAttacked = false; // also used for chops and strikes
        hasShakenTree = false;
        location = rc.getLocation();
        nearbyBullets = rc.senseNearbyBullets(MAX_BULLET_SPEED + myType.strideRadius + myType.bodyRadius);
        nearbyBots = rc.senseNearbyRobots();
        nearbyAllies = filterNearbyBots(myTeam);
        nearbyEnemies = filterNearbyBots(enemyTeam);
        nearbyTrees = rc.senseNearbyTrees();
        bulletCount = rc.getTeamBullets();
        nextRoundBullets = advanceBullets(nearbyBullets);
        if (bulletCount >= rc.getVictoryPointCost() * GameConstants.VICTORY_POINTS_TO_WIN) {
            rc.donate(bulletCount);
        }

        if (rc.getRoundNum() == rc.getRoundLimit()) {
            rc.donate(rc.getTeamBullets());
        }

        updateTypeCount();
    }

    private void updateTypeCount() throws GameActionException {
        switch (myType) {
            case GARDENER:
                postCount(Coms.GARNDENER_COUNT);
                break;
            case LUMBERJACK:
                postCount(Coms.LUMBERJACK_COUNT);
                break;
            case SCOUT:
                postCount(Coms.SCOUT_COUNT);
                break;
            case SOLDIER:
                postCount(Coms.SOLDIER_COUNT);
                break;
            case TANK:
                postCount(Coms.TANK_COUNT);
        }
    }

    private void postCount(int chan) throws GameActionException {
        int count = rc.readBroadcast(chan) + 1;
        rc.broadcast(chan, count);
    }


    protected void debug(String msg) {
        if (debugCheck()) System.out.println("BytecodesLeft: " + Clock.getBytecodesLeft() + " Round: " + rc.getRoundNum() + " Msg: " + msg);
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
        if (hasMoved) return false;

        // First, try intended direction
        if (rc.canMove(dir)) {
            move(dir);
            return true;
        }

        int checksPerSide = (int) (180F / TRY_MOVE_DEGREE_OFFSET);

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


    protected boolean safeMove(Direction startDir) throws GameActionException {
        if (hasMoved) return false;

        Direction toMove = null;
        Direction next;
        float nextHealth;
        float minHealth = Float.MAX_VALUE;
        for (int i = 0; i <= 180; i += RANDOM_MOVE_GRANULARITY) {
            next = startDir.rotateRightDegrees(i * RANDOM_MOVE_GRANULARITY);
            if (rc.canMove(next)) {
                nextHealth = damageAtLocation(location.add(next, myType.strideRadius));
                if (nextHealth < minHealth) {
                    toMove = next;
                    minHealth = nextHealth;
                    if (nextHealth == 0) break;
                }
            }

            if (i == 180 || i == 0) continue;
            next = startDir.rotateLeftDegrees(i * RANDOM_MOVE_GRANULARITY);
            if (rc.canMove(next)) {
                nextHealth = damageAtLocation(location.add(next, myType.strideRadius));
                if (nextHealth < minHealth) {
                    toMove = next;
                    minHealth = nextHealth;
                    if (nextHealth == 0) break;
                }
            }
        }

        if (toMove != null && rc.canMove(toMove)) {
            move(toMove, myType.strideRadius);
            return true;
        }
        return false;
    }

    protected float damageAtLocation(MapLocation loc) {
        float damage = 0F;
        for (BulletInfo b : nearbyBullets) {
            if (pointInCircle(b.location, loc, myType.bodyRadius)) {
                damage += b.damage;
            }
        }

        for (BulletInfo b : nextRoundBullets) {
            if (pointInCircle(b.location, loc, myType.bodyRadius)) {
                damage += b.damage;
            }
        }
//        for (BulletInfo b : nearbyBullets) {
//            if (willCollideLocation(b, loc)) {
//                damage += b.damage;
//            }
//        }

        // assume they will shoot us / strike us
        for (RobotInfo e : nearbyEnemies) {
            if (e.type.equals(RobotType.LUMBERJACK)) {
                if (loc.distanceTo(e.location) <= myType.bodyRadius + 2F + e.type.strideRadius) {
                    damage += e.type.attackPower;
                }
            } else if (loc.distanceTo(e.location) <= myType.bodyRadius + e.type.bodyRadius + e.type.strideRadius) {
                damage += e.type.attackPower;
            }
        }
        debug("Dagmage at location " + loc.toString() + " " + damage);
        return damage;
    }

    protected boolean willCollideLocation(BulletInfo bullet, MapLocation loc) {
        // Get relevant bullet information
        Direction propagationDirection = bullet.dir;
        MapLocation bulletLocation = bullet.location;

        // Calculate bullet relations to this robot
        Direction directionToRobot = bulletLocation.directionTo(loc);
        float distToRobot = bulletLocation.distanceTo(loc);
        if (distToRobot > bullet.speed) {
            debug("WUt " + distToRobot + " " + bullet.speed);
            return false;
        }
        debug("z");
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
//        float adjacentDist = (float) Math.abs(distToRobot * Math.cos(theta));
        return (perpendicularDist <= myType.bodyRadius);
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
        return damageAtLocation(location) > 0 && safeMove(randomDirection());
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
            if (location.distanceTo(b.location) <= b.type.bodyRadius + myType.bodyRadius + WAY_CLOSE_DISTANCE) {
                spray(location.directionTo(b.location));
                return true;
            }
        }
        return false;
    }

//    protected boolean attackAndFleeIfWayClose() throws GameActionException {
//        if (hasAttacked) return false;
//        for (RobotInfo b : nearbyEnemies) {
//            if (location.distanceSquaredTo(b.location) <= Math.pow(b.type.bodyRadius + myType.bodyRadius + WAY_CLOSE_DISTANCE, 2)) {
//                spray(location.directionTo(b.location));
//                tryMove(location.directionTo(b.location).opposite());
//                return true;
//            }
//        }
//        return false;
//    }

    protected boolean pieCountAttack() throws GameActionException {
        if (hasAttacked) return false;
        int numPieces = 6;
        int pieceDegrees = 360 / numPieces;

        int[] counts = new int[numPieces];

        RobotInfo[] closestBots = new RobotInfo[numPieces];

        Direction next;
        float degrees;
        int countIndex;
        for (RobotInfo ri : nearbyEnemies) {
            next = location.directionTo(ri.location);
            degrees = (next.getAngleDegrees() + 360) % 360;
            countIndex = (int) degrees / pieceDegrees;
            counts[countIndex]++;
            if (closestBots[countIndex] == null || closestBots[countIndex].location.distanceTo(location) > ri.location.distanceTo(location)) {
                closestBots[countIndex] = ri;
            }
        }
        for (RobotInfo ri : nearbyAllies) {
            int multiplier = 2;
            if (ri.getType() == RobotType.TANK){
                multiplier = 4;
            }
            next = location.directionTo(ri.location);
            degrees = (next.getAngleDegrees() + 360) % 360;
            countIndex = (int) degrees / pieceDegrees;
            counts[countIndex]-= multiplier;
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
//        int toShootDeg = ( pieceDegrees * (index + 1) ) - (pieceDegrees / 2);
//        Direction toShoot = new Direction((float) Math.toRadians(toShootDeg));
        spray(location.directionTo(closestBots[index].location));
        return true;
    }

    protected void spray(Direction dir) throws GameActionException {
        if (rc.canFirePentadShot()) {
            rc.firePentadShot(dir);
            hasAttacked = true;
            addSingleBulletToNearby(dir);
        } else if (rc.canFireTriadShot()) {
            rc.fireTriadShot(dir);
            hasAttacked = true;
            addTriadBulletToNearby(dir);
        } else if (rc.canFireSingleShot()) {
            rc.fireSingleShot(dir);
            hasAttacked = true;
            addPentadBulletToNearby(dir);
        }
    }

    private void addSingleBulletToNearby(Direction dir) {
        nearbyBullets = Arrays.copyOf(nearbyBullets, nearbyBullets.length + 1);
        nearbyBullets[nearbyBullets.length - 1] = newBullet(dir);

        nextRoundBullets = Arrays.copyOf(nextRoundBullets, nextRoundBullets.length + 1);
        nextRoundBullets[nextRoundBullets.length - 1] = newBullet(dir);
    }

    private void addTriadBulletToNearby(Direction dir) {
        nearbyBullets = Arrays.copyOf(nearbyBullets, nearbyBullets.length + 3);
        nearbyBullets[nearbyBullets.length - 1] = newBullet(dir);
        nearbyBullets[nearbyBullets.length - 2] = newBullet(dir.rotateRightDegrees(20));
        nearbyBullets[nearbyBullets.length - 3] = newBullet(dir.rotateLeftDegrees(20));

        nextRoundBullets = Arrays.copyOf(nextRoundBullets, nextRoundBullets.length + 3);
        nextRoundBullets[nextRoundBullets.length - 1] = newBullet(dir);
        nextRoundBullets[nextRoundBullets.length - 2] = newBullet(dir.rotateRightDegrees(20));
        nextRoundBullets[nextRoundBullets.length - 3] = newBullet(dir.rotateLeftDegrees(20));
    }

    private void addPentadBulletToNearby(Direction dir) {
        nearbyBullets = Arrays.copyOf(nearbyBullets, nearbyBullets.length + 5);
        nearbyBullets[nearbyBullets.length - 1] = newBullet(dir);
        nearbyBullets[nearbyBullets.length - 2] = newBullet(dir.rotateRightDegrees(15));
        nearbyBullets[nearbyBullets.length - 3] = newBullet(dir.rotateLeftDegrees(15));
        nearbyBullets[nearbyBullets.length - 4] = newBullet(dir.rotateRightDegrees(30));
        nearbyBullets[nearbyBullets.length - 5] = newBullet(dir.rotateLeftDegrees(30));

        nextRoundBullets = Arrays.copyOf(nextRoundBullets, nextRoundBullets.length + 5);
        nextRoundBullets[nextRoundBullets.length - 1] = newBullet(dir);
        nextRoundBullets[nextRoundBullets.length - 2] = newBullet(dir.rotateRightDegrees(15));
        nextRoundBullets[nextRoundBullets.length - 3] = newBullet(dir.rotateLeftDegrees(15));
        nextRoundBullets[nextRoundBullets.length - 4] = newBullet(dir.rotateRightDegrees(30));
        nextRoundBullets[nextRoundBullets.length - 5] = newBullet(dir.rotateLeftDegrees(30));
    }

    private BulletInfo newBullet(Direction dir) {
        return new BulletInfo(-1, location.add(dir, myType.bodyRadius), dir, myType.bulletSpeed, myType.attackPower);
    }

    protected boolean doCirclesOverlap(MapLocation locA, MapLocation locB, float radiusA, float radiusB) {
        double distance = Math.sqrt(locA.distanceSquaredTo(locB));
        return radiusA + radiusB >= distance;
    }

    protected boolean pointInCircle(MapLocation point, MapLocation center, float radius) {
        return radius > center.distanceTo(point);
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

        float minDist = Float.MAX_VALUE;
        MapLocation closest = null;
        float nextDist;

        for (TreeInfo ti : nearbyTrees) {
            if (ti.getTeam().equals(rc.getTeam())) continue;

            nextDist = location.distanceSquaredTo(ti.location);
            if (nextDist < minDist) {
                minDist = nextDist;
                closest = ti.location;
            }
//            if (nextDist < 12) {
//                broadcastChannel = broadcastLocToNextOpenChan(broadcastChannel, roundNum, ti.location);
//            }
        }

        if (closest != null) broadcastLocToNextOpenChan(broadcastChannel, roundNum, closest);
    }

    protected void postPeskyAttackers() throws GameActionException {
        if (nearbyEnemies == null) return;

        int roundNum = rc.getRoundNum();
        int broadcastChannel = Coms.PESKY_ATTACKERS;

        float minDist = Float.MAX_VALUE;
        MapLocation closest = null;
        float nextDist;

        for (RobotInfo ri : nearbyEnemies) {
            nextDist = location.distanceSquaredTo(ri.location);
            if (nextDist < minDist) {
                minDist = nextDist;
                closest = ri.location;
            }
        }

        if (closest != null) broadcastLocToNextOpenChan(broadcastChannel, roundNum, closest);
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
                return ++broadcastChannel;
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
            if (locInChan == null || locInChan.roundNum < roundFilter) return closestLoc;
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
            shake(ti.getID());
            return true;
        }
        return false;
    }

    protected float sqrFloat(float a) {
        return a * a;
    }

    protected RobotInfo getActualNearestEnemy(MapLocation loc) {
        float minDist = Float.MAX_VALUE;
        RobotInfo closest = null;
        float nextDist;

        for (RobotInfo e: nearbyEnemies) {
            nextDist = loc.distanceSquaredTo(e.location);
            if (nextDist < minDist) {
                minDist = nextDist;
                closest = e;
            }
        }
        return closest;
    }

    protected TreeInfo getActualNearestOppTree(MapLocation loc) {
        float minDist = Float.MAX_VALUE;
        TreeInfo closest = null;
        float nextDist;

        for (TreeInfo e: nearbyTrees) {
            nextDist = loc.distanceSquaredTo(e.location);
            if (nextDist < minDist && !e.team.equals(myTeam)) {
                minDist = nextDist;
                closest = e;
            }
        }
        return closest;
    }

    protected void stayAwayFromAllies() throws GameActionException {
        float paddingForGardener = myType.equals(RobotType.GARDENER) ? 2F : 0F;
        for (RobotInfo ri : nearbyAllies) {
            if (location.distanceSquaredTo(ri.location) < sqrFloat(ri.type.bodyRadius + 2F + myType.bodyRadius + paddingForGardener)) {
                safeMove(ri.location.directionTo(location));
                return;
            }
        }
    }

    protected MapLocation acquireDestination() throws GameActionException {
        int currentRound = rc.getRoundNum();

        // look for an AOI
        DecodedLocation dl = Coms.decodeLocation(rc.readBroadcast(Coms.AREA_OF_INTEREST_1));
        if (dl != null && dl.roundNum - currentRound < 100) {
            return dl.location;
        }

        dl = Coms.decodeLocation(rc.readBroadcast(Coms.AREA_OF_INTEREST_2));
        if (dl != null && dl.roundNum - currentRound < 100) {
            return dl.location;
        }

        dl = Coms.decodeLocation(rc.readBroadcast(Coms.AREA_OF_INTEREST_3));
        if (dl != null && dl.roundNum - currentRound < 100) {
            return dl.location;
        }

        return null;
    }

    protected boolean atDestination(MapLocation l) throws GameActionException {
        return rc.canSenseLocation(l) && rc.getLocation().distanceSquaredTo(l) < 16;
    }

    protected MapLocation getDestination() throws GameActionException {
        if (destination == null || atDestination(location)) {
            if (nearbyEnemies.length > 0) {
                destination = nearbyEnemies[0].getLocation();
            } else {
                destination = acquireDestination();
            }
        }
        return destination;
    }
}
