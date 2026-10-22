package resources;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Frequency of each byte value in the selected range, laid out as the 16x16
 * table of hex values. A byte that never occurs is invisible; the more often one
 * occurs, the larger and more opaque its cell.
 */
public class ByteCloudVisualizer extends Visualizer {

    /** Bytes inspected per repaint. The full range would stall the event thread. */
    private static final int SAMPLE = 10000;

    private final OffsetJump jump;
    /** Geometry of the last painted table, so a click can name the cell under it. */
    private float cellSize, originX, originY;

    public ByteCloudVisualizer(int windowSize, Host cantordust) {
        super(windowSize, cantordust);
        jump = new OffsetJump(cantordust);
        addClickToJump();
    }

    // Special constructor for initialization of plugin
    public ByteCloudVisualizer(int windowSize, Host cantordust, MainInterface mainInterface) {
        super(windowSize, cantordust, mainInterface);
        jump = new OffsetJump(cantordust);
        addClickToJump();
    }

    /**
     * Each cell is one byte value, so a click asks where that value occurs in
     * the selected range and follows it into the listing.
     */
    private void addClickToJump() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getButton() != MouseEvent.BUTTON1 || cellSize <= 0) {
                    return;
                }
                int col = (int) Math.floor((e.getX() - originX + cellSize / 2f) / cellSize);
                int row = (int) Math.floor((e.getY() - originY + cellSize / 2f) / cellSize);
                if(col < 0 || col > 15 || row < 0 || row > 15) {
                    return;
                }
                int value = row * 16 + col;
                byte[] data = cantordust.getMainInterface().getData();
                String msg = jump.toPattern(data,
                        dataMicroSlider.getValue(), dataMicroSlider.getUpperValue(),
                        new int[]{value}, String.format("byte %02X", value));
                cantordust.getMainInterface().setStatus(msg);
            }
        });
    }

    @Override
    public void paintComponent(Graphics g) {
        // The base class already repaints this panel when a slider moves. The
        // previous version added two more ChangeListeners on every single
        // paintComponent call, so the listener lists - and the work done per
        // slider nudge - grew without limit for as long as the panel was open.
        super.paintComponent(g);
        byteCloud((Graphics2D) g);
    }

    private void byteCloud(Graphics2D g) {
        byte[] data = this.cantordust.getMainInterface().getData();
        int low = Math.max(0, dataMicroSlider.getValue());
        int high = Math.min(data.length, dataMicroSlider.getUpperValue());
        if(high <= low) {
            return;
        }

        int[] freq = new int[256];
        int maxFreq = 0;
        // Step across the whole selection rather than reading the first SAMPLE
        // bytes of it: a header-only sample says nothing about the range the
        // sliders actually select.
        int step = Math.max(1, (high - low) / SAMPLE);
        for(int i = low; i < high; i += step) {
            int b = data[i] & 0xff;
            // 00 and FF swamp everything else in most binaries and say little
            // about the content, so they are left out of the scale.
            if(b == 0x00 || b == 0xff) {
                continue;
            }
            freq[b]++;
            if(freq[b] > maxFreq) {
                maxFreq = freq[b];
            }
        }
        if(maxFreq == 0) {
            return;
        }

        // Lay the table out from the panel's own size so it fills a docked window
        // instead of sitting in a fixed 320px square in the corner.
        int w = getWidth();
        int h = getHeight();
        if(w <= 0 || h <= 0) {
            return;
        }
        float cell = Math.min(w, h) / 17f;
        float ox = (w - cell * 16f) / 2f + cell / 2f;
        float oy = (h - cell * 16f) / 2f + cell / 2f;
        this.cellSize = cell;
        this.originX = ox;
        this.originY = oy;
        float maxFont = cell * 0.9f;

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Font base = new Font(Font.MONOSPACED, Font.BOLD, 12);

        for(int row = 0; row < 16; row++) {
            for(int col = 0; col < 16; col++) {
                int value = row * 16 + col;
                if(freq[value] == 0) {
                    continue;
                }
                // Frequencies span orders of magnitude, so scale on a square root
                // rather than linearly: the old code multiplied the font size and
                // the alpha by the same ratio, which left everything but the few
                // most common bytes rounded away to nothing.
                float t = (float) Math.sqrt(freq[value] / (double) maxFreq);
                float fontSize = maxFont * (0.35f + 0.65f * t);
                if(fontSize < 5f) {
                    continue;
                }
                int alpha = Math.max(40, Math.min(255, (int)(255 * (0.25f + 0.75f * t))));
                g.setColor(new Color(0, 255, 0, alpha));
                g.setFont(base.deriveFont(fontSize));

                String s = String.format("%02X", value);
                float tw = g.getFontMetrics().stringWidth(s);
                float ta = g.getFontMetrics().getAscent();
                g.drawString(s, ox + col * cell - tw / 2f,
                        oy + row * cell + ta / 2f);
            }
        }
    }

    public static int getWindowSize() {
        return 512;
    }
}
