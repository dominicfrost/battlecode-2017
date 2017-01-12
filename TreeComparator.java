package battlecode2017;

import battlecode.common.MapLocation;
import battlecode.common.TreeInfo;

import java.util.Comparator;

class TreeComparator implements Comparator<TreeInfo> {
    MapLocation location;

    TreeComparator(MapLocation loc) {
        location = loc;
    }

    @Override
    public int compare(TreeInfo a, TreeInfo b) {
        return (int) a.location.distanceSquaredTo(location) - (int) b.location.distanceSquaredTo(location);
    }
}