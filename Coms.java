package battlecode2017;
import battlecode.common.*;


public class Coms {

    // Broadcasting channels
    public static final int AREA_OF_INTEREST_1 = 1;
    public static final int AREA_OF_INTEREST_2 = 2;
    public static final int AREA_OF_INTEREST_3 = 3;

    public static final int PESKY_TREES = 500;
    public static final int PESKY_ATTACKERS = 1000;

    // Bitmasks
    private static final int tenBitMask =    0b1111111111;
    private static final int twelveBitMask = 0b111111111111;

    static int encodeLocation(MapLocation location, int roundNumber) {
        int x = ((int) location.x);
        int y = ((int) location.y) << 10;
        int round = roundNumber << 20;

        return x | y | round;
    }

    static int encodeLocation(MapLocation location) {
        int x = ((int) location.x);
        int y = ((int) location.y) << 10;

        return x | y;
    }

    static DecodedLocation decodeLocation(int location) {
        if (location == 0) return null;
        int x = location & tenBitMask;
        location >>= 10;
        int y = location & tenBitMask;
        location >>= 10;
        int round = location & twelveBitMask;

        return new DecodedLocation(new MapLocation(x, y), round);
    }

}

class DecodedLocation {
    MapLocation location;
    int roundNum;

    DecodedLocation(MapLocation location, int roundNum) {
        this.location = location;
        this.roundNum = roundNum;
    }
}
