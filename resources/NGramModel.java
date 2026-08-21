package resources;

/**
 * An n-gram frequency model of one content class, stored as a fixed table of
 * log probabilities.
 *
 * The previous implementation kept a HashMap<VectorN, Double> with one entry per
 * distinct n-gram. Measured against the templates in this repository, that cost
 * about fifteen times the size of the input: the 82MB template set became
 * roughly 1.2GB of boxed doubles, hash nodes and four-byte arrays, which does not
 * fit a default Ghidra heap. Scoring was worse than the memory - classifying one
 * block built a whole second HashMap, then walked it once per class doing
 * arbitrary-precision arithmetic.
 *
 * Here every n-gram is hashed into a fixed number of buckets and the table holds
 * log P(bucket) directly. Memory no longer depends on the size of the template,
 * scoring is an array read and an add per gram, and probabilities multiply as
 * sums of logs, which is both faster and better conditioned than the exponential
 * notation it replaces - a 12-byte block produced products around 1e-100, which
 * is exactly what logarithms exist to avoid.
 */
class NGramModel {

    /**
     * 2^20 buckets, four bytes each: 4MB per class, 68MB for the full set of
     * seventeen. Collisions blur rare n-grams into each other, which costs
     * little - classification is driven by the common ones, and a frequent gram
     * sharing a bucket with a rare one barely moves its probability.
     */
    static final int BUCKET_BITS = 20;

    /**
     * Weight given to a uniform distribution when smoothing, so an n-gram absent
     * from a class is unlikely rather than impossible - without it a single
     * unseen gram would drive the whole block's likelihood to zero.
     *
     * It has to be mixed in at a fixed weight rather than as a pseudo-count.
     * Additive smoothing makes the floor ALPHA/(total + ALPHA*buckets), which
     * falls as a class is trained on more data, so classes with small templates
     * assign unseen grams a higher probability and win by default. That is not a
     * subtle effect: with additive smoothing the two smallest templates, "ones"
     * and "zeros", took 64% of the blocks of a Mach-O executable that is 2.8%
     * 0xFF. Interpolating against a uniform gives every class the same floor.
     */
    private static final double LAMBDA = 0.1;

    private final int n;
    private final int mask;
    private final float[] logP;

    NGramModel(byte[] data, int n) {
        this(data, 0, data.length, n);
    }

    NGramModel(byte[] data, int startIndex, int length, int n) {
        if (n < 1) {
            throw new IllegalArgumentException("n must be positive");
        }
        this.n = n;
        int buckets = 1 << BUCKET_BITS;
        this.mask = buckets - 1;

        int[] counts = new int[buckets];
        long total = count(data, startIndex, length, counts);

        this.logP = new float[buckets];
        double uniform = LAMBDA / buckets;
        for (int i = 0; i < buckets; i++) {
            double empirical = (total > 0) ? counts[i] / (double) total : 0;
            logP[i] = (float) Math.log((1 - LAMBDA) * empirical + uniform);
        }
    }

    /** Tallies every n-gram of the range into counts, returning how many there were. */
    private long count(byte[] data, int startIndex, int length, int[] counts) {
        int start = Math.max(0, startIndex);
        int end = (int) Math.min(data.length, (long) startIndex + length);
        long total = 0;
        if (end - start < n) {
            return 0;
        }
        if (n <= 4) {
            // The gram fits an int, so roll it a byte at a time.
            int keyMask = (n == 4) ? -1 : ((1 << (8 * n)) - 1);
            int key = 0;
            for (int i = start; i < end; i++) {
                key = ((key << 8) | (data[i] & 0xff)) & keyMask;
                if (i - start + 1 >= n) {
                    counts[mix(key) & mask]++;
                    total++;
                }
            }
        } else {
            for (int i = start; i + n <= end; i++) {
                int h = 0;
                for (int k = 0; k < n; k++) {
                    h = h * 31 + (data[i + k] & 0xff);
                }
                counts[mix(h) & mask]++;
                total++;
            }
        }
        return total;
    }

    /**
     * Log probability of a range under this model. Higher is a better match.
     * Ranges shorter than one n-gram score 0, which leaves every class tied.
     */
    double logLikelihood(byte[] data, int from, int to) {
        int start = Math.max(0, from);
        int end = Math.min(data.length, to);
        if (end - start < n) {
            return 0;
        }
        double sum = 0;
        if (n <= 4) {
            int keyMask = (n == 4) ? -1 : ((1 << (8 * n)) - 1);
            int key = 0;
            for (int i = start; i < end; i++) {
                key = ((key << 8) | (data[i] & 0xff)) & keyMask;
                if (i - start + 1 >= n) {
                    sum += logP[mix(key) & mask];
                }
            }
        } else {
            for (int i = start; i + n <= end; i++) {
                int h = 0;
                for (int k = 0; k < n; k++) {
                    h = h * 31 + (data[i + k] & 0xff);
                }
                sum += logP[mix(h) & mask];
            }
        }
        return sum;
    }

    int getN() {
        return n;
    }

    /** murmur3 finalizer: spreads sequential byte patterns across the table. */
    private static int mix(int h) {
        h ^= h >>> 16;
        h *= 0x85ebca6b;
        h ^= h >>> 13;
        h *= 0xc2b2ae35;
        h ^= h >>> 16;
        return h;
    }
}
