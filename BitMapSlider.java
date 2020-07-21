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

public class BitMapSlider extends RangeSlider {
    protected byte[] data;
    protected Cantordust cd;
    BitMapSliderUI ui;

    /**
     * Constructs a BitMapSlider with the specified default minimum and maximum 
     * values.
     */
    public BitMapSlider(int min, int max, byte[] data, Cantordust cd) {
        super(min, max);
        initSlider();
        this.data = data;
        this.cd = cd;
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