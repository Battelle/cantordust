package resources;

import java.util.Arrays;

/**
 * Shared tone mapping for the density plots.
 *
 * The tuple visualizations all produce the same thing: a buffer of counts, most
 * of them zero, a few of them enormous. Turning that into an image is where the
 * plot is won or lost. A linear ramp - which the 2-tuple plot used to use, at
 * five levels per occurrence - saturates at twenty occurrences and throws away
 * every gradient above it, which is precisely the structure worth seeing.
 *
 * So: normalise against a high percentile rather than the maximum, and compress
 * with a logarithm. The maximum is useless as a reference because one trigram or
 * digraph (00 00, FF FF, a hot opcode pair) routinely outruns the rest by four
 * orders of magnitude; dividing by it floors everything else to black.
 */
final class DensityShader {

    static final int TRIGRAM = 0;
    static final int TRIGRAM_LITERAL = 1;
    static final int GREEN = 2;
    static final int HEAT = 3;
    static final int ICE = 4;
    static final int MONO = 5;

    /** Counts at or above this are all "very bright"; keeps the lookup bounded. */
    static final int LUT_SIZE = 4096;

    /**
     * Percentile used as the exposure reference, chosen by measuring mean
     * brightness across a corpus spanning 5 bytes to 31MB and zeros to
     * compressed data. Higher percentiles collapse on small or highly repetitive
     * inputs, where the dominant value is itself inside the top 1%: a 512 byte
     * header rendered at a mean brightness of 38/255 at p99 and 177 at p90. The
     * top decile clipping to white is what a dense core should look like.
     */
    private static final double PERCENTILE = 0.90;

    private DensityShader() {
    }

    /**
     * Fills lut with intensities for counts 0..LUT_SIZE-1, scaled to this
     * buffer's own distribution. Returns the number of non-empty cells, or 0
     * when there is nothing to draw.
     *
     * hist and lut are caller-owned scratch of length LUT_SIZE, so a redraw at
     * animation rates does not allocate.
     */
    static int buildLut(int[] acc, float exposure, int[] hist, int[] lut) {
        Arrays.fill(hist, 0);
        int lit = 0;
        for (int i = 0; i < acc.length; i++) {
            int a = acc[i];
            if (a > 0) {
                hist[a < LUT_SIZE ? a : LUT_SIZE - 1]++;
                lit++;
            }
        }
        if (lit == 0) {
            return 0;
        }

        int target = (int) (lit * PERCENTILE);
        int ref = 1;
        int seen = 0;
        for (int v = 0; v < LUT_SIZE; v++) {
            seen += hist[v];
            if (seen >= target) {
                ref = v;
                break;
            }
        }
        float refEff = ref / exposure;
        if (refEff < 2f) {
            refEff = 2f;
        }
        double denom = Math.log1p(refEff);
        for (int i = 0; i < LUT_SIZE; i++) {
            int v = (int) (255.0 * Math.log1p(i) / denom);
            lut[i] = v < 0 ? 0 : (v > 255 ? 255 : v);
        }
        return lit;
    }

    /** Intensity for one accumulated count. */
    static int intensity(int[] lut, int count) {
        return lut[count < LUT_SIZE ? count : LUT_SIZE - 1];
    }

    /**
     * Packed RGB for an intensity. key carries the byte values behind the cell
     * (b0 in the high byte) for the trigram modes, and is ignored by the ramps.
     */
    static int shade(int mode, int v, int key) {
        switch (mode) {
            case MONO:
                return (v << 16) | (v << 8) | v;
            case GREEN:
                return ((v >> 3) << 16) | (v << 8) | (v >> 3);
            case HEAT: {
                int r = v * 3;
                int g = v * 3 - 255;
                int b = v * 3 - 510;
                if (r > 255) r = 255;
                if (g < 0) g = 0; else if (g > 255) g = 255;
                if (b < 0) b = 0; else if (b > 255) b = 255;
                return (r << 16) | (g << 8) | b;
            }
            case ICE: {
                int r = v < 128 ? 0 : (v - 128) * 2;
                int g = v < 64 ? 0 : (v - 64) * 255 / 191;
                int b = 40 + v * 215 / 255;
                if (g > 255) g = 255;
                if (b > 255) b = 255;
                return (r << 16) | (g << 8) | b;
            }
            default: {
                int r = (key >> 16) & 0xff;
                int g = (key >> 8) & 0xff;
                int b = key & 0xff;
                if (mode == TRIGRAM) {
                    // Byte values give the hue; the accumulated density gives the
                    // brightness. Scaling the raw values by intensity - which
                    // TRIGRAM_LITERAL still does - ties how bright a cell is to
                    // how large its bytes are, so ASCII, opcodes and zero padding
                    // all come out dark while compressed data glows.
                    int m = r > g ? (r > b ? r : b) : (g > b ? g : b);
                    if (m == 0) {
                        r = v; g = v; b = v;
                    } else {
                        r = r * v / m;
                        g = g * v / m;
                        b = b * v / m;
                    }
                } else {
                    r = r * v / 255;
                    g = g * v / 255;
                    b = b * v / 255;
                }
                // Floor keeps a dark cell visible against a black background.
                int floor = v / 4;
                if (r < floor) r = floor;
                if (g < floor) g = floor;
                if (b < floor) b = floor;
                return (r << 16) | (g << 8) | b;
            }
        }
    }

    static String name(int mode) {
        switch (mode) {
            case TRIGRAM_LITERAL: return "literal";
            case GREEN: return "green";
            case HEAT: return "heat";
            case ICE: return "ice";
            case MONO: return "mono";
            default: return "value";
        }
    }
}
