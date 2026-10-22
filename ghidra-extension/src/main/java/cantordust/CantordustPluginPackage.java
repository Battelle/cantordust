package cantordust;

import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import ghidra.framework.plugintool.util.PluginPackage;

/** Cantordust's own group in Ghidra's plugin configuration dialog. */
public class CantordustPluginPackage extends PluginPackage {

    public static final String NAME = "Cantordust";

    public CantordustPluginPackage() {
        super(NAME, icon(),
                "Binary visualization: see the shape of a file, then click through to the bytes that made it.");
    }

    /**
     * Loaded with ImageIO rather than Ghidra's ResourceManager. That class lives
     * in a package named "resources", and so does every Cantordust visualization
     * class; importing it from here would put two packages of that name in play
     * in one classloader, which is a split-package problem waiting to happen.
     */
    private static Icon icon() {
        try (InputStream in = CantordustPluginPackage.class.getClassLoader()
                .getResourceAsStream("images/cantordust.png")) {
            if (in != null) {
                return new ImageIcon(ImageIO.read(in));
            }
        } catch (IOException | RuntimeException e) {
            // fall through to no icon
        }
        return new ImageIcon();
    }
}
