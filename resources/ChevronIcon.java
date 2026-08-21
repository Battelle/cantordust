package resources;

import javax.swing.Icon;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

/**
 * A directional chevron for the nudge buttons.
 *
 * Drawn with Java2D for the same reasons as ThemeIcon: it stays sharp on high
 * density displays and takes the button's foreground colour, so it follows the
 * theme instead of being a "<" or ">" character rendered in whatever font the
 * look and feel happened to pick.
 */
public class ChevronIcon implements Icon {

    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    public static final int UP = 2;
    public static final int DOWN = 3;

    private final int direction;
    private final int size;

    public ChevronIcon(int direction, int size) {
        this.direction = direction;
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
            g2.setStroke(new BasicStroke(Math.max(1.4f, size / 8f),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            // A third of the box either side of centre: enough to read at 16px
            // without the stroke ends touching the button edge.
            float cx = x + size / 2f;
            float cy = y + size / 2f;
            float arm = size * 0.26f;

            Path2D.Float p = new Path2D.Float();
            switch (direction) {
                case LEFT:
                    p.moveTo(cx + arm * 0.6f, cy - arm);
                    p.lineTo(cx - arm * 0.6f, cy);
                    p.lineTo(cx + arm * 0.6f, cy + arm);
                    break;
                case UP:
                    p.moveTo(cx - arm, cy + arm * 0.6f);
                    p.lineTo(cx, cy - arm * 0.6f);
                    p.lineTo(cx + arm, cy + arm * 0.6f);
                    break;
                case DOWN:
                    p.moveTo(cx - arm, cy - arm * 0.6f);
                    p.lineTo(cx, cy + arm * 0.6f);
                    p.lineTo(cx + arm, cy - arm * 0.6f);
                    break;
                default:
                    p.moveTo(cx - arm * 0.6f, cy - arm);
                    p.lineTo(cx + arm * 0.6f, cy);
                    p.lineTo(cx - arm * 0.6f, cy + arm);
                    break;
            }
            g2.draw(p);
        } finally {
            g2.dispose();
        }
    }
}
