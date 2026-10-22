package resources;

import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

/** The entropy strip over four known regions: right values, increasing order. */
public class EntropyTest {
    public static void main(String[] a) throws Exception {
        byte[] d = Files.readAllBytes(Path.of(a[0]));
        BitMapSlider s = new BitMapSlider(1, d.length - 1, d, new StandaloneHost(d, "x", System.getProperty("user.dir")));
        Method es = BitMapSliderUI.class.getDeclaredMethod("entropyStrip", byte[].class, int.class, int.class);
        es.setAccessible(true);
        BufferedImage strip = (BufferedImage) es.invoke(s.ui, d, 0, d.length);
        int rows = strip.getHeight();
        String[] nm = {"zeros", "ascii", "64-symbol", "random"};
        double[] ex = {0, 4.3, 6.0, 8};
        double[] tol = {0.5, 0.9, 0.5, 0.9};
        boolean bad = false;
        double prev = -1;
        for (int r = 0; r < 4; r++) {
            int rgb = strip.getRGB(0, (int) ((r + .5) * rows / 4));
            int R = (rgb >> 16) & 255, G = (rgb >> 8) & 255, B = rgb & 255;
            int v = B > 0 ? (B + 510) / 3 : G > 0 ? (G + 255) / 3 : R / 3;
            double bits = v * 8.0 / 255;
            boolean ok = Math.abs(bits - ex[r]) < tol[r] && bits >= prev - 0.2;
            if (!ok) {
                bad = true;
            }
            prev = bits;
            System.out.printf("  %-7s ~%.2f bits (expect ~%.1f) %s%n", nm[r], bits, ex[r], ok ? "OK" : "*** OFF");
        }
        System.out.println(bad ? "ENTROPY FAIL" : "entropy OK, monotonic");
        System.exit(bad ? 1 : 0);
    }
}
