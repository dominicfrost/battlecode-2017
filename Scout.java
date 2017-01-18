package battlecode2017;
import battlecode.common.*;

public class Scout extends Robot {
    Direction scoutingDirection;

    Scout(RobotController _rc) {
        super(_rc);
    }

    protected void initRobotState() throws GameActionException {
        super.initRobotState();
        scoutingDirection = rc.getLocation().directionTo(enemyArchonLocs[rc.getID() % enemyArchonLocs.length]);
    }

    protected void doTurn() throws GameActionException {
        determineAreasOfInterest();
        if (sitOnGardenerTree()) return;

        if (!tryDodge()) {

            if (!rc.onTheMap(location.add(scoutingDirection, myType.sensorRadius - 1))) {
                scoutingDirection = randomDirection();
            }


            if (attackAndFleeIfWayClose())return;

            tryMove(scoutingDirection);
        }
        if (attackIfWayClose()) return;
        pieCountAttack();
    }

    private void determineAreasOfInterest() throws GameActionException {
        if (nearbyEnemies.length > 3) {
            int currentRoundNum = rc.getRoundNum();
            // write to the first AOI channel that is stale ( > 100 rounds old)
            DecodedLocation loc = Coms.decodeLocation(rc.readBroadcast(Coms.AREA_OF_INTEREST_1));
            if (loc.roundNum == 0 || currentRoundNum - loc.roundNum > 100) {
                rc.broadcast(Coms.AREA_OF_INTEREST_1, Coms.encodeLocation(location, currentRoundNum));
                return;
            }

            loc = Coms.decodeLocation(rc.readBroadcast(Coms.AREA_OF_INTEREST_2));
            if (loc.roundNum == 0 || currentRoundNum - loc.roundNum > 100) {
                rc.broadcast(Coms.AREA_OF_INTEREST_2, Coms.encodeLocation(location, currentRoundNum));
                return;
            }

            loc = Coms.decodeLocation(rc.readBroadcast(Coms.AREA_OF_INTEREST_3));
            if (loc.roundNum == 0 || currentRoundNum - loc.roundNum > 100) {
                rc.broadcast(Coms.AREA_OF_INTEREST_3, Coms.encodeLocation(location, currentRoundNum));
            }
        }
    }

    private boolean sitOnGardenerTree() throws GameActionException {
        MapLocation attackLoc;
        float dist;
        MapLocation closestLoc = null, gardenerLoc = null;
        float closestDist = Float.MAX_VALUE;

        for (RobotInfo ri : nearbyEnemies) {
            if (ri.type.equals(RobotType.GARDENER)) {
                attackLoc = findTreeNextToGardener(ri.location, RobotType.GARDENER.bodyRadius, myType.bodyRadius);
                if (attackLoc != null) {
                    dist = location.distanceSquaredTo(attackLoc);
                    if (dist < closestDist) {
                        closestLoc = attackLoc;
                        closestDist = dist;
                        gardenerLoc = ri.location;
                    }
                }
            }
        }

        if (closestLoc != null && gardenerLoc != null) {
            if (!location.equals(closestLoc) && rc.canMove(closestLoc)) move(closestLoc);
            if (location.equals(closestLoc) && rc.canFireSingleShot()) rc.fireSingleShot(location.directionTo(gardenerLoc));
            return true;
        }

        return false;
    }

    private MapLocation findTreeNextToGardener(MapLocation center, float centerRadius, float revolverRadius) throws GameActionException {
        float distanceToCenter = centerRadius + revolverRadius;
        float distanceToCenterSquared = (float) Math.pow(distanceToCenter, 2);

        if (location.distanceSquaredTo(center) <= distanceToCenterSquared + .001) return location;

        MapLocation nextLoc;
        Direction nextDir = Direction.getNorth();
        float dist;
        MapLocation closestLoc = null;
        float closestDist = Float.MAX_VALUE;

        for (int i = 0; i < 6; i++) {
            nextDir = nextDir.rotateLeftDegrees(60 * i);
            nextLoc = center.add(nextDir, distanceToCenter);
            if (rc.canSenseAllOfCircle(nextLoc, revolverRadius) && rc.onTheMap(nextLoc) && rc.isLocationOccupiedByTree(nextLoc)) {
                nextLoc = rc.senseTreeAtLocation(nextLoc).location;
                dist = location.distanceSquaredTo(nextLoc);
                if (dist < closestDist) {
                    closestLoc = nextLoc;
                    closestDist = dist;
                }
            }
        }

        return closestLoc;
    }
}