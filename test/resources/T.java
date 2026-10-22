package resources;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.SwingUtilities;

/** Shared setup for the checks: the real StandaloneHost and MainInterface, no doubles. */
final class T {

    static MainInterface mi;
    static StandaloneHost host;
    static volatile long lastGoto = -1;

    private T() {
    }

    static void boot(String file) throws Exception {
        byte[] data = Files.readAllBytes(Path.of(file));
        host = new StandaloneHost(data, Path.of(file).getFileName().toString(), System.getProperty("user.dir"));
        host.setGotoHandler(o -> {
            lastGoto = o;
            return true;
        });
        SwingUtilities.invokeAndWait(() -> {
            try {
                mi = new MainInterface(data, host);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        host.setMainInterface(mi);
    }

    static void lay(Container c) {
        c.doLayout();
        for (Component x : c.getComponents()) {
            if (x instanceof Container) {
                lay((Container) x);
            }
        }
    }

    static BufferedImage shot(int w, int h) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        SwingUtilities.invokeAndWait(() -> {
            Graphics2D g = img.createGraphics();
            mi.paint(g);
            g.dispose();
        });
        return img;
    }
}
