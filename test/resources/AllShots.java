package resources;

import java.awt.Component;
import java.awt.Rectangle;

import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Every visualization, at three window sizes: fits, and every child stays inside the panel. */
public class AllShots {
    public static void main(String[] a) throws Exception {
        T.boot(a[0]);
        System.out.println("  look and feel: " + UIManager.getLookAndFeel().getName());
        String[] names = {"metric", "2tuple", "bitmap", "bytecloud", "1tuple", "3tuple"};
        int[][] sizes = {{725, 580}, {460, 340}, {1200, 820}};
        boolean bad = false;
        int n = 0;
        for (int[] wh : sizes) {
            for (int i = 0; i < names.length; i++) {
                final int idx = i, w = wh[0], h = wh[1];
                SwingUtilities.invokeAndWait(() -> {
                    new JButton[]{T.mi.metricMapButton, T.mi.twoTupleButton, T.mi.eightBitPerPixelBitMapButton,
                            T.mi.byteCloudButton, T.mi.oneTupleButton, T.mi.threeTupleButton}[idx].doClick();
                    T.mi.setSize(w, h);
                    T.lay(T.mi);
                });
                Thread.sleep(900);
                SwingUtilities.invokeAndWait(() -> T.lay(T.mi));
                Thread.sleep(300);
                T.shot(w, h);
                Rectangle r = T.mi.currVis.getBounds();
                Rectangle box = new Rectangle(0, 0, T.mi.getWidth(), T.mi.getHeight());
                boolean inside = true;
                for (Component c : T.mi.getComponents()) {
                    if (!box.contains(c.getBounds())) {
                        inside = false;
                    }
                }
                if (r.width < 50 || r.height < 50 || !inside) {
                    bad = true;
                    System.out.printf("  PROBLEM %s %dx%d currVis %dx%d inside=%s%n", names[i], w, h, r.width, r.height, inside);
                }
                n++;
            }
        }
        System.out.println("  " + n + " layouts: " + (bad ? "PROBLEMS" : "all OK"));
        System.exit(bad ? 1 : 0);
    }
}
