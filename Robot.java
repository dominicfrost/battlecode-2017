package battlecode2017;
import java.util.Arrays;
import java.util.Random;
import battlecode.common.*;

abstract public class Robot {
    // DEBUG CONSTANTS
    private final int ROBOT_ID = 11849;
    private final int MIN_ROUND = 169;
    private final int MAX_ROUND = 170;

    private final double POTENTIAL_LOC_GRANULARITY = 1.0;
    private final float BROADCAST_FLOAT_MULTIPLIER = 100F;
    private final int CIRCLING_GRANULARITY = 8;
    private final float CIRCLING_DEGREE_INTERVAL = 360.0F / CIRCLING_GRANULARITY;
    private final int TRY_MOVE_DEGREE_OFFSET = 10;

    private final int SPAWN_SEARCH_DEGREE = 30;
    private final float SPAWN_SEARCH_DEGREE_INTERVAL = 360.0F / SPAWN_SEARCH_DEGREE;

    // Broadcast channels
    private final int HOME_X = 0;
    private final int HOME_Y = 1;

    protected Random rand;
    protected Team myTeam;
    protected Team enemyTeam;
    protected RobotController rc;
    protected MapLocation location;
    protected BulletInfo[] nearbyBullets;
    protected RobotInfo[] nearbyBots;
    protected RobotInfo[] nearbyEnemies;
    protected RobotInfo[] nearbyAllies;
    protected TreeInfo[] nearbyTrees;
    protected RobotType myType;
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

    protected void debug(String msg) {
        if (debugCheck()) System.out.println(msg);
    }

    protected void debugIndicator(MapLocation l) throws GameActionException {
        if (debugCheck() && rc.onTheMap(l)) rc.setIndicatorDot(l, 0,0,0);
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
        while(currentCheck<=checksPerSide) {
            // Try the offset of the left side
            if(rc.canMove(dir.rotateLeftDegrees(TRY_MOVE_DEGREE_OFFSET * currentCheck))) {
                rc.move(dir.rotateLeftDegrees(TRY_MOVE_DEGREE_OFFSET * currentCheck));
                return true;
            }
            // Try the offset on the right side
            if(rc.canMove(dir.rotateRightDegrees(TRY_MOVE_DEGREE_OFFSET * currentCheck))) {
                rc.move(dir.rotateRightDegrees(TRY_MOVE_DEGREE_OFFSET * currentCheck));
                return true;
            }
            // No move performed, try slightly further
            currentCheck++;
        }

        // A move never happened, so return false.
        return false;
    }

    protected MapLocation findSpotAroundCircle(MapLocation center, float centerRadius, float revolverRadius) throws GameActionException {
        float distanceToCenter = centerRadius + revolverRadius;
        debug("findSpotAroundCircle\n  center: " + center.toString());

        MapLocation nextLoc;
        Direction nextDir = Direction.getEast();
        for (int i = 0; i < CIRCLING_GRANULARITY; i++) {
            nextDir = nextDir.rotateLeftDegrees(CIRCLING_DEGREE_INTERVAL * i);
            debug("  nextDir " + nextDir.toString());
            nextLoc = center.add(nextDir, distanceToCenter);
            debug("  nextLoc " + nextLoc.toString());
            debugIndicator(nextLoc);
            if (rc.canSenseAllOfCircle(nextLoc, revolverRadius) && rc.onTheMap(nextLoc) && !rc.isCircleOccupied(nextLoc, revolverRadius)  && !rc.isCircleOccupied(nextLoc, revolverRadius)) return nextLoc;
        }

        return null;
    }

    protected float convertCoordinateFromBroadcast(int v) throws GameActionException {
         return ( (float) v ) / BROADCAST_FLOAT_MULTIPLIER;
    }

    protected int convertCoordinateToBroadcast(float v) throws GameActionException {
        return (int) (v * BROADCAST_FLOAT_MULTIPLIER);
    }


    protected boolean tryDodge() throws GameActionException {
        return willAnyBulletsCollideWithMe() && moveToDodgeBullet();
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
        return false;
    }

    protected boolean willCollideWithMe(BulletInfo bullet) {
        return willCollideLocation(bullet, location);
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

    protected boolean moveToDodgeBullet() throws GameActionException {
        MapLocation[] locs = potentialDodgeLocations(POTENTIAL_LOC_GRANULARITY);
        if (locs.length == 0) return false;
        for (int i = locs.length -1; i >= 0; i--) {
            if (rc.canMove(locs[i])) {
                rc.move(locs[i]);
                return true;
            }
        }
        return false;
    }

    protected void initRobotState() throws GameActionException {
        rand = new Random();
        home = home();
        myTeam = rc.getTeam();
        enemyTeam = myTeam.opponent();
        myType = rc.getType();
    }

    protected void initRoundState() {
        location = rc.getLocation();
        nearbyBullets = rc.senseNearbyBullets();
        nearbyBots = rc.senseNearbyRobots();
        nearbyAllies = filterNearbyBots(myTeam);
        nearbyEnemies = filterNearbyBots(enemyTeam);
        nearbyTrees = rc.senseNearbyTrees();
        bulletCount = rc.getTeamBullets();
    }



    private MapLocation home() throws GameActionException {
        int x = rc.readBroadcast(HOME_X);
        int y = rc.readBroadcast(HOME_Y);

        // we have not set up a home yet
        if (x == 0 && y == 0) {
            return setHome();
        }

        return new MapLocation(convertCoordinateFromBroadcast(x), convertCoordinateFromBroadcast(y));
    }

    private MapLocation setHome() throws GameActionException {
        MapLocation home = rc.getLocation();
        rc.broadcast(HOME_X, convertCoordinateToBroadcast(home.x));
        rc.broadcast(HOME_Y, convertCoordinateToBroadcast(home.y));
        return home;
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



    protected Direction getBuildDirection(RobotType t) {
        Direction dir;
        for (int i = 0; i < SPAWN_SEARCH_DEGREE_INTERVAL; i++) {
            dir = Direction.getNorth().rotateRightDegrees(SPAWN_SEARCH_DEGREE * i);
            if (rc.canBuildRobot(t, dir)) return dir;
        }
        return null;
    }
}
