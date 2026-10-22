package resources;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Supplier;

public class MainInterface extends JPanel {
    private byte[] data;
    private byte[] fullData;
    public BitMapSlider macroSlider;
    public BitMapSlider microSlider;
    public JSlider widthSlider;
    public JSlider offsetSlider;
    public JSlider dataSlider;
    public JButton widthDownButton;
    public JButton widthUpButton;
    public JButton offsetDownButton;
    public JButton offsetUpButton;
    public JButton microUpButton;
    public JButton microDownButton;
    public JButton hilbertMapButton;
    public JButton themeButton;
    public JButton twoTupleButton;
    public JButton eightBitPerPixelBitMapButton;
    public JButton byteCloudButton;
    public JButton metricMapButton;
    public JButton oneTupleButton;
    public JButton threeTupleButton;
    public JPopupMenu popup;

    public Host cantordust;
    public JLabel dataRange = new JLabel();
    public JLabel macroCaption = new JLabel();
    public JLabel microCaption = new JLabel();
    /** Only present for files too large to hold at once; see the dataSlider. */
    private JLabel fileCaption;
    public JLabel macroValueHigh = new JLabel();
    public JLabel macroValueLow = new JLabel();
    public JLabel microValueHigh = new JLabel();
    public JLabel microValueLow = new JLabel();
    public JLabel widthValue = new JLabel();
    public JLabel offsetValue = new JLabel();
    public JLabel programName = new JLabel();
    /** Shared readout for what a click in a visualization landed on. */
    public JLabel visStatus = new JLabel(" ");

    public JPanel currVis = new JPanel();
    /** Owns the centre of the window; the current visualization is its only child. */
    private JPanel visHolder;

    /* visualizers stored here so no duplicate visualizer instances are ever created.*/
    public HashMap<visualizerMapKeys, JPanel> visualizerPanels;

    public enum visualizerMapKeys {
        BITMAP,
        BYTECLOUD,
        METRIC,
        TWOTUPLE,
        ONETUPLE,
        THREETUPLE
    }

    public String basePath;
    /** Set while one slider is updating the other, to stop the two listeners re-entering. */
    private boolean syncingSliders = false;
    protected byte theme;
    protected Boolean dispMetricMap;

    public MainInterface(byte[] mdata, Host cd) throws IOException {
        this.data = mdata;
        this.fullData = mdata;
        this.cantordust = cd;
        visualizerPanels = new HashMap<>();

        this.dispMetricMap = false;
        this.basePath = this.cantordust.getCurrentDirectory();

        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        // A real layout. The previous one used GridBagLayout grid indices as if
        // they were pixel coordinates (gridx = xOffset + 532, gridwidth = 512),
        // which only ever looked right at one window size: nothing carried a
        // weight, so docking the panel left the visualization at its minimum
        // size while the buttons were positioned past the right edge.
        setLayout(new BorderLayout(8, 8));

        if(fullData.length > 26214400){
            // 0xfffff = 1048575, 25MB = 0x1900000 = 26214400 bytes
            this.data = Arrays.copyOfRange(fullData, 0, 1048575);
            int range = fullData.length - 1048575;
            dataSlider = new JSlider(1, range);
            dataSlider.setOrientation(SwingConstants.VERTICAL);
            dataSlider.setInverted(true);
            dataSlider.setValue(0);
            dataSlider.setPreferredSize(new Dimension(34, 320));
            dataSlider.setMinimumSize(new Dimension(18, 60));
        }
        cantordust.cdprint("data: "+data.length+"\n");

        macroSlider = new BitMapSlider(1, this.data.length-1, this.data, this.cantordust);
        macroSlider.setValue(1);
        macroSlider.setUpperValue(this.data.length-1);

        microSlider = new BitMapSlider(0, this.data.length-1, this.data, this.cantordust);
        microSlider.setValue(macroSlider.getValue());
        microSlider.setUpperValue(macroSlider.getUpperValue());

        // Sliders need a small minimum, or the layout cannot shrink them and
        // the panel stops fitting into a docked window at all.
        for(BitMapSlider s : new BitMapSlider[]{macroSlider, microSlider}){
            s.setPreferredSize(new Dimension(92, 320));
            s.setMinimumSize(new Dimension(44, 60));
        }

        Dimension incDim = new Dimension(NUDGE, NUDGE);
        Insets zeroIn = new Insets(0, 0, 0, 0);
        microDownButton = stepButton(ChevronIcon.LEFT, "Nudge the selection back", new dec_micro(), incDim, zeroIn);
        microUpButton = stepButton(ChevronIcon.RIGHT, "Nudge the selection forward", new inc_micro(), incDim, zeroIn);
        widthDownButton = stepButton(ChevronIcon.LEFT, "Narrower", new dec_width(), incDim, zeroIn);
        widthUpButton = stepButton(ChevronIcon.RIGHT, "Wider", new inc_width(), incDim, zeroIn);
        offsetDownButton = stepButton(ChevronIcon.LEFT, "Shift back a byte", new dec_offset(), incDim, zeroIn);
        offsetUpButton = stepButton(ChevronIcon.RIGHT, "Shift forward a byte", new inc_offset(), incDim, zeroIn);

        widthSlider = new JSlider(1, 1024);
        widthSlider.setValue(512);
        widthSlider.setOrientation(SwingConstants.HORIZONTAL);
        widthSlider.setMinimumSize(new Dimension(60, 20));

        offsetSlider = new JSlider(1, 255);
        offsetSlider.setValue(0);
        offsetSlider.setMaximum(255);
        offsetSlider.setOrientation(SwingConstants.HORIZONTAL);
        offsetSlider.setMinimumSize(new Dimension(60, 20));

        macroCaption = new JLabel("Overview");
        microCaption = new JLabel("Selection");
        macroCaption.setHorizontalAlignment(SwingConstants.CENTER);
        microCaption.setHorizontalAlignment(SwingConstants.CENTER);
        macroCaption.setToolTipText("Which part of the file the selection slider covers");
        microCaption.setToolTipText("The bytes drawn in the visualization");

        macroSlider.setToolTipText("Overview: choose which part of the file the selection slider covers");
        microSlider.setToolTipText("Selection: choose the bytes drawn in the visualization");
        widthSlider.setToolTipText("Width of the rendered image, in pixels");
        offsetSlider.setToolTipText("Shift the data by a number of bytes before rendering");
        microUpButton.setToolTipText("Nudge the selection forward");
        if(dataSlider != null){
            dataSlider.setToolTipText("Scroll the 1MB working window through a file too large to hold at once");
        }

        Font readout = macroValueLow.getFont().deriveFont(Font.PLAIN,
                Math.max(10f, macroValueLow.getFont().getSize2D() - 1f));
        for(JLabel l : new JLabel[]{macroValueLow, macroValueHigh, microValueLow, microValueHigh}){
            l.setHorizontalAlignment(SwingConstants.CENTER);
            l.setFont(readout);
        }
        updateMacroLabels();
        updateMicroLabels();

        widthValue.setText("Width " + hex(widthSlider.getValue()));
        offsetValue.setText("Offset " + hex(offsetSlider.getValue()));
        widthValue.setFont(readout);
        offsetValue.setFont(readout);

        add(buildSliderPanel(), BorderLayout.WEST);

        // The visualization lives in a holder that owns the centre of the
        // window, so every spare pixel goes to it and swapping visualizations
        // is a swap of one child rather than a relayout of the whole panel.
        visHolder = new JPanel(new BorderLayout());
        visHolder.setOpaque(false);
        visHolder.setMinimumSize(new Dimension(120, 120));
        currVis = new MetricMap(MetricMap.getWindowSize(), cantordust, this);
        visualizerPanels.put(visualizerMapKeys.METRIC, currVis);
        visHolder.add(currVis, BorderLayout.CENTER);
        add(visHolder, BorderLayout.CENTER);

        add(buildToolbar(), BorderLayout.EAST);
        JPanel south = new JPanel(new BorderLayout(0, 4));
        south.setOpaque(false);
        visStatus.setFont(readout);
        visStatus.setToolTipText("What the last click in the visualization landed on");
        south.add(visStatus, BorderLayout.NORTH);
        south.add(buildControls(), BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);


        // Add listener to update display.
        if(dataSlider != null){
            dataSlider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    JSlider slider = (JSlider)e.getSource();
                    data = Arrays.copyOfRange(fullData, dataSlider.getValue(), dataSlider.getValue() + 1048575);
    
                    long minGhidraAddress1 = cantordust.getMinAddressOffset();
                    // Update text for upper and lower value of microSlider
                    long maxAddress1 = minGhidraAddress1 + dataSlider.getValue() + microSlider.getUpperValue();
                    long minAddress1 = minGhidraAddress1 + dataSlider.getValue() + macroSlider.getValue() + microSlider.getValue() - 1;
                    microValueHigh.setText(Long.toHexString(maxAddress1).toUpperCase());
                    microValueLow.setText(Long.toHexString(minAddress1).toUpperCase());
                    if(slider.getValueIsAdjusting()){
                        macroSlider.updateData(data);
                        microSlider.updateData(data);
                        macroSlider.ui.makeBitmapAsync(0, data.length);
                        microSlider.ui.makeBitmapAsync(macroSlider.getValue(), macroSlider.getUpperValue());
                    }
                }
            });
        }
        macroSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if(syncingSliders){
                    return;
                }
                int lo = macroSlider.getValue();
                int hi = macroSlider.getUpperValue();
                updateMacroLabels();

                // Keep the micro selection over the same bytes, clamped into the
                // new window. The previous code rescaled it proportionally, so
                // resizing the macro window slid the selection to a different
                // part of the file; it also clamped first and then overwrote
                // that with the rescale, making the clamp dead code.
                int microLo = Math.min(Math.max(microSlider.getValue(), lo), hi);
                int microHi = Math.min(Math.max(microSlider.getUpperValue(), microLo), hi);

                // One atomic model update: setting minimum, maximum, value and
                // extent separately fired four events, each re-entering these
                // listeners, and the order decided whether a value got clamped.
                syncingSliders = true;
                try {
                    microSlider.getModel().setRangeProperties(microLo, microHi - microLo,
                            lo, hi, microSlider.getValueIsAdjusting());
                } finally {
                    syncingSliders = false;
                }
                updateMicroLabels();

                if(macroSlider.getValueIsAdjusting()) {
                    repaint();
                } else {
                    // Redraw the micro slider's strip for the window now selected.
                    microSlider.ui.makeBitmapAsync(lo, hi);
                }
            }
        });
        microSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if(syncingSliders){
                    return;
                }
                // The model already confines this slider to the macro window, so
                // re-clamping here only fought the macro listener.
                updateMicroLabels();
                if(microSlider.getValueIsAdjusting()) {
                    repaint();
                }
            }
        });
        widthSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider slider = (JSlider) e.getSource();
                widthValue.setText("Width " + hex(slider.getValue()));
            }
        });
        offsetSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider slider = (JSlider) e.getSource();
                offsetValue.setText("Offset " + hex(slider.getValue()));
            }
        });

        darkTheme();
    }

    /*public changeDemo() {
        JButton decLowerButton = new JButton("decrease lower bound");
        JButton incLowerButton = new JButton("increase lower bound");
        JButton decUpperButton = new JButton("decrease upper bound");
        JButton incUpperButton = new JButton("increase upper bound");
    }*/
    
    public static int getWindowWidth() {
        return 900;
    }

    public static int getWindowHeight() {
        return 645;
    }

    public byte[] getData() {
        return this.data;
    }

    /**
     * Sets the current theme to dark
     */
    private void darkTheme() {
        this.theme = 1;
        setTheme(Color.black, Color.white, Color.darkGray);
        // The button advertises what a click will do, so in the dark theme it
        // offers the sun.
        themeButton.setIcon(new ThemeIcon(true, 16));
        themeButton.setToolTipText("Switch to the light theme");
    }

    /**
     * Sets the current theme to light
     */
    private void lightTheme() {
        this.theme = 0;
        // "panelButtons.background" is a Ghidra theme key. If the running look
        // and feel does not define it, getColor returns null, and setting a null
        // background makes the panel transparent - which read as black text on a
        // black panel rather than a light theme.
        Color c = UIManager.getColor("panelButtons.background");
        if(c == null){
            c = UIManager.getColor("Panel.background");
        }
        if(c == null){
            c = Color.white;
        }
        Color textColor = Color.black;
        setTheme(c, textColor, c);
        themeButton.setIcon(new ThemeIcon(false, 16));
        themeButton.setToolTipText("Switch to the dark theme");
    }

    /**
     * Sets colors of various components
     */
    private void setTheme(Color c, Color textColor, Color buttonColor) {
        this.setBackground(c);

        this.widthSlider.setBackground(c);
        this.offsetSlider.setBackground(c);
        if(this.dataSlider != null){
            this.dataSlider.setBackground(c);
        }

        this.macroSlider.setBackground(c);
        this.microSlider.setBackground(c);

        this.macroValueHigh.setForeground(textColor);
        this.macroValueLow.setForeground(textColor);
        this.microValueHigh.setForeground(textColor);
        this.microValueLow.setForeground(textColor);
        this.widthValue.setForeground(textColor);
        this.offsetValue.setForeground(textColor);

        this.widthDownButton.setBackground(c);
        this.widthDownButton.setForeground(textColor);

        this.widthUpButton.setBackground(c);
        this.widthUpButton.setForeground(textColor);

        this.offsetDownButton.setBackground(c);
        this.offsetDownButton.setForeground(textColor);

        this.offsetUpButton.setBackground(c);
        this.offsetUpButton.setForeground(textColor);

        this.dataRange.setForeground(textColor);
        this.visStatus.setForeground(textColor);
        this.macroCaption.setForeground(textColor);
        this.microCaption.setForeground(textColor);
        if(this.fileCaption != null){
            this.fileCaption.setForeground(textColor);
        }
        this.programName.setForeground(textColor);

        this.microUpButton.setBackground(c);
        this.microUpButton.setForeground(textColor);
        this.microDownButton.setBackground(c);
        this.microDownButton.setForeground(textColor);

        this.themeButton.setBackground(buttonColor);
        this.themeButton.setForeground(textColor);

        if(dispMetricMap) {
            currVis.setBackground(c);
        }
    }


    /**
     * One of the small nudge buttons flanking a slider. The arrow is drawn rather
     * than typed as a "<" or ">" character, so it is centred, consistent, and
     * takes the theme's foreground colour.
     */
    private JButton stepButton(int direction, String tip, ActionListener action, Dimension size, Insets margin) {
        JButton b = new JButton(new ChevronIcon(direction, 14));
        b.addActionListener(action);
        b.setToolTipText(tip);
        b.setPreferredSize(size);
        b.setMinimumSize(size);
        b.setMargin(margin);
        b.setBorder(BorderFactory.createEmptyBorder());
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        return b;
    }

    /**
     * A captioned slider with its two readouts. These sliders are inverted, so
     * the low offset sits at the top and the high offset at the bottom; putting
     * each label on the end it belongs to reads correctly and costs no width,
     * where a side-by-side pair made the column twice as wide as the slider.
     *
     * Every column gets the same three-part frame - caption, readout, nudge row -
     * whether or not it has nudge buttons. Hanging a button off only one column
     * made that column's foot taller, which is why the two strips did not line
     * up with each other.
     */
    private JPanel sliderColumn(JLabel caption, JComponent slider, JLabel low, JLabel high, JComponent nudge) {
        JPanel col = new JPanel(new BorderLayout(0, 2));
        col.setOpaque(false);

        JPanel head = new JPanel(new GridLayout(2, 1));
        head.setOpaque(false);
        head.add(caption);
        head.add(low);
        col.add(head, BorderLayout.NORTH);

        col.add(slider, BorderLayout.CENTER);

        JPanel foot = new JPanel(new BorderLayout(0, 2));
        foot.setOpaque(false);
        foot.add(high, BorderLayout.NORTH);
        foot.add(nudge != null ? nudge : Box.createVerticalStrut(NUDGE), BorderLayout.CENTER);
        col.add(foot, BorderLayout.SOUTH);
        return col;
    }

    /** Height reserved for a column's nudge row, with or without buttons in it. */
    private static final int NUDGE = 20;

    /**
     * Back and forward, centred under the slider they belong to. There used to be
     * a single forward button with no partner, which read as a stray control.
     */
    private JPanel nudgeRow(JButton back, JButton forward) {
        JPanel row = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 6, 0));
        row.setOpaque(false);
        row.add(back);
        row.add(forward);
        return row;
    }

    /** The overview / selection sliders, plus the working-window scroller. */
    private JPanel buildSliderPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.insets = new Insets(0, 0, 0, 6);

        int x = 0;
        if(dataSlider != null){
            // Built through the same helper as the other two so the three
            // sliders line up; a bare column left this one taller than its
            // neighbours and its caption unstyled, which in the dark theme meant
            // black text on a black panel.
            fileCaption = new JLabel("File");
            fileCaption.setHorizontalAlignment(SwingConstants.CENTER);
            c.gridx = x++;
            c.weightx = 0;
            panel.add(sliderColumn(fileCaption, dataSlider, new JLabel(), new JLabel(), null), c);
        }
        c.gridx = x++;
        c.weightx = 1;
        panel.add(sliderColumn(macroCaption, macroSlider, macroValueLow, macroValueHigh, null), c);
        c.gridx = x;
        c.insets = new Insets(0, 0, 0, 0);
        panel.add(sliderColumn(microCaption, microSlider, microValueLow, microValueHigh,
                nudgeRow(microDownButton, microUpButton)), c);
        return panel;
    }

    /** The column of visualization buttons down the right-hand edge. */
    private JPanel buildToolbar() throws IOException {
        JPanel bar = new JPanel(new GridBagLayout());
        bar.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 4, 0);

        twoTupleButton = visButton("icon_2_tuple.bmp", "Two Tuple", new open_two_tuple());
        eightBitPerPixelBitMapButton = visButton("icon_bit_map.bmp", "Linear BitMap", new open_8bpp_BitMap());
        byteCloudButton = visButton("icon_cloud.bmp", "Byte Cloud", new open_byte_cloud());
        metricMapButton = visButton("icon_metricMap.png", "Metric Map", new open_metric_map());
        oneTupleButton = visButton("icon_1_tuple.bmp", "One Tuple", new open_one_tuple());
        threeTupleButton = visButton("icon_3_tuple.bmp", "Three Tuple", new open_three_tuple());

        JButton[] buttons = {twoTupleButton, eightBitPerPixelBitMapButton, byteCloudButton,
                             metricMapButton, oneTupleButton, threeTupleButton};
        int y = 0;
        for(JButton b : buttons){
            c.gridy = y++;
            bar.add(b, c);
        }

        themeButton = new JButton();
        themeButton.addActionListener(new change_theme());
        themeButton.setPreferredSize(new Dimension(52, 30));
        themeButton.setMinimumSize(new Dimension(28, 22));
        c.gridy = y++;
        c.insets = new Insets(8, 0, 0, 0);
        bar.add(themeButton, c);

        // Soak up the leftover height so the buttons stay at the top instead of
        // spreading out down a tall window.
        c.gridy = y;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        bar.add(Box.createVerticalGlue(), c);
        return bar;
    }

    private JButton visButton(String iconFile, String name, ActionListener action) throws IOException {
        Image icon;
        try (java.io.InputStream in = cantordust.openResource("resources/icons/" + iconFile)) {
            icon = ImageIO.read(in).getScaledInstance(41, 41, Image.SCALE_SMOOTH);
        }
        JButton b = new JButton(new RoundedIcon(icon, 41, 41));
        b.addActionListener(action);
        b.setPreferredSize(new Dimension(52, 52));
        // Lets the column compress in a short window rather than being clipped.
        b.setMinimumSize(new Dimension(28, 28));
        b.setToolTipText(name + "  (" + detachHint() + " for a new window)");
        return b;
    }

    /** Width and offset, along the bottom. Both sliders share the spare width. */
    private JPanel buildControls() {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.insets = new Insets(0, 2, 0, 2);
        c.fill = GridBagConstraints.HORIZONTAL;

        int x = 0;
        c.gridx = x++; c.weightx = 0; row.add(widthDownButton, c);
        c.gridx = x++; c.weightx = 1; row.add(widthSlider, c);
        c.gridx = x++; c.weightx = 0; row.add(widthUpButton, c);
        c.gridx = x++; c.insets = new Insets(0, 6, 0, 18); row.add(widthValue, c);
        c.insets = new Insets(0, 2, 0, 2);
        c.gridx = x++; row.add(offsetDownButton, c);
        c.gridx = x++; c.weightx = 1; row.add(offsetSlider, c);
        c.gridx = x++; c.weightx = 0; row.add(offsetUpButton, c);
        c.gridx = x;   c.insets = new Insets(0, 6, 0, 0); row.add(offsetValue, c);
        return row;
    }

    /**
     * Shows what a click in a visualization resolved to. Every visualization
     * reports through the same line so the feedback is in one predictable place
     * rather than a different corner of each panel.
     */
    public void setStatus(String text) {
        final String t = (text == null || text.isEmpty()) ? " " : text;
        if(SwingUtilities.isEventDispatchThread()){
            visStatus.setText(t);
        } else {
            SwingUtilities.invokeLater(() -> visStatus.setText(t));
        }
    }

    /** Format an offset the way Ghidra shows addresses, with an 0x prefix. */
    private static String hex(long v) {
        return "0x" + Long.toHexString(v).toUpperCase();
    }

    private void updateMacroLabels() {
        long base = cantordust.getMinAddressOffset();
        macroValueLow.setText(hex(base + macroSlider.getValue()));
        macroValueHigh.setText(hex(base + macroSlider.getUpperValue()));
    }

    private void updateMicroLabels() {
        long base = cantordust.getMinAddressOffset();
        if(dataSlider != null){
            base += dataSlider.getValue();
        }
        microValueLow.setText(hex(base + microSlider.getValue()));
        microValueHigh.setText(hex(base + microSlider.getUpperValue()));
    }

    /**
     * Modifier that opens a visualization in its own window instead of the main
     * one: Command on macOS, Control elsewhere. ActionEvent reports legacy
     * modifier bits, so the toolkit's extended mask is mapped back onto them.
     */
    private static int detachModifier() {
        int ex = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        return ((ex & InputEvent.META_DOWN_MASK) != 0) ? ActionEvent.META_MASK : ActionEvent.CTRL_MASK;
    }

    /** Label for the detach modifier, for button tooltips. */
    private static String detachHint() {
        return (detachModifier() == ActionEvent.META_MASK) ? "\u2318-click" : "Ctrl-click";
    }

    private static boolean opensInNewWindow(ActionEvent e) {
        return (e.getModifiers() & detachModifier()) != 0;
    }

    /**
     * Swap the main window's visualization for the panel stored under key,
     * building it through factory the first time it is asked for.
     */
    private void showInMainWindow(visualizerMapKeys key, Supplier<JPanel> factory) {
        dispMetricMap = false;
        if(!visualizerPanels.containsKey(key)) {
            visualizerPanels.put(key, factory.get());
        }
        JPanel next = visualizerPanels.get(key);
        if(next == currVis) {
            return;
        }
        // No preferred size is imposed: the holder gives the visualization the
        // whole centre of the window, whatever size that turns out to be.
        visHolder.removeAll();
        currVis = next;
        visHolder.add(currVis, BorderLayout.CENTER);
        visHolder.revalidate();
        visHolder.repaint();
    }

    private class open_one_tuple implements ActionListener {
        open_one_tuple() {
        	
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (opensInNewWindow(e)) {
                //JOptionPane.showMessageDialog(null, "test", "InfoBox: " + "test", JOptionPane.INFORMATION_MESSAGE);
                JFrame frame1 = new JFrame("1 Tuple Visualization");
                OneTupleVisualizer oneTupleVis = new OneTupleVisualizer(OneTupleVisualizer.getWindowSize(), cantordust);
                frame1.getContentPane().add(oneTupleVis);
                frame1.setSize(OneTupleVisualizer.getWindowSize(), OneTupleVisualizer.getWindowSize());
                //frame.pack();
                frame1.setVisible(true);
                frame1.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            } else if (!(currVis instanceof OneTupleVisualizer)) {
                showInMainWindow(visualizerMapKeys.ONETUPLE, () -> new OneTupleVisualizer(OneTupleVisualizer.getWindowSize(), cantordust));
            }
        }
    }

    private class open_two_tuple implements ActionListener {
        open_two_tuple() {

        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (opensInNewWindow(e)) {
                //JOptionPane.showMessageDialog(null, "test", "InfoBox: " + "test", JOptionPane.INFORMATION_MESSAGE);
                JFrame frame1 = new JFrame("2 Tuple Visualization");
                TwoTupleVisualizer twoTupleVis = new TwoTupleVisualizer(TwoTupleVisualizer.getWindowSize(), cantordust);
                frame1.getContentPane().add(twoTupleVis);
                frame1.setSize(TwoTupleVisualizer.getWindowSize(), TwoTupleVisualizer.getWindowSize());
                //frame.pack();
                frame1.setVisible(true);
                frame1.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            } else if (!(currVis instanceof TwoTupleVisualizer)) {
                showInMainWindow(visualizerMapKeys.TWOTUPLE, () -> new TwoTupleVisualizer(TwoTupleVisualizer.getWindowSize(), cantordust));
            }
        }
    }

    private class open_three_tuple implements ActionListener {
        open_three_tuple() {

        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (opensInNewWindow(e)) {
                JFrame frame1 = new JFrame("3 Tuple Visualization");
                ThreeTupleVisualizer threeTupleVis = new ThreeTupleVisualizer(ThreeTupleVisualizer.getWindowSize(), cantordust);
                frame1.getContentPane().add(threeTupleVis);
                frame1.setSize(ThreeTupleVisualizer.getWindowSize(), ThreeTupleVisualizer.getWindowSize());
                frame1.setVisible(true);
                frame1.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            } else if (!(currVis instanceof ThreeTupleVisualizer)) {
                showInMainWindow(visualizerMapKeys.THREETUPLE, () -> new ThreeTupleVisualizer(ThreeTupleVisualizer.getWindowSize(), cantordust));
            }
        }
    }

    private class open_8bpp_BitMap implements ActionListener {
        open_8bpp_BitMap() {

        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (opensInNewWindow(e)) {
                JFrame frame1 = new JFrame("Linear Bit Map");
                BitMapVisualizer bitMapVis = new BitMapVisualizer(BitMapVisualizer.getWindowSize(), cantordust);
                frame1.getContentPane().add(bitMapVis);
                bitMapVis.setColorMapper(new EightBitPerPixelMapper(cantordust));
                frame1.setSize(BitMapVisualizer.getWindowSize(), BitMapVisualizer.getWindowSize());
                frame1.setVisible(true);
                frame1.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            } else if (!(currVis instanceof BitMapVisualizer)) {
                showInMainWindow(visualizerMapKeys.BITMAP, () -> new BitMapVisualizer(BitMapVisualizer.getWindowSize(), cantordust));
            }
        }
    }

    private class open_byte_cloud implements ActionListener {
        open_byte_cloud() {
        	
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (opensInNewWindow(e)) {
                JFrame frame1 = new JFrame("Byte Cloud Visualization");
                ByteCloudVisualizer byteCloudVis = new ByteCloudVisualizer(ByteCloudVisualizer.getWindowSize(), cantordust);
                frame1.getContentPane().add(byteCloudVis);
                frame1.setSize(ByteCloudVisualizer.getWindowSize(), ByteCloudVisualizer.getWindowSize());
                frame1.setVisible(true);
                frame1.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            } else if (!(currVis instanceof ByteCloudVisualizer)) {
                showInMainWindow(visualizerMapKeys.BYTECLOUD, () -> new ByteCloudVisualizer(ByteCloudVisualizer.getWindowSize(), cantordust));
            }
        }
    }

    private class open_metric_map implements ActionListener {

        open_metric_map() {
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (opensInNewWindow(e)) {
                JFrame frame1 = new JFrame("Metric Map");
                MetricMap metricMap = new MetricMap(MetricMap.getWindowSize(), cantordust);
                frame1.getContentPane().add(metricMap);
                frame1.setSize(MetricMap.getWindowSize(), MetricMap.getWindowSize()+30);
                frame1.setVisible(true);
                frame1.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            } else if (!(currVis instanceof MetricMap)) {
                showInMainWindow(visualizerMapKeys.METRIC, () -> new MetricMap(MetricMap.getWindowSize(), cantordust));
            }
        }
    }

    private class dec_width implements ActionListener {
        dec_width() {
        	
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            widthSlider.setValue(widthSlider.getValue() - 1);
        }
    }
    
    private class inc_width implements ActionListener {
        inc_width() {
        	
        }
    
        @Override
        public void actionPerformed(ActionEvent e) {
            widthSlider.setValue(widthSlider.getValue() + 1);
        }
    }
    
    private class dec_offset implements ActionListener {
        dec_offset() {
        	
        }
    
        @Override
        public void actionPerformed(ActionEvent e) {
            offsetSlider.setValue(offsetSlider.getValue() - 1);
        }
    }
    
    private class inc_offset implements ActionListener {
        inc_offset() {
        	
        }
    
        @Override
        public void actionPerformed(ActionEvent e) {
            offsetSlider.setValue(offsetSlider.getValue() + 1);
        }
    }
    
    private class dec_micro implements ActionListener {
        dec_micro() {

        }

        @Override
        public void actionPerformed(ActionEvent e) {
            microSlider.setValue(microSlider.getValue() - 1);
        }
    }

    private class inc_micro implements ActionListener {
        inc_micro() {
        	
        }
    
        @Override
        public void actionPerformed(ActionEvent e) {
            microSlider.setValue(microSlider.getValue() + 1);
        }
    }

    private class change_theme implements ActionListener {
        change_theme() {

        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if(theme == 0) {
                // Swap to dark theme
                darkTheme();
            } else {
                // Swap to light theme
                lightTheme();
            }
        }
    }
}
