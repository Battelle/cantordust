package resources;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;

/**
 * A hex dump that can be told to show an offset.
 *
 * Standalone, this is what a click lands in - the counterpart of the Ghidra
 * listing. It paints only the rows on screen, so a large file costs nothing to
 * hold; the scroll position is the only state.
 */
public class HexPanel extends JComponent implements Scrollable {

    private static final int BYTES_PER_ROW = 16;

    private final byte[] data;
    private long highlight = -1;
    private int rowHeight = 16;
    private int charWidth = 8;

    public HexPanel(byte[] data) {
        this.data = data;
        setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        setOpaque(true);
        setBackground(new Color(0x14, 0x17, 0x1b));
        setForeground(new Color(0xd8, 0xdc, 0xe0));
        measure();
    }

    private void measure() {
        FontMetrics fm = getFontMetrics(getFont());
        rowHeight = fm.getHeight() + 2;
        charWidth = fm.charWidth('0');
        int rows = (data.length + BYTES_PER_ROW - 1) / BYTES_PER_ROW;
        // offset (8) + gap + hex (16*3) + gap + ascii (16)
        setPreferredSize(new Dimension(charWidth * (8 + 2 + BYTES_PER_ROW * 3 + 2 + BYTES_PER_ROW) + 16,
                Math.max(1, rows) * rowHeight));
    }

    /** Scroll so the row holding this offset is visible, and mark the byte. */
    public void showOffset(long offset) {
        if (offset < 0 || offset >= data.length) {
            return;
        }
        highlight = offset;
        int row = (int) (offset / BYTES_PER_ROW);
        Rectangle r = new Rectangle(0, row * rowHeight - rowHeight * 4, 1, rowHeight * 9);
        SwingUtilities.invokeLater(() -> {
            scrollRectToVisible(r);
            repaint();
        });
    }

    public long getHighlight() {
        return highlight;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Rectangle clip = g2.getClipBounds();
        g2.setColor(getBackground());
        g2.fillRect(clip.x, clip.y, clip.width, clip.height);

        int first = Math.max(0, clip.y / rowHeight);
        int last = Math.min((data.length - 1) / BYTES_PER_ROW, (clip.y + clip.height) / rowHeight + 1);
        int ascent = getFontMetrics(getFont()).getAscent();
        int xOffset = 8;
        int xHex = xOffset + charWidth * 10;
        int xAscii = xHex + charWidth * (BYTES_PER_ROW * 3 + 2);
        Color dim = new Color(0x7a, 0x82, 0x8a);
        Color mark = new Color(0x2e, 0x7d, 0x51);

        StringBuilder sb = new StringBuilder(BYTES_PER_ROW * 3);
        for (int row = first; row <= last; row++) {
            int y = row * rowHeight;
            int base = row * BYTES_PER_ROW;
            int baseline = y + ascent + 1;

            g2.setColor(dim);
            g2.drawString(String.format("%08X", base), xOffset, baseline);

            int end = Math.min(data.length, base + BYTES_PER_ROW);
            for (int i = base; i < end; i++) {
                int col = i - base;
                int hx = xHex + col * charWidth * 3;
                if (i == highlight) {
                    g2.setColor(mark);
                    g2.fillRect(hx - 2, y, charWidth * 2 + 4, rowHeight);
                    g2.fillRect(xAscii + col * charWidth - 1, y, charWidth + 2, rowHeight);
                }
                int v = data[i] & 0xff;
                g2.setColor(v == 0 ? dim : getForeground());
                g2.drawString(HEX[v], hx, baseline);
                g2.drawString(String.valueOf(v >= 32 && v < 127 ? (char) v : '.'),
                        xAscii + col * charWidth, baseline);
            }
        }
    }

    private static final String[] HEX = new String[256];
    static {
        for (int i = 0; i < 256; i++) {
            HEX[i] = String.format("%02X", i);
        }
    }

    // ---- Scrollable: scroll a row at a time, and never stretch horizontally ----

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return rowHeight;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return Math.max(rowHeight, visibleRect.height - rowHeight);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    /** Convenience: this panel inside a scroll pane, sized for a 16-byte row. */
    public JScrollPane inScrollPane() {
        JScrollPane sp = new JScrollPane(this);
        sp.setPreferredSize(new Dimension(getPreferredSize().width + 20, 400));
        return sp;
    }
}
