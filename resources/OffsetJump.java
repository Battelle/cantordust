package resources;

/**
 * Turns a click in a visualization into a jump to the bytes behind it.
 *
 * Every visualization here is a lossy projection of the file, so a pixel maps
 * back to either one offset (the linear maps, where position is the axis) or to
 * a byte pattern that may occur many times (the tuple plots, where position is
 * the value). Both end up in the Ghidra listing; the difference is only whether
 * a search is needed to get there.
 *
 * Repeating a click on the same target advances to the next occurrence, so a
 * cell with hundreds of hits can be walked rather than always re-reporting the
 * first one.
 */
class OffsetJump {

    /** Matches any value at that position of the pattern. */
    static final int ANY = 0x100;

    private final GhidraSrc cantordust;
    private int[] lastPattern;
    private long lastHit = -1;

    OffsetJump(GhidraSrc cantordust) {
        this.cantordust = cantordust;
    }

    /**
     * Jump straight to a known offset. Used by the maps whose geometry already
     * is the file position.
     */
    String toOffset(long offset, String what) {
        if(offset < 0) {
            return "";
        }
        boolean went = go(offset);
        return String.format("%s @ 0x%X%s", what, offset, went ? "" : "   (not mapped)");
    }

    /**
     * Find the pattern in [low, high) and jump to it, counting how often it
     * occurs. Pattern entries are byte values, or ANY for a wildcard.
     */
    String toPattern(byte[] data, int low, int high, int[] pattern, String what) {
        if(data == null || pattern.length == 0) {
            return "";
        }
        low = Math.max(0, low);
        high = Math.min(data.length, high);
        if(high - low < pattern.length) {
            return what + "   nothing in range";
        }

        // Walking the range once gives both the count and the next hit after the
        // one we reported last time, so a repeated click steps forward.
        boolean same = java.util.Arrays.equals(pattern, lastPattern);
        long from = same ? lastHit : -1;
        int count = 0;
        long first = -1;
        long next = -1;
        int end = high - pattern.length;
        for(int i = low; i <= end; i++) {
            if(!matches(data, i, pattern)) {
                continue;
            }
            count++;
            if(first < 0) {
                first = i;
            }
            if(next < 0 && i > from) {
                next = i;
            }
        }
        if(count == 0) {
            lastPattern = pattern.clone();
            lastHit = -1;
            return what + "   no match in range";
        }
        // Past the last occurrence, wrap round to the first.
        long target = (next >= 0) ? next : first;
        lastPattern = pattern.clone();
        lastHit = target;

        boolean went = go(target);
        int index = 1;
        for(int i = low; i <= end && i < target; i++) {
            if(matches(data, i, pattern)) {
                index++;
            }
        }
        return String.format("%s   %d of %d   @ 0x%X%s",
                what, index, count, target, went ? "" : "   (not mapped)");
    }

    private static boolean matches(byte[] data, int at, int[] pattern) {
        for(int k = 0; k < pattern.length; k++) {
            int want = pattern[k];
            if(want != ANY && (data[at + k] & 0xff) != want) {
                return false;
            }
        }
        return true;
    }

    private boolean go(long offset) {
        try {
            return cantordust.gotoFileAddress(offset);
        } catch (RuntimeException e) {
            // An offset outside every mapped block has no address; that is a
            // normal outcome for padding, not a reason to fail the click.
            cantordust.cdprint("goto failed for offset " + offset + ": " + e + "\n");
            return false;
        }
    }
}
