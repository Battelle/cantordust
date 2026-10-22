package resources;

/**
 * An extension of JSlider to select a range of values using two thumb controls.
 * The thumb controls are used to select the lower and upper value of a range
 * with predetermined minimum and maximum values.
 * 
 * <p>Note that BitMapSlider makes use of the default BoundedRangeModel, which 
 * supports an inner range defined by a value and an extent.  The upper value
 * returned by BitMapSlider is simply the lower value plus the extent.</p>
 */
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

public class BitMapSlider extends RangeSlider {

    /** The strip shows each byte's value. */
    public static final int MODE_VALUE = 0;
    /** The strip shows the Shannon entropy of each window of the range. */
    public static final int MODE_ENTROPY = 1;

    protected byte[] data;
    private int stripMode = MODE_VALUE;
    private JPopupMenu popup;
    protected Host 
cd;
    BitMapSliderUI ui;

    /**
     * Constructs a BitMapSlider with the specified default minimum and maximum 
     * values.
     */
    public BitMapSlider(int min, int max, byte[] data, Host 
cd) {
        super(min, max);
        initSlider();
        this.data = data;
        this.cd = cd;
        createPopup();
    }

    public int getStripMode() {
        return stripMode;
    }

    /**
     * Swaps what the strip behind the thumbs is showing. Byte value says what
     * the bytes are; entropy says how disordered they are, which is what tells
     * compressed and encrypted regions apart from code, text and padding.
     */
    public void setStripMode(int mode) {
        if(stripMode == mode){
            return;
        }
        stripMode = mode;
        setToolTipText(mode == MODE_ENTROPY
                ? "Strip: entropy - bright is high entropy (compressed or encrypted)"
                : "Strip: byte value");
        if(ui != null){
            ui.refresh();
        }
    }

    private void createPopup() {
        popup = new JPopupMenu("Strip");
        JMenu modes = new JMenu("Strip shows");
        ButtonGroup group = new ButtonGroup();
        addModeItem(modes, group, "Byte value", MODE_VALUE);
        addModeItem(modes, group, "Entropy", MODE_ENTROPY);
        popup.add(modes);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShow(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShow(e);
            }

            private void maybeShow(MouseEvent e) {
                if(e.isPopupTrigger()){
                    popup.show(BitMapSlider.this, e.getX(), e.getY());
                }
            }
        });
    }

    private void addModeItem(JMenu menu, ButtonGroup group, String label, final int mode) {
        JRadioButtonMenuItem item = new JRadioButtonMenuItem(label, stripMode == mode);
        item.addActionListener(e -> setStripMode(mode));
        group.add(item);
        menu.add(item);
    }

    /**
     * Initializes the slider by setting default properties.
     */
    private void initSlider() {
        setInverted(true);
        setPreferredSize(new Dimension(100, 500));
    }

    public void updateData(byte[] data){
        this.data = data;
        repaint();
    }


    /**
     * Overrides the superclass method to install the UI delegate to draw two
     * thumbs.
     */
    @Override
    public void updateUI() {
        this.ui = new BitMapSliderUI(this);
        setUI(this.ui);
        // Update UI for slider labels.  This must be called after updating the
        // UI of the slider.  Refer to JSlider.updateUI().
        updateLabelUIs();
    }
}