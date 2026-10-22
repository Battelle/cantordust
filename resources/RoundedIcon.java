package resources;

import javax.swing.Icon;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * An image icon with rounded corners.
 *
 * The toolbar icons are crops of the visualizations - square tiles of pixels
 * to the edge. Rounding them in code rather than in the files keeps one radius
 * for all of them, leaves the source images untouched, and works for the
 * bitmap ones, whose format has no dependable alpha channel to round in.
 *
 * The corners are cut with an antialiased mask rather than a clip, which would
 * give jagged edges: the rounded shape is filled, then the image is composited
 * into it with SrcIn so only the covered pixels survive, and the result is
 * cached since the image never changes.
 */
public class RoundedIcon implements Icon {

    /** Corner radius as a fraction of the shorter side. */
    private static final double RADIUS = 0.2;

    private final Image image;
    private final int width;
    private final int height;
    private BufferedImage rounded;

    public RoundedIcon(Image image, int width, int height) {
        this.image = image;
        this.width = width;
        this.height = height;
    }

    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public int getIconHeight() {
        return height;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        if (rounded == null) {
            rounded = render();
        }
        g.drawImage(rounded, x, y, null);
    }

    private BufferedImage render() {
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            double d = Math.min(width, height) * RADIUS * 2;
            g2.setColor(Color.WHITE);
            g2.fill(new RoundRectangle2D.Double(0, 0, width, height, d, d));
            g2.setComposite(AlphaComposite.SrcIn);
            g2.drawImage(image, 0, 0, width, height, null);
        } finally {
            g2.dispose();
        }
        return out;
    }
}
