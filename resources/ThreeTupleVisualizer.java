package resources;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.KeyStroke;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Trigram point cloud: every run of three consecutive bytes becomes a point at
 * (b0, b1, b2) inside a 256x256x256 cube, which is the three dimensional form of
 * what TwoTupleVisualizer already draws in two.
 *
 * A Ghidra script cannot pull in a GPU binding - there is no dependency
 * resolution here, and shipping JOGL natives through the OSGi bundle would trade
 * a git-clone install for a fragile one. None of that is needed. Trigram clouds
 * drop the parts of 3D that are actually expensive:
 *
 *   - Points are additively blended, so the result is order independent. No
 *     depth buffer, no sorting.
 *   - The position space is bounded at 256^3, so the file is histogrammed once
 *     and only occupied voxels are ever drawn. Tens of millions of trigrams
 *     collapse to a few million distinct points.
 *   - There are no triangles, textures or lights. A point costs nine multiplies,
 *     a divide and an array increment.
 *
 * What is left is a software rasteriser that keeps up with a mouse drag.
 */
public class ThreeTupleVisualizer extends Visualizer {

    /** Above this many occupied voxels the volume folds down to 128^3. */
    private static final int MAX_POINTS = 2_500_000;

    /** Points to aim for while the camera is moving; the rest are skipped. */
    private static final int DRAG_BUDGET = 400_000;

    // Shapes the cube can be poured into, as in BinaryVis.
    private static final int SHAPE_CUBE = 0;
    private static final int SHAPE_SPHERE = 1;
    private static final int SHAPE_CYLINDER = 2;

    // Colour modes and the tone curve are shared with the 2-tuple plot; see
    // DensityShader for why the curve is shaped the way it is.
    private static final int COLOR_TRIGRAM = DensityShader.TRIGRAM;
    private static final int COLOR_TRIGRAM_LITERAL = DensityShader.TRIGRAM_LITERAL;
    private static final int COLOR_GREEN = DensityShader.GREEN;
    private static final int COLOR_HEAT = DensityShader.HEAT;
    private static final int COLOR_ICE = DensityShader.ICE;
    private static final int COLOR_MONO = DensityShader.MONO;

    /**
     * One immutable snapshot of the histogram. The build thread publishes a new
     * one and never touches it again; the render thread owns the position arrays
     * from then on. Nothing between them needs a lock.
     */
    private static final class Cloud {
        final int n;
        final int[] key;      // packed trigram, b0<<16 | b1<<8 | b2
        final int[] weight;   // occurrences of that trigram in range
        final float[] px, py, pz;
        /** 0 for a full 256^3 build, 1 when folded to 128^3. */
        final int fold;
        /** Shape the positions currently hold; -1 until the renderer fills them. */
        int shape = -1;

        Cloud(int n, int fold) {
            this.n = n;
            this.fold = fold;
            key = new int[n];
            weight = new int[n];
            px = new float[n];
            py = new float[n];
            pz = new float[n];
        }
    }

    /**
     * A finished frame, published with the buffer recording which trigram
     * dominates each pixel so a click can be resolved back to file offsets.
     */
    private static final class Frame {
        final BufferedImage img;
        final int[] pick;     // key + 1, so 0 means "nothing here"
        final int w, h;

        Frame(BufferedImage img, int[] pick, int w, int h) {
            this.img = img;
            this.pick = pick;
            this.w = w;
            this.h = h;
        }
    }

    private volatile Cloud cloud;
    private volatile Frame frame;

    // Camera and appearance. Written by the EDT, read by the render thread.
    private volatile float yaw = 0.6f;
    private volatile float pitch = -0.35f;
    private volatile float dist = 3.4f;
    private volatile float exposure = 1.0f;
    private volatile boolean moving;
    private volatile int shape = SHAPE_CUBE;
    private volatile int colorMode = COLOR_TRIGRAM;
    /** Splat size in pixels. Larger points make a sparse cloud far more legible. */
    private volatile int pointSize = 1;
    /** Until the user picks a size, it follows how sparse the cloud turned out. */
    private volatile boolean pointSizeAuto = true;
    /** Grow points as they come towards the camera, for a stronger sense of depth. */
    private volatile boolean depthScale;
    /** The reference cube helps orientation, but hides the cloud's own shape. */
    private volatile boolean showCube = true;
    private javax.swing.Timer spinTimer;
    private volatile int viewW = 512, viewH = 512;
    private volatile String status = "";

    // Render scratch, reused across frames. Only the render thread touches it.
    private int[] acc;
    private int[] pickWeight;
    private final BufferedImage[] images = new BufferedImage[2];
    private final int[][] pickBuffers = new int[2][];
    private int bufIndex;
    private int bufW, bufH;
    private int[] toneLut;
    private int[] toneHist;

    private final Object renderLock = new Object();
    private boolean renderDirty;
    private final Object buildLock = new Object();
    private boolean buildDirty;

    private JPopupMenu popup;
    private int dragX, dragY;

    public ThreeTupleVisualizer(int windowSize, GhidraSrc cantordust) {
        super(windowSize, cantordust);
        init();
    }

    // Special constructor for initialization of plugin
    public ThreeTupleVisualizer(int windowSize, GhidraSrc cantordust, MainInterface mainInterface) {
        super(windowSize, cantordust, mainInterface);
        init();
    }

    private void init() {
        startThreads();
        addChangeListeners();
        addMouseControls();
        createPopupMenu();
        addKeyBindings();
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                viewW = Math.max(1, getWidth());
                viewH = Math.max(1, getHeight());
                requestRender();
            }
        });
        requestBuild();
    }

    public static int getWindowSize() {
        return 512;
    }

    // ------------------------------------------------------------------
    // Background workers
    // ------------------------------------------------------------------

    /**
     * Histogramming and rendering each get a thread that parks until asked for
     * work, so a burst of slider or drag events collapses into one pass rather
     * than queueing behind itself. Both are daemons: neither should hold a
     * Ghidra session open once the tool is done with it.
     */
    private void startThreads() {
        Thread builder = new Thread(new Runnable() {
            public void run() {
                while (awaitWork(buildLock, true)) {
                    try {
                        rebuild();
                    } catch (RuntimeException e) {
                        cantordust.cdprint("3-tuple rebuild failed: " + e + "\n");
                    } catch (OutOfMemoryError e) {
                        cloud = null;
                        status = "not enough heap for a trigram volume";
                        requestRender();
                    }
                }
            }
        }, "cantordust-3tuple-build");
        builder.setDaemon(true);
        builder.setPriority(Thread.NORM_PRIORITY - 1);
        builder.start();

        Thread renderer = new Thread(new Runnable() {
            public void run() {
                while (awaitWork(renderLock, false)) {
                    try {
                        renderFrame();
                    } catch (RuntimeException e) {
                        cantordust.cdprint("3-tuple render failed: " + e + "\n");
                    }
                }
            }
        }, "cantordust-3tuple-render");
        renderer.setDaemon(true);
        renderer.start();
    }

    private boolean awaitWork(Object lock, boolean build) {
        synchronized (lock) {
            while (build ? !buildDirty : !renderDirty) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            if (build) {
                buildDirty = false;
            } else {
                renderDirty = false;
            }
            return true;
        }
    }

    private void requestBuild() {
        synchronized (buildLock) {
            buildDirty = true;
            buildLock.notifyAll();
        }
    }

    private void requestRender() {
        synchronized (renderLock) {
            renderDirty = true;
            renderLock.notifyAll();
        }
    }

    // ------------------------------------------------------------------
    // Histogram
    // ------------------------------------------------------------------

    private void rebuild() {
        byte[] data = cantordust.getMainInterface().getData();
        // Read only: this runs on the build thread, where writing to a slider
        // would both touch Swing off the event thread and re-fire the listener
        // that asked for this rebuild. Intersecting the two ranges keeps the
        // result sane whatever state the sliders are mid-sync in.
        int low = Math.max(dataMacroSlider.getValue(), dataMicroSlider.getValue());
        int high = Math.min(dataMacroSlider.getUpperValue(), dataMicroSlider.getUpperValue());
        low = Math.max(0, low);
        high = Math.min(data.length, high);
        if (high - low < 3) {
            cloud = null;
            status = "range too small for a trigram";
            requestRender();
            return;
        }

        // 16M shorts is 33MB, held for the length of this method only. Counts
        // saturate rather than wrap: a trigram seen 65535 times is already as
        // bright as the tone curve can render it.
        short[] vol = new short[1 << 24];
        int occupied = 0;
        int b0 = data[low] & 0xff;
        int b1 = data[low + 1] & 0xff;
        for (int i = low + 2; i < high; i++) {
            int b2 = data[i] & 0xff;
            int k = (b0 << 16) | (b1 << 8) | b2;
            short c = vol[k];
            if (c == 0) {
                occupied++;
                vol[k] = 1;
            } else if (c != -1) {
                vol[k] = (short) (c + 1);
            }
            b0 = b1;
            b1 = b2;
        }

        Cloud built = (occupied > MAX_POINTS) ? compactFolded(vol) : compactFull(vol, occupied);
        if (pointSizeAuto) {
            // A small binary puts a few thousand points on a quarter-million
            // pixel canvas. One-pixel splats leave that looking almost empty,
            // so start sparse clouds at a larger point.
            pointSize = built.n < 100_000 ? 2 : 1;
        }
        cloud = built;
        status = "";
        requestRender();
    }

    /** Straight compaction at full 256^3 resolution. */
    private Cloud compactFull(short[] vol, int occupied) {
        Cloud c = new Cloud(occupied, 0);
        int n = 0;
        for (int k = 0; k < vol.length && n < occupied; k++) {
            int w = vol[k] & 0xffff;
            if (w == 0) {
                continue;
            }
            c.key[n] = k;
            c.weight[n] = w;
            n++;
        }
        return c;
    }

    /**
     * Folds the volume to 128^3 when a file is dense enough to blow past the
     * point budget. 128^3 is 2.1M cells, below MAX_POINTS, so one fold always
     * suffices and this cannot loop.
     */
    private Cloud compactFolded(short[] vol) {
        int[] small = new int[1 << 21];
        int occupied = 0;
        for (int k = 0; k < vol.length; k++) {
            int w = vol[k] & 0xffff;
            if (w == 0) {
                continue;
            }
            int j = (((k >> 17) & 0x7f) << 14) | (((k >> 9) & 0x7f) << 7) | ((k >> 1) & 0x7f);
            if (small[j] == 0) {
                occupied++;
            }
            small[j] += w;
        }
        Cloud c = new Cloud(occupied, 1);
        int n = 0;
        for (int j = 0; j < small.length && n < occupied; j++) {
            int w = small[j];
            if (w == 0) {
                continue;
            }
            // Re-expand onto the 0..255 scale so colour, geometry and picking
            // all stay in the same units as a full resolution cloud.
            int x = ((j >> 14) & 0x7f) << 1;
            int y = ((j >> 7) & 0x7f) << 1;
            int z = (j & 0x7f) << 1;
            c.key[n] = (x << 16) | (y << 8) | z;
            c.weight[n] = w;
            n++;
        }
        return c;
    }

    // ------------------------------------------------------------------
    // Shapes
    // ------------------------------------------------------------------

    /**
     * Positions are derived from the packed key rather than stored twice, so a
     * shape change costs one pass and no extra memory. Called only from the
     * render thread, which owns the position arrays once a cloud is published.
     */
    private static void applyShape(Cloud c, int shape) {
        if (c.shape == shape) {
            return;
        }
        for (int i = 0; i < c.n; i++) {
            int k = c.key[i];
            float x = (((k >> 16) & 0xff) / 127.5f) - 1f;
            float y = (((k >> 8) & 0xff) / 127.5f) - 1f;
            float z = ((k & 0xff) / 127.5f) - 1f;
            if (shape == SHAPE_SPHERE) {
                float xx = x * x, yy = y * y, zz = z * z;
                float sx = x * (float) Math.sqrt(1f - yy / 2f - zz / 2f + yy * zz / 3f);
                float sy = y * (float) Math.sqrt(1f - zz / 2f - xx / 2f + zz * xx / 3f);
                float sz = z * (float) Math.sqrt(1f - xx / 2f - yy / 2f + xx * yy / 3f);
                x = sx; y = sy; z = sz;
            } else if (shape == SHAPE_CYLINDER) {
                // Elliptical grid mapping of the x/z square onto a disc, height
                // left alone, so the middle byte still reads as an axis.
                float xx = x * x, zz = z * z;
                float sx = x * (float) Math.sqrt(1f - zz / 2f);
                float sz = z * (float) Math.sqrt(1f - xx / 2f);
                x = sx; z = sz;
            }
            c.px[i] = x;
            c.py[i] = y;
            c.pz[i] = z;
        }
        c.shape = shape;
    }

    // ------------------------------------------------------------------
    // Rasteriser
    // ------------------------------------------------------------------

    /**
     * Buffers are kept and cleared rather than reallocated: at 30 frames a
     * second a fresh image plus scratch would hand the collector well over
     * 100MB of garbage per second for no benefit. The published image and pick
     * buffer alternate between two slots so the frame the UI is holding is not
     * the one being overwritten.
     */
    private void ensureBuffers(int w, int h) {
        if (bufW == w && bufH == h && acc != null) {
            return;
        }
        bufW = w;
        bufH = h;
        acc = new int[w * h];
        pickWeight = new int[w * h];
        for (int i = 0; i < 2; i++) {
            images[i] = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            pickBuffers[i] = new int[w * h];
        }
    }

    private void renderFrame() {
        int w = viewW, h = viewH;
        if (w < 2 || h < 2) {
            return;
        }
        ensureBuffers(w, h);

        bufIndex ^= 1;
        BufferedImage img = images[bufIndex];
        int[] pick = pickBuffers[bufIndex];
        int[] out = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
        Arrays.fill(acc, 0);
        Arrays.fill(pickWeight, 0);
        Arrays.fill(pick, 0);

        Cloud c = cloud;
        int maxAcc = 0;
        if (c != null && c.n > 0) {
            applyShape(c, shape);

            int stride = (moving && c.n > DRAG_BUDGET) ? (c.n + DRAG_BUDGET - 1) / DRAG_BUDGET : 1;
            float cy = (float) Math.cos(yaw), sy = (float) Math.sin(yaw);
            float cp = (float) Math.cos(pitch), sp = (float) Math.sin(pitch);
            float d0 = dist;
            float scale = 1.6f * Math.min(w, h) * 0.5f;
            float ox = w * 0.5f, oy = h * 0.5f;
            float[] px = c.px, py = c.py, pz = c.pz;
            int[] key = c.key, wt = c.weight;
            int basePs = pointSize;
            boolean scaleWithDepth = depthScale;

            for (int i = 0; i < c.n; i += stride) {
                float x = px[i], y = py[i], z = pz[i];
                float ax = x * cy + z * sy;
                float az = z * cy - x * sy;
                float ay = y * cp - az * sp;
                float bz = y * sp + az * cp;
                float dd = d0 + bz;
                if (dd < 0.05f) {
                    continue;
                }
                float s = scale / dd;
                int sx = (int) (ox + ax * s);
                int sy2 = (int) (oy - ay * s);
                if (sx < 0 || sx >= w || sy2 < 0 || sy2 >= h) {
                    continue;
                }
                int ps = basePs;
                if (scaleWithDepth) {
                    // Nearer points get bigger. dd runs roughly dist-1 .. dist+1.
                    ps = (int)(basePs * (d0 / dd));
                    if (ps < 1) {
                        ps = 1;
                    } else if (ps > 6) {
                        ps = 6;
                    }
                }
                int weight = wt[i];
                int keyPlus = key[i] + 1;
                if (ps == 1) {
                    int idx = sy2 * w + sx;
                    int a = acc[idx] + weight;
                    acc[idx] = a;
                    if (a > maxAcc) {
                        maxAcc = a;
                    }
                    // The heaviest contributor owns the pixel: it drives colour
                    // in trigram mode and is what a click resolves back to.
                    if (weight > pickWeight[idx]) {
                        pickWeight[idx] = weight;
                        pick[idx] = keyPlus;
                    }
                } else {
                    int half = ps >> 1;
                    int x0 = sx - half, y0 = sy2 - half;
                    int x1 = x0 + ps, y1 = y0 + ps;
                    if (x0 < 0) x0 = 0;
                    if (y0 < 0) y0 = 0;
                    if (x1 > w) x1 = w;
                    if (y1 > h) y1 = h;
                    for (int py2 = y0; py2 < y1; py2++) {
                        int row = py2 * w;
                        for (int px2 = x0; px2 < x1; px2++) {
                            int idx = row + px2;
                            int a = acc[idx] + weight;
                            acc[idx] = a;
                            if (a > maxAcc) {
                                maxAcc = a;
                            }
                            if (weight > pickWeight[idx]) {
                                pickWeight[idx] = weight;
                                pick[idx] = keyPlus;
                            }
                        }
                    }
                }
            }
        }

        toneMap(acc, pick, out, maxAcc);
        frame = new Frame(img, pick, w, h);
        repaint();
    }

    private void toneMap(int[] acc, int[] pick, int[] out, int maxAcc) {
        if (maxAcc <= 0) {
            Arrays.fill(out, 0);
            return;
        }
        if (toneHist == null) {
            toneHist = new int[DensityShader.LUT_SIZE];
            toneLut = new int[DensityShader.LUT_SIZE];
        }
        if (DensityShader.buildLut(acc, exposure, toneHist, toneLut) == 0) {
            Arrays.fill(out, 0);
            return;
        }
        int[] lut = toneLut;
        int mode = colorMode;
        for (int i = 0; i < acc.length; i++) {
            int a = acc[i];
            if (a == 0) {
                out[i] = 0;
                continue;
            }
            out[i] = DensityShader.shade(mode, DensityShader.intensity(lut, a), pick[i] - 1);
        }
    }

    // ------------------------------------------------------------------
    // Painting
    // ------------------------------------------------------------------

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        int w = getWidth(), h = getHeight();

        Frame f = frame;
        if (f != null) {
            // Scaled to the component, so a resize shows a stretched frame for
            // one beat instead of nothing at all.
            g2.drawImage(f.img, 0, 0, w, h, this);
        }

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (showCube) {
            drawWireframe(g2, w, h);
        }
        drawHud(g2, w, h);
    }

    /**
     * The reference cube is drawn from the live camera rather than baked into
     * the frame, so orientation keeps up with the mouse even when a large cloud
     * takes a moment to redraw.
     */
    private void drawWireframe(Graphics2D g2, int w, int h) {
        float cy = (float) Math.cos(yaw), sy = (float) Math.sin(yaw);
        float cp = (float) Math.cos(pitch), sp = (float) Math.sin(pitch);
        float scale = 1.6f * Math.min(w, h) * 0.5f;
        float ox = w * 0.5f, oy = h * 0.5f;

        int[] cx = new int[8], cyv = new int[8];
        boolean[] ok = new boolean[8];
        for (int i = 0; i < 8; i++) {
            float x = ((i & 1) == 0) ? -1f : 1f;
            float y = ((i & 2) == 0) ? -1f : 1f;
            float z = ((i & 4) == 0) ? -1f : 1f;
            float ax = x * cy + z * sy;
            float az = z * cy - x * sy;
            float ay = y * cp - az * sp;
            float bz = y * sp + az * cp;
            float dd = dist + bz;
            if (dd < 0.05f) {
                continue;
            }
            float s = scale / dd;
            cx[i] = (int) (ox + ax * s);
            cyv[i] = (int) (oy - ay * s);
            ok[i] = true;
        }

        int[][] edges = {
            {0, 1}, {1, 3}, {3, 2}, {2, 0},
            {4, 5}, {5, 7}, {7, 6}, {6, 4},
            {0, 4}, {1, 5}, {2, 6}, {3, 7}
        };
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(new Color(90, 100, 110, 130));
        for (int[] e : edges) {
            if (ok[e[0]] && ok[e[1]]) {
                g2.drawLine(cx[e[0]], cyv[e[0]], cx[e[1]], cyv[e[1]]);
            }
        }

        // Label the axes off the origin corner so it stays clear which byte of
        // the trigram is which.
        g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        g2.setColor(new Color(140, 150, 160, 200));
        if (ok[0] && ok[1]) drawAxisLabel(g2, cx[0], cyv[0], cx[1], cyv[1], "b0");
        if (ok[0] && ok[2]) drawAxisLabel(g2, cx[0], cyv[0], cx[2], cyv[2], "b1");
        if (ok[0] && ok[4]) drawAxisLabel(g2, cx[0], cyv[0], cx[4], cyv[4], "b2");
    }

    private void drawAxisLabel(Graphics2D g2, int x0, int y0, int x1, int y1, String s) {
        g2.drawString(s, x1 + (x1 - x0) / 12, y1 + (y1 - y0) / 12);
    }

    private static String shapeName(int shape) {
        return shape == SHAPE_CUBE ? "cube" : (shape == SHAPE_SPHERE ? "sphere" : "cylinder");
    }

    private static String colorName(int mode) {
        return mode == DensityShader.TRIGRAM ? "trigram" : DensityShader.name(mode);
    }

    private void drawHud(Graphics2D g2, int w, int h) {
        Cloud c = cloud;
        g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        g2.setColor(new Color(170, 180, 190, 220));

        String left;
        if (c == null) {
            left = "building…";
        } else {
            left = c.n + " pts"
                    + (c.fold > 0 ? " (128³)" : "")
                    + "   " + shapeName(shape)
                    + "   " + colorName(colorMode)
                    + "   " + pointSize + "px"
                    + (Math.abs(exposure - 1f) > 0.01f
                        ? String.format("   %.1fx", exposure) : "")
                    + (spinTimer != null ? "   spin" : "");
        }
        g2.drawString(left, 8, h - 10);

        String s = status;
        if (s != null && !s.isEmpty()) {
            g2.setColor(new Color(225, 232, 240, 235));
            g2.drawString(s, 8, 18);
        }
    }

    // ------------------------------------------------------------------
    // Interaction
    // ------------------------------------------------------------------

    private void addChangeListeners() {
        // Read only, both here and in rebuild(). MainInterface owns nesting the
        // micro range inside the macro one and does it as a single atomic model
        // update; a visualizer that also moved the bounds would fight that.
        dataMacroSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (!dataMacroSlider.getValueIsAdjusting()) {
                    requestBuild();
                }
            }
        });
        dataMicroSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (!dataMicroSlider.getValueIsAdjusting() && !dataMacroSlider.getValueIsAdjusting()) {
                    requestBuild();
                }
            }
        });
        if (dataRangeSlider != null) {
            // Files over 25MB are viewed through a 1MB window that this slider
            // scrolls. Moving it replaces the bytes underneath, so the histogram
            // has to be built again - a repaint would just redraw a stale cloud.
            dataRangeSlider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    if (!dataRangeSlider.getValueIsAdjusting()) {
                        requestBuild();
                    }
                }
            });
        }
    }

    private void addMouseControls() {
        MouseAdapter m = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                dragX = e.getX();
                dragY = e.getY();
                if (e.isPopupTrigger()) {
                    popup.show(ThreeTupleVisualizer.this, e.getX(), e.getY());
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - dragX;
                int dy = e.getY() - dragY;
                dragX = e.getX();
                dragY = e.getY();
                yaw += dx * 0.008f;
                // Stop short of straight up or down, where yaw stops meaning
                // anything and the cloud looks like it is spinning on the spot.
                float p = pitch + dy * 0.008f;
                float lim = 1.53f;
                pitch = p < -lim ? -lim : (p > lim ? lim : p);
                moving = true;
                requestRender();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                boolean wasMoving = moving;
                moving = false;
                if (e.isPopupTrigger()) {
                    popup.show(ThreeTupleVisualizer.this, e.getX(), e.getY());
                } else if (wasMoving) {
                    // Redraw at full point count now the camera has settled.
                    requestRender();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && !e.isPopupTrigger()) {
                    pick(e.getX(), e.getY());
                }
            }
        };
        addMouseListener(m);
        addMouseMotionListener(m);
        addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                float d = dist * (float) Math.pow(1.12, e.getPreciseWheelRotation());
                dist = d < 1.35f ? 1.35f : (d > 12f ? 12f : d);
                requestRender();
            }
        });
    }

    /**
     * Resolves a click back to the bytes that produced it. This is the reason to
     * have the cloud inside Ghidra rather than in a browser: the point under the
     * cursor knows its trigram, and the trigram can be found in the file.
     */
    private void pick(int mx, int my) {
        Frame f = frame;
        if (f == null) {
            return;
        }
        int w = getWidth(), h = getHeight();
        if (w < 1 || h < 1) {
            return;
        }
        int fx = mx * f.w / w;
        int fy = my * f.h / h;
        if (fx < 0 || fx >= f.w || fy < 0 || fy >= f.h) {
            return;
        }
        int stored = f.pick[fy * f.w + fx];
        if (stored == 0) {
            status = "";
            repaint();
            return;
        }
        final int key = stored - 1;
        final int fold = (cloud == null) ? 0 : cloud.fold;
        Thread t = new Thread(new Runnable() {
            public void run() {
                searchTrigram(key, fold);
            }
        }, "cantordust-3tuple-pick");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Mirror the in-canvas readout to the shared status line, so a click reports
     * in the same place every other visualization does.
     */
    private void publishStatus() {
        MainInterface mi = cantordust.getMainInterface();
        if(mi != null){
            mi.setStatus(status);
        }
    }

    private void searchTrigram(int key, int fold) {
        byte[] data = cantordust.getMainInterface().getData();
        int low = Math.max(0, dataMicroSlider.getValue());
        int high = Math.min(data.length, dataMicroSlider.getUpperValue());
        // A folded cloud dropped the low bit of each axis, so a point stands for
        // a 2x2x2 block of trigrams rather than one.
        int mask = (fold > 0) ? 0xfefefe : 0xffffff;
        int want = key & mask;

        int count = 0;
        long first = -1;
        if (high - low >= 3) {
            int b0 = data[low] & 0xff;
            int b1 = data[low + 1] & 0xff;
            for (int i = low + 2; i < high; i++) {
                int b2 = data[i] & 0xff;
                if ((((b0 << 16) | (b1 << 8) | b2) & mask) == want) {
                    if (first < 0) {
                        first = i - 2;
                    }
                    count++;
                }
                b0 = b1;
                b1 = b2;
            }
        }

        String hex = (fold > 0)
                ? String.format("%02X %02X %02X +", (key >> 16) & 0xff, (key >> 8) & 0xff, key & 0xff)
                : String.format("%02X %02X %02X", (key >> 16) & 0xff, (key >> 8) & 0xff, key & 0xff);
        if (first < 0) {
            status = hex + "   no match in range";
            publishStatus();
        } else {
            boolean went = false;
            try {
                went = cantordust.gotoFileAddress(first);
            } catch (RuntimeException e) {
                cantordust.cdprint("goto failed for offset " + first + ": " + e + "\n");
            }
            status = String.format("%s   %d occurrence%s   first @ 0x%X%s",
                    hex, count, count == 1 ? "" : "s", first, went ? "" : "   (not mapped)");
            publishStatus();
        }
        repaint();
    }

    // ------------------------------------------------------------------
    // Menu
    // ------------------------------------------------------------------

    private void createPopupMenu() {
        popup = new JPopupMenu("3-tuple");

        JMenu shapeMenu = new JMenu("Shape");
        ButtonGroup shapes = new ButtonGroup();
        addShapeItem(shapeMenu, shapes, "Cube", SHAPE_CUBE);
        addShapeItem(shapeMenu, shapes, "Sphere", SHAPE_SPHERE);
        addShapeItem(shapeMenu, shapes, "Cylinder", SHAPE_CYLINDER);
        popup.add(shapeMenu);

        JMenu colorMenu = new JMenu("Color");
        ButtonGroup colors = new ButtonGroup();
        addColorItem(colorMenu, colors, "Trigram", COLOR_TRIGRAM);
        addColorItem(colorMenu, colors, "Trigram (literal)", COLOR_TRIGRAM_LITERAL);
        addColorItem(colorMenu, colors, "Green", COLOR_GREEN);
        addColorItem(colorMenu, colors, "Heat", COLOR_HEAT);
        addColorItem(colorMenu, colors, "Ice", COLOR_ICE);
        addColorItem(colorMenu, colors, "Mono", COLOR_MONO);
        popup.add(colorMenu);

        JMenu sizeMenu = new JMenu("Point size");
        ButtonGroup sizes = new ButtonGroup();
        for(int i = 1; i <= 4; i++){
            addPointSizeItem(sizeMenu, sizes, i);
        }
        popup.add(sizeMenu);

        JCheckBoxMenuItem cube = new JCheckBoxMenuItem("Reference cube", showCube);
        cube.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showCube = ((JCheckBoxMenuItem) e.getSource()).isSelected();
                repaint();
            }
        });
        popup.add(cube);

        JCheckBoxMenuItem depth = new JCheckBoxMenuItem("Scale points by depth", depthScale);
        depth.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                depthScale = ((JCheckBoxMenuItem) e.getSource()).isSelected();
                requestRender();
            }
        });
        popup.add(depth);

        JCheckBoxMenuItem spin = new JCheckBoxMenuItem("Spin", false);
        spin.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setSpinning(((JCheckBoxMenuItem) e.getSource()).isSelected());
            }
        });
        popup.add(spin);

        popup.addSeparator();

        JMenuItem brighter = new JMenuItem("Brighter");
        brighter.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                adjustExposure(1.8f);
            }
        });
        popup.add(brighter);

        JMenuItem dimmer = new JMenuItem("Dimmer");
        dimmer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                adjustExposure(1f / 1.8f);
            }
        });
        popup.add(dimmer);

        JMenuItem reset = new JMenuItem("Reset view");
        reset.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                resetView();
            }
        });
        popup.add(reset);

        add(popup);
    }

    private void adjustExposure(float factor) {
        exposure = Math.max(0.05f, Math.min(64f, exposure * factor));
        requestRender();
    }

    private void resetView() {
        yaw = 0.6f;
        pitch = -0.35f;
        dist = 3.4f;
        exposure = 1.0f;
        status = "";
        requestRender();
    }

    /**
     * Slow turntable rotation. Driven by a Swing timer so the camera is only
     * ever written from the event thread, as the mouse handlers do.
     */
    private void setSpinning(boolean on) {
        if(spinTimer != null){
            spinTimer.stop();
            spinTimer = null;
        }
        if(on){
            spinTimer = new javax.swing.Timer(33, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if(!isShowing()){
                        // Nothing is looking at it; stop burning frames.
                        setSpinning(false);
                        return;
                    }
                    yaw += 0.006f;
                    moving = true;
                    requestRender();
                }
            });
            spinTimer.start();
        } else {
            moving = false;
            requestRender();
        }
    }

    /**
     * Keyboard shortcuts for the controls worth reaching for repeatedly. Bound
     * WHEN_FOCUSED and paired with a click-to-focus, so they never swallow keys
     * meant for the rest of the Ghidra tool.
     */
    private void addKeyBindings() {
        setFocusable(true);
        InputMap in = getInputMap(WHEN_FOCUSED);
        ActionMap act = getActionMap();

        bind(in, act, "brighter", new KeyStroke[]{
                KeyStroke.getKeyStroke('+'), KeyStroke.getKeyStroke('=')}, new AbstractAction() {
            public void actionPerformed(ActionEvent e) { adjustExposure(1.4f); }
        });
        bind(in, act, "dimmer", new KeyStroke[]{
                KeyStroke.getKeyStroke('-'), KeyStroke.getKeyStroke('_')}, new AbstractAction() {
            public void actionPerformed(ActionEvent e) { adjustExposure(1f / 1.4f); }
        });
        bind(in, act, "bigger", new KeyStroke[]{KeyStroke.getKeyStroke(']')}, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                pointSize = Math.min(4, pointSize + 1);
                pointSizeAuto = false;
                requestRender();
            }
        });
        bind(in, act, "smaller", new KeyStroke[]{KeyStroke.getKeyStroke('[')}, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                pointSize = Math.max(1, pointSize - 1);
                pointSizeAuto = false;
                requestRender();
            }
        });
        bind(in, act, "reset", new KeyStroke[]{KeyStroke.getKeyStroke('r')}, new AbstractAction() {
            public void actionPerformed(ActionEvent e) { resetView(); }
        });
        bind(in, act, "spin", new KeyStroke[]{KeyStroke.getKeyStroke(' ')}, new AbstractAction() {
            public void actionPerformed(ActionEvent e) { setSpinning(spinTimer == null); }
        });
    }

    private static void bind(InputMap in, ActionMap act, String name, KeyStroke[] keys, Action a) {
        for(KeyStroke k : keys){
            in.put(k, name);
        }
        act.put(name, a);
    }

    private void addPointSizeItem(JMenu menu, ButtonGroup group, final int value) {
        JRadioButtonMenuItem item = new JRadioButtonMenuItem(value + " px", pointSize == value);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                pointSize = value;
                pointSizeAuto = false;
                requestRender();
            }
        });
        group.add(item);
        menu.add(item);
    }

    private void addShapeItem(JMenu menu, ButtonGroup group, String label, final int value) {
        JRadioButtonMenuItem item = new JRadioButtonMenuItem(label, shape == value);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                shape = value;
                requestRender();
            }
        });
        group.add(item);
        menu.add(item);
    }

    private void addColorItem(JMenu menu, ButtonGroup group, String label, final int value) {
        JRadioButtonMenuItem item = new JRadioButtonMenuItem(label, colorMode == value);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                colorMode = value;
                requestRender();
            }
        });
        group.add(item);
        menu.add(item);
    }
}
