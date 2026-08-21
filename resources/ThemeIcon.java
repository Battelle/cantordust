package resources;

import javax.swing.Icon;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

/**
 * A sun or a crescent moon for the theme toggle.
 *
 * Drawn with Java2D rather than loaded from a bitmap so it stays sharp on high
 * density displays and picks up the button's foreground colour in either theme.
 */
public class ThemeIcon implements Icon {

    private final boolean sun;
    private final int size;

    public ThemeIcon(boolean sun, int size) {
        this.sun = sun;
        this.size = size;
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c.getForeground());
            double r = size / 2.0;
            if (sun) {
                double body = r * 0.5;
                g2.fill(new Ellipse2D.Double(x + r - body, y + r - body, body * 2, body * 2));
                g2.setStroke(new BasicStroke(Math.max(1f, size / 12f),
                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * i / 4.0;
                    g2.draw(new Line2D.Double(
                            x + r + Math.cos(a) * body * 1.45, y + r + Math.sin(a) * body * 1.45,
                            x + r + Math.cos(a) * r * 0.95, y + r + Math.sin(a) * r * 0.95));
                }
            } else {
                Area moon = new Area(new Ellipse2D.Double(x + r * 0.2, y + r * 0.2, r * 1.6, r * 1.6));
                moon.subtract(new Area(new Ellipse2D.Double(x + r * 0.7, y, r * 1.6, r * 1.6)));
                g2.fill(moon);
            }
        } finally {
            g2.dispose();
        }
    }
}
