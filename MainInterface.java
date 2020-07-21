//import org.joml.Matrix4f;
//import org.lwjgl.BufferUtils;
//import org.lwjgl.opengl.*;
//import org.lwjgl.opengl.awt.AWTGLCanvas;
//import org.lwjgl.opengl.awt.GLData;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.util.Arrays;
import java.util.HashMap;

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
    public JButton hilbertMapButton;
    public JButton themeButton;
    public JButton twoTupleButton;
    public JButton eightBitPerPixelBitMapButton;
    public JButton byteCloudButton;
    public JButton metricMapButton;
    public JButton oneTupleButton;
    public JPopupMenu popup;

    private Cantordust cantordust;
    private JLabel dataRange = new JLabel();
    private JLabel macroValueHigh = new JLabel();
    private JLabel macroValueLow = new JLabel();
    private JLabel microValueHigh = new JLabel();
    private JLabel microValueLow = new JLabel();
    private JLabel widthValue = new JLabel();
    private JLabel offsetValue = new JLabel();
    private JLabel programName = new JLabel();

    private JPanel currVis = new JPanel();

    /* visualizers stored here so no duplicate visualizer instances are ever created.*/
    private HashMap<visualizerMapKeys, JPanel> visualizerPanels;

    private enum visualizerMapKeys {
        BITMAP,
        BYTECLOUD,
        METRIC,
        TWOTUPLE,
        ONETUPLE
    }

    private JFrame frame;
    private String basePath;
    private int xOffset = 0;
    protected byte theme;
    protected Boolean dispMetricMap;

    public MainInterface(byte[] mdata, Cantordust cantordust, JFrame frame) throws IOException {
        this.data = mdata;
        this.fullData = mdata;
        this.cantordust = cantordust;
        this.frame = frame;
        visualizerPanels = new HashMap<>();

        this.dispMetricMap = false;
        this.basePath = cantordust.currentDirectory;

        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        if(fullData.length > 26214400){
            // 0xfffff = 1048575, 25MB = 0x1900000 = 26214400 bytes
            this.data = Arrays.copyOfRange(fullData, 0, 1048575);
            int range = fullData.length - 1048575;
            dataSlider = new JSlider(1, range);
            dataSlider.setOrientation(SwingConstants.VERTICAL);
            dataSlider.setInverted(true);
            dataSlider.setValue(0);
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridheight = 512;
            xOffset = 5;
            gbc.gridwidth = xOffset;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.insets = new Insets(5, 5, 5, 5);
            add(dataSlider, gbc);
        }
        cantordust.cdprint("data: "+data.length+"\n");
        macroSlider = new BitMapSlider(1, this.data.length-1, this.data, this.cantordust);
        macroSlider.setValue(1);
        macroSlider.setUpperValue(this.data.length-1);
        gbc.gridx = xOffset + 0;
        gbc.gridy = 0;
        gbc.gridheight = 512;
        gbc.gridwidth = 10;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(5, 5, 5, 5);
        add(macroSlider, gbc);
        
        microSlider = new BitMapSlider(0, this.data.length-1, this.data, this.cantordust);
        microSlider.setValue(macroSlider.getValue());
        microSlider.setUpperValue(macroSlider.getUpperValue());
        gbc.gridx = xOffset + 10;
        add(microSlider, gbc);

        Dimension incDim = new Dimension(18, 18);
        Insets zeroIn = new Insets(0, 0, 0, 0);

        microUpButton = new JButton(">");
        microUpButton.addActionListener(new inc_micro());
        microUpButton.setPreferredSize(incDim);
        microUpButton.setMargin(zeroIn);
        microUpButton.setBorder(BorderFactory.createEmptyBorder());
        gbc.gridx = xOffset + 19;
        gbc.gridy = 512;
        gbc.gridheight = 1;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        add(microUpButton, gbc);

        widthDownButton = new JButton("<");
        widthDownButton.addActionListener(new dec_width());
        widthDownButton.setPreferredSize(incDim);
        widthDownButton.setMargin(zeroIn);
        widthDownButton.setBorder(BorderFactory.createEmptyBorder());
        gbc.gridx = xOffset + 20;
        add(widthDownButton, gbc);

        Dimension slideDim = new Dimension(200, 15);

        widthSlider = new JSlider(1, 1024);
        widthSlider.setValue(512);
        widthSlider.setMaximum(1024);
        widthSlider.setOrientation(SwingConstants.HORIZONTAL);
        widthSlider.setPreferredSize(slideDim);
        gbc.gridy = 512;
        gbc.gridx = xOffset + 21;
        gbc.gridheight = 1;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        add(widthSlider, gbc);

        widthUpButton = new JButton(">");
        widthUpButton.addActionListener(new inc_width());
        widthUpButton.setPreferredSize(incDim);
        widthUpButton.setMargin(zeroIn);
        widthUpButton.setBorder(BorderFactory.createEmptyBorder());
        gbc.gridx = xOffset + 260;
        add(widthUpButton, gbc);

        offsetDownButton = new JButton("<");
        offsetDownButton.addActionListener(new dec_offset());
        offsetDownButton.setPreferredSize(incDim);
        offsetDownButton.setMargin(zeroIn);
        offsetDownButton.setBorder(BorderFactory.createEmptyBorder());
        gbc.gridx = xOffset + 261;
        add(offsetDownButton, gbc);

        offsetSlider = new JSlider(1, 255);
        offsetSlider.setValue(0);
        offsetSlider.setMaximum(255);
        offsetSlider.setOrientation(SwingConstants.HORIZONTAL);
        offsetSlider.setPreferredSize(slideDim);
        gbc.gridx = xOffset + 270;
        add(offsetSlider, gbc);

        offsetUpButton = new JButton(">");
        offsetUpButton.addActionListener(new inc_offset());
        offsetUpButton.setPreferredSize(incDim);
        offsetUpButton.setMargin(zeroIn);
        offsetUpButton.setBorder(BorderFactory.createEmptyBorder());
        gbc.gridx = xOffset + 512;
        add(offsetUpButton, gbc);
        
        // Default Current Visualization: MetricMap
        currVis = new MetricMap(MetricMap.getWindowSize(), cantordust, this, frame, true);
        currVis.setPreferredSize(new Dimension(512, 512));
        gbc.gridx = xOffset + 20;
        gbc.gridy = 0;
        gbc.gridheight = 512;
        gbc.gridwidth = 512;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(5, 5, 5, 5);
        add(currVis, gbc);

        // Setup buttons and button icons
        
        Image twoTupleIcon = ImageIO.read(new File(basePath + "resources/icons/icon_2_tuple.bmp")).getScaledInstance(41, 41, Image.SCALE_SMOOTH);
        twoTupleButton = new JButton(new ImageIcon(twoTupleIcon));
        twoTupleButton.addActionListener(new open_two_tuple());
        twoTupleButton.setPreferredSize(new Dimension(50, 50));
        twoTupleButton.setBackground(Color.darkGray);
        twoTupleButton.setToolTipText("Two Tuple");
        gbc.gridx = xOffset + 532;
        gbc.gridheight = 1;
        gbc.gridwidth = 1;
        add(twoTupleButton, gbc);

        Image bmpIcon = ImageIO.read(new File(basePath + "resources/icons/icon_bit_map.bmp")).getScaledInstance(41, 41, Image.SCALE_SMOOTH);
        eightBitPerPixelBitMapButton = new JButton(new ImageIcon(bmpIcon));
        eightBitPerPixelBitMapButton.addActionListener(new open_8bpp_BitMap());
        eightBitPerPixelBitMapButton.setPreferredSize(new Dimension(50, 50));
        eightBitPerPixelBitMapButton.setBackground(Color.darkGray);
        eightBitPerPixelBitMapButton.setToolTipText("Linear BitMap");
        gbc.gridy = 1;
        add(eightBitPerPixelBitMapButton, gbc);

        Image byteCloudIcon = ImageIO.read(new File(basePath + "resources/icons/icon_cloud.bmp")).getScaledInstance(41, 41, Image.SCALE_SMOOTH);
        byteCloudButton = new JButton(new ImageIcon(byteCloudIcon));
        byteCloudButton.addActionListener(new open_byte_cloud());
        byteCloudButton.setPreferredSize(new Dimension(50, 50));
        byteCloudButton.setBackground(Color.darkGray);
        byteCloudButton.setToolTipText("Byte Cloud");
        gbc.gridy = 2;
        add(byteCloudButton, gbc);
        
        Image metricMapIcon = ImageIO.read(new File(basePath + "resources/icons/icon_metricMap.png")).getScaledInstance(41, 41, Image.SCALE_SMOOTH);
        metricMapButton = new JButton(new ImageIcon(metricMapIcon));
        metricMapButton.addActionListener(new open_metric_map());
        metricMapButton.setPreferredSize(new Dimension(50, 50));
        metricMapButton.setBackground(Color.darkGray);
        metricMapButton.setToolTipText("Metric Map");
        gbc.gridy = 3;
        add(metricMapButton, gbc);

        Image oneTupleIcon = ImageIO.read(new File(basePath + "resources/icons/icon_1_tuple.bmp")).getScaledInstance(41, 41, Image.SCALE_SMOOTH);
        oneTupleButton = new JButton(new ImageIcon(oneTupleIcon));
        oneTupleButton.addActionListener(new open_one_tuple());
        oneTupleButton.setPreferredSize(new Dimension(50, 50));
        oneTupleButton.setBackground(Color.darkGray);
        oneTupleButton.setToolTipText("One Tuple");
        gbc.gridy = 4;
        add(oneTupleButton, gbc);
        
        themeButton = new JButton("th");
        themeButton.addActionListener(new change_theme());
        gbc.gridy = 5;
        add(themeButton, gbc);

        long minGhidraAddress = Long.parseLong(cantordust.getCurrentProgram().getMinAddress().toString(false), 16);
        long maxAddress = minGhidraAddress + macroSlider.getUpperValue(); 
        long minAddress = minGhidraAddress + macroSlider.getValue() - 1;
        
        macroValueHigh.setText(Long.toHexString(maxAddress).toUpperCase());
        macroValueHigh.setHorizontalAlignment(SwingConstants.LEFT);

        macroValueLow.setText(Long.toHexString(minAddress).toUpperCase());
        macroValueLow.setHorizontalAlignment(SwingConstants.LEFT);

        maxAddress = minGhidraAddress + microSlider.getUpperValue();
        minAddress = minGhidraAddress + microSlider.getValue() - 1;
        
        programName.setText(cantordust.name);
        gbc.gridx = xOffset + 0;
        gbc.gridy = 513;
        //add(programName, gbc);

        microValueLow.setText(Long.toHexString(minAddress).toUpperCase());
        microValueLow.setHorizontalAlignment(SwingConstants.LEFT);
        gbc.gridx = xOffset + 5;
        gbc.gridwidth = 5;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        add(microValueLow, gbc);

        dataRange.setText("-");
        gbc.gridx = xOffset + 10;
        gbc.gridwidth = 1;
        add(dataRange, gbc);

        microValueHigh.setText(Long.toHexString(maxAddress).toUpperCase());
        microValueHigh.setHorizontalAlignment(SwingConstants.LEFT);
        gbc.gridx = xOffset + 11;
        gbc.gridwidth = 5;
        add(microValueHigh, gbc);


        widthValue.setText(Integer.toHexString(widthSlider.getValue()).toUpperCase());
        widthValue.setHorizontalAlignment(SwingConstants.LEFT);

        offsetValue.setText(Integer.toHexString(offsetSlider.getValue()).toUpperCase());
        offsetValue.setHorizontalAlignment(SwingConstants.LEFT);
         
        // Add listener to update display.
        if(dataSlider != null){
            dataSlider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    JSlider slider = (JSlider)e.getSource();
                    data = Arrays.copyOfRange(fullData, dataSlider.getValue(), dataSlider.getValue() + 1048575);
    
                    long minGhidraAddress1 = Long.parseLong(cantordust.getCurrentProgram().getMinAddress().toString(false), 16);
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
                BitMapSlider slider = (BitMapSlider) e.getSource();
                long minGhidraAddress1 = Long.parseLong(cantordust.getCurrentProgram().getMinAddress().toString(false), 16);
                long maxAddress1 = minGhidraAddress1 + slider.getUpperValue();
                long minAddress1 = minGhidraAddress1 + slider.getValue() - 1;

                // Update text for upper and lower value
                macroValueHigh.setText(Long.toHexString(maxAddress1).toUpperCase());
                macroValueLow.setText(Long.toHexString(minAddress1).toUpperCase());

                int max = microSlider.getMaximum();
                int min = microSlider.getMinimum();
                int high = microSlider.getUpperValue();
                int low = microSlider.getValue();
                double highRatio = (double)(high-min)/(double)(max-min);
                double lowRatio = (double)(low-min)/(double)(max-min);

                // Update the upper and lower value of microSlider
                microSlider.setMinimum(slider.getValue());
                microSlider.setMaximum(slider.getUpperValue());
                int nMax = microSlider.getMaximum();
                int nMin = microSlider.getMinimum();
                if(slider.getValue()-1 > microSlider.getValue()-1) {
                    microSlider.setValue(slider.getValue());
                }
                if(slider.getUpperValue() < microSlider.getUpperValue()) {
                    microSlider.setUpperValue(slider.getUpperValue());
                }
                microSlider.setUpperValue( (int)(highRatio * (nMax-nMin)) + nMin);
                microSlider.setValue( (int)(lowRatio * (nMax-nMin)) + nMin);

                // Update text for upper and lower value of microSlider
                if(dataSlider != null){
                    maxAddress1 = minGhidraAddress1 + dataSlider.getValue() + microSlider.getUpperValue();
                    minAddress1 = minGhidraAddress1 + dataSlider.getValue() + microSlider.getValue() - 1;
                    microValueHigh.setText(Long.toHexString(maxAddress1).toUpperCase());
                    microValueLow.setText(Long.toHexString(minAddress1).toUpperCase());
                } else {
                    maxAddress1 = minGhidraAddress1 + microSlider.getUpperValue();
                    minAddress1 = minGhidraAddress1 + microSlider.getValue() - 1 + slider.getValue();
                    microValueHigh.setText(Long.toHexString(maxAddress1).toUpperCase());
                    microValueLow.setText(Long.toHexString(minAddress1).toUpperCase());
                }

                if(slider.getValueIsAdjusting()) {
                    repaint();
                }
            }
        });
        microSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                BitMapSlider slider = (BitMapSlider) e.getSource();
                long minGhidraAddress1 = Long.parseLong(cantordust.getCurrentProgram().getMinAddress().toString(false), 16);
                long maxAddress1 = minGhidraAddress1 + slider.getUpperValue();
                long minAddress1 = minGhidraAddress1 + slider.getValue();

                // Make sure the slider stays within its bounds
                if(macroSlider.getValue()-1 > slider.getValue()-1) {
                    slider.setMinimum(macroSlider.getValue());
                    slider.setValue(macroSlider.getValue());
                }
                if(macroSlider.getUpperValue() < slider.getUpperValue()) {
                    slider.setMaximum(macroSlider.getUpperValue());
                    slider.setUpperValue(macroSlider.getUpperValue());
                }

                // Update text for the slider
                if(dataSlider != null){
                    // cantordust.cdprint("max"+slider.getMaximum()+"\n");
                    // cantordust.cdprint("min"+slider.getMinimum()+"\n");
                    maxAddress1 = maxAddress1 + dataSlider.getValue();
                    minAddress1 = minAddress1 + dataSlider.getValue();
                    microValueHigh.setText(Long.toHexString(maxAddress1).toUpperCase());
                    microValueLow.setText(Long.toHexString(minAddress1).toUpperCase());
                } else {
                    microValueHigh.setText(Long.toHexString(maxAddress1).toUpperCase());
                    microValueLow.setText(Long.toHexString(minAddress1).toUpperCase());
                }

                if(macroSlider.getValueIsAdjusting()) {
                    repaint();
                }
            }
        });
        widthSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider slider = (JSlider) e.getSource();
                widthValue.setText(Integer.toHexString(slider.getValue()).toUpperCase());
            }
        });
        offsetSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider slider = (JSlider) e.getSource();
                offsetValue.setText(Integer.toHexString(slider.getValue()).toUpperCase());
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

    public JFrame getFrame() {
        return this.frame;
    }

    /**
     * Sets the current theme to dark
     */
    private void darkTheme() {
        this.theme = 1;
        setTheme(Color.black, Color.white, Color.darkGray);
    }

    /**
     * Sets the current theme to light
     */
    private void lightTheme() {
        this.theme = 0;
        Color c = UIManager.getColor("panelButtons.background");
        Color textColor = Color.black;
        setTheme(c, textColor, c);
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
        this.programName.setForeground(textColor);

        this.microUpButton.setBackground(c);
        this.microUpButton.setForeground(textColor);

        this.themeButton.setBackground(buttonColor);
        this.themeButton.setForeground(textColor);

        if(dispMetricMap) {
            currVis.setBackground(c);
        }
    }

    private class open_one_tuple implements ActionListener {
        open_one_tuple() {
        	
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!(currVis instanceof OneTupleVisualizer)) {
                if ((e.getModifiers() & ActionEvent.SHIFT_MASK) > 0) {
                    currVis.setVisible(false);
                    remove(currVis);
                    dispMetricMap = false;
                    if(!visualizerPanels.containsKey(visualizerMapKeys.ONETUPLE)) {
                        visualizerPanels.put(visualizerMapKeys.ONETUPLE, new OneTupleVisualizer(OneTupleVisualizer.getWindowSize(), cantordust, frame));
                    }
                    currVis = visualizerPanels.get(visualizerMapKeys.ONETUPLE);
                    //currVis = new OneTupleVisualizer(OneTupleVisualizer.getWindowSize(), cantordust, frame);
                    currVis.setPreferredSize(new Dimension(512, 512));
                    currVis.setVisible(true);
                    GridBagConstraints gbc = new GridBagConstraints();
                    gbc.gridx = xOffset + 20;
                    gbc.gridy = 0;
                    gbc.gridheight = 512;
                    gbc.gridwidth = 512;
                    gbc.fill = GridBagConstraints.NONE;
                    gbc.anchor = GridBagConstraints.CENTER;
                    gbc.insets = new Insets(5, 5, 5, 5);
                    add(currVis, gbc);
                    validate();
                } else {
                    //JOptionPane.showMessageDialog(null, "test", "InfoBox: " + "test", JOptionPane.INFORMATION_MESSAGE);
                    JFrame frame1 = new JFrame("1 Tuple Visualization");
                    OneTupleVisualizer oneTupleVis = new OneTupleVisualizer(OneTupleVisualizer.getWindowSize(), cantordust, frame1);
                    frame1.getContentPane().add(oneTupleVis);
                    frame1.setSize(OneTupleVisualizer.getWindowSize(), OneTupleVisualizer.getWindowSize());
                    //frame.pack();
                    frame1.setVisible(true);
                    frame1.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                }
            }
        }
    }

    private class open_two_tuple implements ActionListener {
        open_two_tuple() {

        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!(currVis instanceof TwoTupleVisualizer)) {
                if ((e.getModifiers() & ActionEvent.SHIFT_MASK) > 0) {
                    currVis.setVisible(false);
                    remove(currVis);
                    dispMetricMap = false;
                    if(!visualizerPanels.containsKey(visualizerMapKeys.TWOTUPLE)) {
                        visualizerPanels.put(visualizerMapKeys.TWOTUPLE, new TwoTupleVisualizer(TwoTupleVisualizer.getWindowSize(), cantordust, frame));
                    }
                    currVis = visualizerPanels.get(visualizerMapKeys.TWOTUPLE);
                    //currVis = new OneTupleVisualizer(OneTupleVisualizer.getWindowSize(), cantordust, frame);
                    currVis.setPreferredSize(new Dimension(512, 512));
                    currVis.setVisible(true);
                    GridBagConstraints gbc = new GridBagConstraints();
                    gbc.gridx = xOffset + 20;
                    gbc.gridy = 0;
                    gbc.gridheight = 512;
                    gbc.gridwidth = 512;
                    gbc.fill = GridBagConstraints.NONE;
                    gbc.anchor = GridBagConstraints.CENTER;
                    gbc.insets = new Insets(5, 5, 5, 5);
                    add(currVis, gbc);
                    validate();
                } else {
                    //JOptionPane.showMessageDialog(null, "test", "InfoBox: " + "test", JOptionPane.INFORMATION_MESSAGE);
                    JFrame frame1 = new JFrame("2 Tuple Visualization");
                    TwoTupleVisualizer twoTupleVis = new TwoTupleVisualizer(TwoTupleVisualizer.getWindowSize(), cantordust, frame1);
                    frame1.getContentPane().add(twoTupleVis);
                    frame1.setSize(TwoTupleVisualizer.getWindowSize(), TwoTupleVisualizer.getWindowSize());
                    //frame.pack();
                    frame1.setVisible(true);
                    frame1.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                }
            }
        }
    }

    private class open_8bpp_BitMap implements ActionListener {
        open_8bpp_BitMap() {

        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!(currVis instanceof BitMapVisualizer)) {
                if ((e.getModifiers() & ActionEvent.SHIFT_MASK) > 0) {
                    currVis.setVisible(false);
                    remove(currVis);
                    dispMetricMap = false;
                    if(!visualizerPanels.containsKey(visualizerMapKeys.BITMAP)) {
                        cantordust.cdprint("map does not contain bitmap\n");
                        visualizerPanels.put(visualizerMapKeys.BITMAP, new BitMapVisualizer(BitMapVisualizer.getWindowSize(), cantordust, frame));
                    } else {cantordust.cdprint("map does contain bitmap\n");}
                    currVis = visualizerPanels.get(visualizerMapKeys.BITMAP);
                    currVis.setVisible(true);
                    currVis.setPreferredSize(new Dimension(512, 512));
                    GridBagConstraints gbc = new GridBagConstraints();
                    gbc.gridx = xOffset + 20;
                    gbc.gridy = 0;
                    gbc.gridheight = 512;
                    gbc.gridwidth = 512;
                    gbc.fill = GridBagConstraints.NONE;
                    gbc.anchor = GridBagConstraints.CENTER;
                    gbc.insets = new Insets(5, 5, 5, 5);
                    add(currVis, gbc);
                    validate();
                } else {
                    JFrame frame1 = new JFrame("Linear Bit Map");
                    BitMapVisualizer bitMapVis = new BitMapVisualizer(BitMapVisualizer.getWindowSize(), cantordust, frame1);
                    frame1.getContentPane().add(bitMapVis);
                    bitMapVis.setColorMapper(new EightBitPerPixelMapper(cantordust));
                    frame1.setSize(BitMapVisualizer.getWindowSize(), BitMapVisualizer.getWindowSize());
                    frame1.setVisible(true);
                    frame1.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                }
            }
        }
    }

    private class open_byte_cloud implements ActionListener {
        open_byte_cloud() {
        	
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!(currVis instanceof ByteCloudVisualizer)) {
                if ((e.getModifiers() & ActionEvent.SHIFT_MASK) > 0) {
                    currVis.setVisible(false);
                    remove(currVis);
                    dispMetricMap = false;
                    if(!visualizerPanels.containsKey(visualizerMapKeys.BYTECLOUD)) {
                        visualizerPanels.put(visualizerMapKeys.BYTECLOUD, new ByteCloudVisualizer(ByteCloudVisualizer.getWindowSize(), cantordust));
                    }
                    currVis = visualizerPanels.get(visualizerMapKeys.BYTECLOUD);
                    currVis.setVisible(true);
                    currVis.setPreferredSize(new Dimension(512, 512));
                    GridBagConstraints gbc = new GridBagConstraints();
                    gbc.gridx = xOffset + 20;
                    gbc.gridy = 0;
                    gbc.gridheight = 512;
                    gbc.gridwidth = 512;
                    gbc.fill = GridBagConstraints.NONE;
                    gbc.anchor = GridBagConstraints.CENTER;
                    gbc.insets = new Insets(5, 5, 5, 5);
                    add(currVis, gbc);
                    validate();
                } else {
                    JFrame frame1 = new JFrame("Byte Cloud Visualization");
                    ByteCloudVisualizer byteCloudVis = new ByteCloudVisualizer(ByteCloudVisualizer.getWindowSize(), cantordust);
                    frame1.getContentPane().add(byteCloudVis);
                    frame1.setSize(ByteCloudVisualizer.getWindowSize(), ByteCloudVisualizer.getWindowSize());
                    frame1.setVisible(true);
                    frame1.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                }
            }
        }
    }

    private class open_metric_map implements ActionListener {

        open_metric_map() {
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!(currVis instanceof MetricMap)) {
                if ((e.getModifiers() & ActionEvent.SHIFT_MASK) > 0) {
                    currVis.setVisible(false);
                    remove(currVis);
                    dispMetricMap = false;
                    if(!visualizerPanels.containsKey(visualizerMapKeys.METRIC)) {
                        visualizerPanels.put(visualizerMapKeys.METRIC, new MetricMap(MetricMap.getWindowSize(), cantordust, frame, true));
                    }
                    currVis = visualizerPanels.get(visualizerMapKeys.METRIC);
                    currVis.setPreferredSize(new Dimension(512, 512));
                    currVis.setVisible(true);
                    GridBagConstraints gbc = new GridBagConstraints();
                    gbc.gridx = xOffset + 20;
                    gbc.gridy = 0;
                    gbc.gridheight = 512;
                    gbc.gridwidth = 512;
                    gbc.fill = GridBagConstraints.NONE;
                    gbc.anchor = GridBagConstraints.CENTER;
                    gbc.insets = new Insets(5, 5, 5, 5);
                    add(currVis, gbc) ;
                    repaint();
                    validate();
                } else {
                    JFrame frame1 = new JFrame("Metric Map");
                    MetricMap metricMap = new MetricMap(MetricMap.getWindowSize(), cantordust, frame1, false);
                    frame1.getContentPane().add(metricMap);
                    frame1.setSize(metricMap.getWindowSize(), metricMap.getWindowSize()+30);
                    frame1.setVisible(true);
                    frame1.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                }
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
