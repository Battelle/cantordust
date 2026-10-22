package resources;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.IllegalComponentStateException;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Cantordust as a standalone application.
 *
 *   java -jar cantordust.jar [file]
 *
 * The same visualizations as the Ghidra script, hosted in a window of their own,
 * with a hex view standing in for the listing: a click in any visualization
 * scrolls the hex view to the bytes it resolved to.
 */
public final class CantordustApp {

    private CantordustApp() {
    }

    public static void main(String[] args) {
        EdtWatchdog.start();
        nativeLookAndFeel();
        File initial = (args.length > 0) ? new File(args[0]) : null;
        SwingUtilities.invokeLater(() -> {
            File f = initial;
            if (f == null) {
                f = chooseFile(null);
                if (f == null) {
                    return;
                }
            }
            open(f);
        });
    }

    /**
     * Look native on each platform. Without this, Windows and Linux get Swing's
     * cross-platform Metal look; macOS already defaults to Aqua. On macOS the
     * menu bar also moves to the top of the screen where the platform keeps it.
     * Any failure here is cosmetic, so it is not allowed to stop the launch.
     */
    private static void nativeLookAndFeel() {
        try {
            if (System.getProperty("os.name", "").toLowerCase().contains("mac")) {
                System.setProperty("apple.laf.useScreenMenuBar", "true");
                System.setProperty("apple.awt.application.name", "Cantordust");
            }
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // cosmetic
        }
    }

    private static File chooseFile(JFrame parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open a file to visualize");
        return chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION
                ? chooser.getSelectedFile() : null;
    }

    static JFrame open(File file) {
        byte[] data;
        try {
            data = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Could not read " + file + "\n" + e.getMessage(),
                    "Cantordust", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        if (data.length < 4) {
            JOptionPane.showMessageDialog(null, file.getName() + " is too small to visualize.",
                    "Cantordust", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        StandaloneHost host = new StandaloneHost(data, file.getName(), baseDir());
        MainInterface mi;
        try {
            mi = new MainInterface(data, host);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Could not build the interface: " + e.getMessage(),
                    "Cantordust", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        host.setMainInterface(mi);

        HexPanel hex = new HexPanel(data);
        host.setGotoHandler(offset -> {
            hex.showOffset(offset);
            return true;
        });

        JFrame frame = new JFrame("Cantordust - " + file.getName());
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setJMenuBar(menuBar(frame));
        applyIcon(frame, host);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mi, hex.inScrollPane());
        split.setResizeWeight(1.0);
        split.setContinuousLayout(true);
        split.setOneTouchExpandable(true);
        frame.add(split, BorderLayout.CENTER);

        mi.setPreferredSize(new Dimension(MainInterface.getWindowWidth(), MainInterface.getWindowHeight()));
        frame.pack();
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
        if (Boolean.getBoolean("cantordust.debug")) {
            // Lets a test drive the running app from outside without a screen
            // reader: where the File menu is, on screen.
            try {
                java.awt.Point p = frame.getJMenuBar().getMenu(0).getLocationOnScreen();
                System.out.println("cantordust.debug: file-menu at " + p.x + "," + p.y);
            } catch (IllegalComponentStateException e) {
                System.out.println("cantordust.debug: file-menu is on the screen menu bar");
            }
        }
        return frame;
    }

    /**
     * The same icon the Ghidra script uses, on the window and - where the
     * platform has one - the dock or taskbar. When run from a jpackage bundle
     * the bundle's own icon already covers the dock, so a failure here is only
     * cosmetic and is not worth surfacing.
     */
    private static void applyIcon(JFrame frame, Host host) {
        try (java.io.InputStream in = host.openResource("resources/icons/icon.png")) {
            java.awt.Image icon = javax.imageio.ImageIO.read(in);
            if (icon == null) {
                return;
            }
            frame.setIconImage(icon);
            if (java.awt.Taskbar.isTaskbarSupported()) {
                java.awt.Taskbar tb = java.awt.Taskbar.getTaskbar();
                if (tb.isSupported(java.awt.Taskbar.Feature.ICON_IMAGE)) {
                    tb.setIconImage(icon);
                }
            }
        } catch (IOException | RuntimeException e) {
            // cosmetic
        }
    }

    private static JMenuBar menuBar(JFrame frame) {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem open = new JMenuItem("Open…");
        open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
                frame.getToolkit().getMenuShortcutKeyMaskEx()));
        open.addActionListener(e -> {
            File f = chooseFile(frame);
            if (f != null) {
                open(f);
            }
        });
        file.add(open);
        JMenuItem quit = new JMenuItem("Quit");
        quit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,
                frame.getToolkit().getMenuShortcutKeyMaskEx()));
        quit.addActionListener(e -> System.exit(0));
        file.add(quit);
        bar.add(file);
        return bar;
    }

    /**
     * Where to look for resources when they are not on the classpath: the
     * directory holding the jar, or the working directory in a checkout.
     */
    private static String baseDir() {
        try {
            File loc = new File(CantordustApp.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File dir = loc.isFile() ? loc.getParentFile() : loc;
            // In a checkout the classes live under out/ or build/; walk up to
            // the directory that actually holds resources/.
            for (File d = dir; d != null; d = d.getParentFile()) {
                if (new File(d, "resources").isDirectory()) {
                    return d.getAbsolutePath();
                }
            }
            return dir.getAbsolutePath();
        } catch (Exception e) {
            return new File(".").getAbsolutePath();
        }
    }
}
