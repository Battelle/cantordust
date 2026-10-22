package resources;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Clicks a grid over each visualization and checks that the offset it reports
 * really holds the bytes it names.
 */
public class JumpTest {
    static byte[] data;
    static int fails = 0, checks = 0;

    public static void main(String[] a) throws Exception {
        for (String f : a) {
            T.boot(f);
            data = T.host.getData();
            SwingUtilities.invokeAndWait(() -> {
                T.mi.setSize(900, 640);
                T.lay(T.mi);
            });
            System.out.println("== " + f.substring(Math.max(f.lastIndexOf('/'), f.lastIndexOf('\\')) + 1) + " ==");
            probe("bitmap", T.mi.eightBitPerPixelBitMapButton);
            probe("2tuple", T.mi.twoTupleButton);
            probe("1tuple", T.mi.oneTupleButton);
            probe("bytecloud", T.mi.byteCloudButton);
            probe("3tuple", T.mi.threeTupleButton);
        }
        System.out.println(fails == 0 ? "ALL JUMP CHECKS PASSED (" + checks + ")" : fails + " FAILURES of " + checks);
        System.exit(fails == 0 && checks > 0 ? 0 : 1);
    }

    static void probe(String name, JButton b) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            b.doClick();
            T.lay(T.mi);
        });
        Thread.sleep(2000);
        SwingUtilities.invokeAndWait(() -> T.lay(T.mi));
        Thread.sleep(300);
        T.shot(Math.max(1, T.mi.getWidth()), Math.max(1, T.mi.getHeight()));
        Thread.sleep(200);
        JPanel vis = T.mi.currVis;
        int w = vis.getWidth(), h = vis.getHeight();
        int hits = 0;
        for (int gy = 1; gy < 9 && hits < 4; gy++) {
            for (int gx = 1; gx < 9 && hits < 4; gx++) {
                T.lastGoto = -1;
                final int px = gx * w / 10, py = gy * h / 10;
                SwingUtilities.invokeAndWait(() -> T.mi.setStatus(""));
                SwingUtilities.invokeAndWait(() -> {
                    MouseEvent e = new MouseEvent(vis, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, px, py, 1, false, MouseEvent.BUTTON1);
                    for (MouseListener ml : vis.getMouseListeners()) {
                        ml.mouseClicked(e);
                    }
                });
                Thread.sleep(name.equals("3tuple") ? 500 : 120);
                String st = T.mi.visStatus.getText().trim();
                if (st.isEmpty() || T.lastGoto < 0) {
                    continue;
                }
                hits++;
                checks++;
                boolean ok = verify(name, st, T.lastGoto);
                if (!ok) {
                    fails++;
                }
                System.out.printf("   %-9s -> %-44s %s%n", name, st, ok ? "OK" : "*** MISMATCH");
            }
        }
        if (hits == 0) {
            System.out.printf("   %-9s no resolvable clicks%n", name);
        }
    }

    static boolean verify(String name, String st, long off) {
        if (off < 0 || off >= data.length) {
            return false;
        }
        Matcher m;
        if (name.equals("2tuple")) {
            m = Pattern.compile("^([0-9A-F]{2}) ([0-9A-F]{2})").matcher(st);
            return m.find() && off + 1 < data.length
                    && (data[(int) off] & 0xff) == Integer.parseInt(m.group(1), 16)
                    && (data[(int) off + 1] & 0xff) == Integer.parseInt(m.group(2), 16);
        }
        if (name.equals("3tuple")) {
            m = Pattern.compile("^([0-9A-F]{2}) ([0-9A-F]{2}) ([0-9A-F]{2})").matcher(st);
            return m.find() && off + 2 < data.length
                    && (data[(int) off] & 0xff) == Integer.parseInt(m.group(1), 16)
                    && (data[(int) off + 1] & 0xff) == Integer.parseInt(m.group(2), 16)
                    && (data[(int) off + 2] & 0xff) == Integer.parseInt(m.group(3), 16);
        }
        m = Pattern.compile("([0-9A-F]{2})").matcher(st);
        return m.find() && (data[(int) off] & 0xff) == Integer.parseInt(m.group(1), 16);
    }
}
