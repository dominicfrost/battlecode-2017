package battlecode2017;

import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;

import java.util.Comparator;

class BotComparator implements Comparator<RobotInfo> {
    MapLocation location;

    BotComparator(MapLocation loc) {
        location = loc;
    }

    @Override
    public int compare(RobotInfo a, RobotInfo b) {
        return (int) a.location.distanceSquaredTo(location) - (int) b.location.distanceSquaredTo(location);
    }
}