package resources;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Digraph plot: every pair of consecutive bytes becomes a point at (b0, b1) in a
 * 256x256 grid, shaded by how often that pair occurs.
 *
 * The grid is the whole domain - there are only 65536 possible pairs - so the
 * image is built at 256x256 and scaled on the way out. Counts live in a flat
 * array rather than a HashMap of boxed tuples: same information, no hashing and
 * no allocation per pair, which matters because the range is re-counted every
 * time a slider moves.
 *
 * Shading goes through DensityShader, the same percentile-normalised log curve
 * the trigram cloud uses. The plot previously ramped linearly at five levels per
 * occurrence and clamped at 255, so any pair occurring fifty times or more came
 * out identical - which is most of a real binary, and exactly the gradient that
 * makes structure legible.
 */
public class TwoTupleVisualizer extends Visualizer {

    /** Blocks the file is pre-counted in, so slider moves merge instead of rescanning. */
    private static final int DIVISIONS = 20;
    private static final int CELLS = 256 * 256;

    private volatile BufferedImage img;
    /** Where img was last drawn, for mapping a click back to a cell. */
    private final Rectangle imgBounds = new Rectangle();

    private int[][] cachedBlocks;
    private int blockSize;

    private volatile int colorMode = DensityShader.MONO;
    private volatile float exposure = 1.0f;

    private final int[] acc = new int[CELLS];
    private final int[] hist = new int[DensityShader.LUT_SIZE];
    private final int[] lut = new int[DensityShader.LUT_SIZE];

    private OffsetJump jump;
    private JPopupMenu popup;

    public TwoTupleVisualizer(int windowSize, Host cantordust) {
        super(windowSize, cantordust);
        init();
    }

    // Special constructor for initialization of plugin
    public TwoTupleVisualizer(int windowSize, Host cantordust, MainInterface mainInterface) {
        super(windowSize, cantordust, mainInterface);
        init();
    }

    private void init() {
        jump = new OffsetJump(cantordust);
        createPopupMenu();
        addChangeListeners();
        addClickToJump();
        rebuildAsync(true);
    }

    public static int getWindowSize() {
        return 512;
    }

    // ------------------------------------------------------------------
    // Counting
    // ------------------------------------------------------------------

    private int[] countRange(byte[] data, int low, int high) {
        int[] counts = new int[CELLS];
        low = Math.max(0, low);
        high = Math.min(data.length, high);
        for (int i = low; i < high - 1; i++) {
            counts[((data[i] & 0xff) << 8) | (data[i + 1] & 0xff)]++;
        }
        return counts;
    }

    private void initializeCaches() {
        byte[] data = cantordust.getMainInterface().getData();
        int[][] blocks = new int[DIVISIONS][];
        int size = Math.max(1, data.length / DIVISIONS);
        for (int d = 0; d < DIVISIONS - 1; d++) {
            blocks[d] = countRange(data, d * size, (d + 1) * size);
        }
        blocks[DIVISIONS - 1] = countRange(data, (DIVISIONS - 1) * size, data.length);
        synchronized (this) {
            cachedBlocks = blocks;
            blockSize = size;
        }
    }

    public void constructImageAsync() {
        rebuildAsync(false);
    }

    private void rebuildAsync(final boolean withCaches) {
        new Thread(() -> {
            try {
                if (withCaches || cachedBlocks == null) {
                    initializeCaches();
                }
                buildImage();
            } catch (RuntimeException e) {
                cantordust.cdprint("2-tuple redraw failed: " + e + "\n");
            }
        }, "cantordust-2tuple").start();
    }

    /**
     * Counts the selected range by merging whole pre-counted blocks and only
     * scanning the partial block at each end.
     */
    private void buildImage() {
        byte[] data = cantordust.getMainInterface().getData();
        int low = Math.max(dataMacroSlider.getValue(), dataMicroSlider.getValue());
        int high = Math.min(dataMacroSlider.getUpperValue(), dataMicroSlider.getUpperValue());
        low = Math.max(0, low);
        high = Math.min(data.length, high);

        int[][] blocks;
        int size;
        synchronized (this) {
            blocks = cachedBlocks;
            size = blockSize;
        }
        if (blocks == null || size <= 0 || high - low < 2) {
            return;
        }

        Arrays.fill(acc, 0);
        int firstWhole = ceilTo(low, size);
        int lastWhole = (high / size) * size;
        if (firstWhole > lastWhole) {
            // The selection sits inside a single block; count it directly.
            addInto(acc, countRange(data, low, high));
        } else {
            if (low < firstWhole) {
                addInto(acc, countRange(data, low, firstWhole + 1));
            }
            for (int b = firstWhole / size; b < lastWhole / size && b < blocks.length; b++) {
                addInto(acc, blocks[b]);
            }
            if (lastWhole < high) {
                addInto(acc, countRange(data, lastWhole, high));
            }
        }

        BufferedImage out = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        int[] px = ((DataBufferInt) out.getRaster().getDataBuffer()).getData();
        if (DensityShader.buildLut(acc, exposure, hist, lut) == 0) {
            Arrays.fill(px, 0);
        } else {
            int mode = colorMode;
            for (int cell = 0; cell < CELLS; cell++) {
                int a = acc[cell];
                if (a == 0) {
                    px[cell] = 0;
                    continue;
                }
                // Row is the first byte of the pair, column the second; the key
                // carries both so the value modes can colour by them.
                int key = ((cell >> 8) << 16) | ((cell & 0xff) << 8);
                px[cell] = DensityShader.shade(mode, DensityShader.intensity(lut, a), key);
            }
        }
        img = out;
        repaint();
    }

    private static void addInto(int[] target, int[] source) {
        for (int i = 0; i < target.length; i++) {
            target[i] += source[i];
        }
    }

    private static int ceilTo(int x, int size) {
        return ((x + size - 1) / size) * size;
    }

    // ------------------------------------------------------------------
    // Painting
    // ------------------------------------------------------------------

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        BufferedImage current = img;
        if (current == null) {
            return;
        }
        int pw = getWidth(), ph = getHeight();
        if (pw <= 0 || ph <= 0) {
            return;
        }
        // The digraph domain is square, so fit rather than stretch. Nearest
        // neighbour keeps single-occurrence cells as crisp points instead of
        // smearing them into the background.
        int side = Math.min(pw, ph);
        int x = (pw - side) / 2;
        int y = (ph - side) / 2;
        imgBounds.setBounds(x, y, side, side);
        Graphics2D g2 = (Graphics2D) g;
        Object prev = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.drawImage(current, x, y, side, side, null);
        if (prev != null) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, prev);
        }
    }

    // ------------------------------------------------------------------
    // Interaction
    // ------------------------------------------------------------------

    private void addChangeListeners() {
        dataMacroSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (!dataMacroSlider.getValueIsAdjusting()) {
                    constructImageAsync();
                }
            }
        });
        dataMicroSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (!dataMicroSlider.getValueIsAdjusting() && !dataMacroSlider.getValueIsAdjusting()) {
                    constructImageAsync();
                }
            }
        });
        if (dataRangeSlider != null) {
            // The cached blocks describe the 1MB working window. Scrolling it
            // replaces the bytes underneath, so they have to be counted again.
            dataRangeSlider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    if (!dataRangeSlider.getValueIsAdjusting()) {
                        rebuildAsync(true);
                    }
                }
            });
        }
    }

    /**
     * A cell names an exact byte pair, so a click can find it in the file - the
     * same move the trigram cloud makes.
     */
    private void addClickToJump() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1) {
                    return;
                }
                Rectangle b = imgBounds;
                if (b.width <= 0 || !b.contains(e.getX(), e.getY())) {
                    return;
                }
                int col = (e.getX() - b.x) * 256 / b.width;
                int row = (e.getY() - b.y) * 256 / b.height;
                if (col < 0 || col > 255 || row < 0 || row > 255) {
                    return;
                }
                byte[] data = cantordust.getMainInterface().getData();
                String msg = jump.toPattern(data,
                        dataMicroSlider.getValue(), dataMicroSlider.getUpperValue(),
                        new int[]{row, col}, String.format("%02X %02X", row, col));
                cantordust.getMainInterface().setStatus(msg);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
                    popup.show(TwoTupleVisualizer.this, e.getX(), e.getY());
                }
            }
        });
    }

    private void createPopupMenu() {
        popup = new JPopupMenu("2-tuple");

        JMenu colorMenu = new JMenu("Color");
        ButtonGroup colors = new ButtonGroup();
        addColorItem(colorMenu, colors, "Mono", DensityShader.MONO);
        addColorItem(colorMenu, colors, "Green", DensityShader.GREEN);
        addColorItem(colorMenu, colors, "Heat", DensityShader.HEAT);
        addColorItem(colorMenu, colors, "Ice", DensityShader.ICE);
        addColorItem(colorMenu, colors, "Byte value", DensityShader.TRIGRAM);
        addColorItem(colorMenu, colors, "Byte value (literal)", DensityShader.TRIGRAM_LITERAL);
        popup.add(colorMenu);

        popup.addSeparator();

        JMenuItem brighter = new JMenuItem("Brighter");
        brighter.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                exposure = Math.min(64f, exposure * 1.8f);
                constructImageAsync();
            }
        });
        popup.add(brighter);

        JMenuItem dimmer = new JMenuItem("Dimmer");
        dimmer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                exposure = Math.max(0.05f, exposure / 1.8f);
                constructImageAsync();
            }
        });
        popup.add(dimmer);

        add(popup);
    }

    private void addColorItem(JMenu menu, ButtonGroup group, String label, final int value) {
        JRadioButtonMenuItem item = new JRadioButtonMenuItem(label, colorMode == value);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                colorMode = value;
                constructImageAsync();
            }
        });
        group.add(item);
        menu.add(item);
    }
}
