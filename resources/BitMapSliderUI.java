package resources;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseEvent;

import javax.swing.JSlider;

/**
 * UI delegate for the BitMapSlider component. BitMapSliderUI paints two thumbs,
 * one for the lower value and one for the upper value.
 */
class BitMapSliderUI extends RangeSliderUI {

    /** Narrowest window the entropy estimate is allowed to be measured over. */
    private static final int MIN_ENTROPY_WINDOW = 64;
    private static final double LOG2 = Math.log(2);

    private BufferedImage img;
    private int lastLow;
    private int lastHigh;

    public BitMapSliderUI(BitMapSlider b) {
        super(b);
        new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while(((BitMapSlider) this.slider).data == null) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            	// Wait until the data field is populated
            }
            int low = ((BitMapSlider) this.slider).getValue()-1;
            int high = ((BitMapSlider) this.slider).getUpperValue();

            makeBitmap(low, high);
        }).start();
    }
    
    /**
     * Returns the size of a thumb.
     */
    @Override
    protected Dimension getThumbSize() {
        // Follow the slider's width rather than assuming the 100px it used to be
        // given, so the thumbs still span the track once the panel is resizable.
        // getThumbSize runs before the track rectangle is calculated, so measure
        // the component instead of trackRect.
        Insets insets = slider.getInsets();
        int w = slider.getWidth() - insets.left - insets.right;
        return new Dimension(Math.max(24, w), 10);
    }
 
    /**
     * Creates a listener to handle track events in the specified slider.
     */
    @Override
    protected TrackListener createTrackListener(JSlider slider1) {
        return new BitMapTrackListener();
    }

    /**
     * Paints the track.
     */
    @Override
    public void paintTrack(Graphics g) {
        Rectangle trackBounds = trackRect;

        if (img != null) {
            // Draw into the track, not next to it. This used to ignore the
            // track's own origin, start 5px down, and run 50px wider than the
            // track, so the strip sat offset from the thumbs and spilled over
            // the edge of the slider at every size but the original one.
            g.drawImage(img, trackBounds.x, trackBounds.y,
                    trackBounds.width, trackBounds.height, null);
        }
    }

    /**
     * Makes a bitmap in a new thread, this makes it so updating the slider does not cause everything else to hang
     */
    public void makeBitmapAsync(int low, int high) {
        new Thread(() -> {
            // This used to spin on a null check, burning a core until the field
            // was set. The data is assigned in the constructor, so it is enough
            // to skip the redraw on the one case where it is genuinely absent.
            if(((BitMapSlider) this.slider).data == null) {
                return;
            }
            makeBitmap(low, high);
        }).start();
    }

    /** Redraw the strip over the range it is already showing. */
    public void refresh() {
        int low, high;
        synchronized (this) {
            low = lastLow;
            high = lastHigh;
        }
        if (high > low) {
            makeBitmapAsync(low, high);
        }
    }

    /**
     * Code that actually makes the bitmap
     */
    private void makeBitmap(int low, int high) {
        byte[] data = ((BitMapSlider) this.slider).data;

        // Check if low or high are out of range
        if (high > data.length) {
            high = data.length;
        }

        if (low < 0) {
            low = 0;
        }
        if (high <= low) {
            return;
        }
        synchronized (this) {
            lastLow = low;
            lastHigh = high;
        }

        BufferedImage next = (((BitMapSlider) this.slider).getStripMode() == BitMapSlider.MODE_ENTROPY)
                ? entropyStrip(data, low, high)
                : valueStrip(data, low, high);
        if (next != null) {
            img = next;
            this.slider.repaint();
        }
    }

    /** Each pixel is one byte, drawn as its value. */
    private BufferedImage valueStrip(byte[] data, int low, int high) {
        int width = 400;
        int height = (high-low)/width-1 > 0? (high-low)/width-1 : 1;

        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for(int y=0; y < height; y++) {
            for(int x=0; x < width; x++) {
                int idx = low + y * width + x;
                if (idx >= data.length) {
                    idx = data.length - 1;
                }
                out.setRGB(x, y, (new Color(0, data[idx] & 0xff, 0)).getRGB());
            }
        }
        return out;
    }

    /**
     * Shannon entropy of each successive window of the range, as a single
     * column that the track stretches to its width.
     *
     * The value strip shows what the bytes are; this shows how disordered they
     * are, which is what actually distinguishes compressed and encrypted regions
     * from code, text and padding. Reading that off the navigation control means
     * the interesting part of a file can be found before picking a visualization.
     *
     * Entropy is counted with a flat 256-entry histogram rather than through
     * Utils.entropy, which allocates a HashMap of boxed Bytes per call - fine for
     * one point, far too slow for a thousand windows of a large file.
     */
    private BufferedImage entropyStrip(byte[] data, int low, int high) {
        int span = high - low;
        if (span <= 0) {
            return null;
        }
        // One value per output row, capped so the cost does not grow with the
        // file, and floored at a window wide enough for the estimate to mean
        // something.
        int rows = Math.min(1024, Math.max(1, span / MIN_ENTROPY_WINDOW));
        int window = Math.max(1, span / rows);

        BufferedImage out = new BufferedImage(1, rows, BufferedImage.TYPE_INT_RGB);
        int[] hist = new int[256];
        for (int r = 0; r < rows; r++) {
            int start = low + r * window;
            int end = Math.min(high, start + window);
            if (start >= end) {
                break;
            }
            Arrays.fill(hist, 0);
            for (int i = start; i < end; i++) {
                hist[data[i] & 0xff]++;
            }
            int n = end - start;
            double bits = 0;
            for (int c : hist) {
                if (c > 0) {
                    double pr = c / (double) n;
                    bits -= pr * (Math.log(pr) / LOG2);
                }
            }
            // Eight bits is the ceiling for byte symbols: uniformly random data.
            int v = (int) (255.0 * bits / 8.0);
            if (v < 0) {
                v = 0;
            } else if (v > 255) {
                v = 255;
            }
            out.setRGB(0, r, DensityShader.shade(DensityShader.HEAT, v, 0));
        }
        return out;
    }

    /**
     * Paints the thumb for the lower value using the specified graphics object.
     */
    @Override
    protected void paintLowerThumb(Graphics g) {
        Rectangle knobBounds = thumbRect;
        int w = knobBounds.width;    
        
        // Create graphics copy.
        Graphics2D g2d = (Graphics2D) g.create();

        // Create default thumb shape.
        Shape thumbShape = createThumbShape(w - 1, 7);

        // Draw thumb.
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.translate(knobBounds.x, knobBounds.y);

        g2d.setColor(Color.gray);
        g2d.fill(thumbShape);

        g2d.setColor(Color.gray);
        g2d.draw(thumbShape);
        
        g2d.setColor(Color.black);
        g2d.fillPolygon(new int[]{(w-1)/2, (w-1)/2-4, (w-1)/2+4}, new int[]{2, 6, 6}, 3);

        // Dispose graphics.
        g2d.dispose();
    }
    
    /**
     * Paints the thumb for the upper value using the specified graphics object.
     */
    @Override
    protected void paintUpperThumb(Graphics g) {
        Rectangle knobBounds = upperThumbRect;
        int w = knobBounds.width;
        
        // Create graphics copy.
        Graphics2D g2d = (Graphics2D) g.create();

        // Create default thumb shape.
        Shape thumbShape = createThumbShape(w - 1, 7);

        // Draw thumb.
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.translate(knobBounds.x, knobBounds.y);

        g2d.setColor(Color.gray);
        g2d.fill(thumbShape);

        g2d.setColor(Color.gray);
        g2d.draw(thumbShape);

        g2d.setColor(Color.black);
        g2d.fillPolygon(new int[]{(w-1)/2, (w-1)/2-4, (w-1)/2+4}, new int[]{6, 2, 2}, 3);

        // Dispose graphics.
        g2d.dispose();
    }


    /**
     * Returns a Shape representing a thumb.
     */
    @Override
    public Shape createThumbShape(int width, int height) {
        // Use circular shape.
        Rectangle shape = new Rectangle(width, height);
        return shape;
    }
    
    /**
     * Listener to handle mouse movements in the slider track.
     */

    public class BitMapTrackListener extends RangeSliderUI.RangeTrackListener {
        private boolean windowSliding;
        private double previousY;

        private void updateRectanglesForSlidingWindow(MouseEvent e) {
            if(windowSliding) {
                double diff = previousY - e.getY();
                int upperThumbRectNewY = (int)(upperThumbRect.getY() - diff);
                int thumbRectNewY = (int)(thumbRect.getY() - diff);
                if(upperThumbRectNewY < yPositionForValue(slider.getMaximum()) && thumbRectNewY > yPositionForValue(slider.getMinimum())) {
                    upperThumbRect.setLocation((int)(upperThumbRect.getX()), upperThumbRectNewY);
                    thumbRect.setLocation((int)(thumbRect.getX()), thumbRectNewY);
                    previousY = e.getY();
                    slider.repaint();
                    slider.setCursor(new Cursor(e.getY() > previousY ? Cursor.S_RESIZE_CURSOR: Cursor.N_RESIZE_CURSOR));
                }
            }
        }

        private void updateValuesForSlidingWindow(MouseEvent e) {
            if(windowSliding) {
                double newVal = valueForYPosition((int)(thumbRect.getY()));
                //((BitMapSlider)slider).setValueWindowSlide((int)newVal);
                ((BitMapSlider)slider).getModel().setRangeProperties((int)newVal, slider.getExtent(), slider.getMinimum(),
                        slider.getMaximum(), slider.getValueIsAdjusting());
                windowSliding = false;
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            updateRectanglesForSlidingWindow(e);
            super.mouseDragged(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            updateValuesForSlidingWindow(e);

            lowerDragging = false;
            upperDragging = false;
            slider.setValueIsAdjusting(false);
            slider.setCursor(new Cursor(Cursor.HAND_CURSOR));
            // The micro slider's strip is redrawn from the macro slider's change
            // listener, which also covers changes made with the arrow buttons or
            // by the data slider, not just those made with the mouse.

            super.mouseReleased(e);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            // Get the X and Y
            int x = e.getX();
            int y = e.getY();

            // double uy = upperThumbRect.getY();
            // double uh = upperThumbRect.getHeight();

            // double ly = thumbRect.getY();
            // double lh = thumbRect.getHeight();

            // Check if the cursor is over or not over one of the slider rectangles
            boolean upperHover = false;
            boolean lowerHover = false;
            if (upperThumbSelected || slider.getMinimum() == slider.getValue()) {
                if (upperThumbRect.contains(x, y)) {
                    upperHover = true;
                } else if (thumbRect.contains(x, y)) {
                    lowerHover = true;
                }
            } else {
                if (thumbRect.contains(x, y)) {
                    lowerHover = true;
                } else if (upperThumbRect.contains(y, x)) {
                    upperHover = true;
                }
            }

            if(upperHover || lowerHover) {
                slider.setCursor(new Cursor(Cursor.HAND_CURSOR));
            } else {
                slider.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
            if(y > thumbRect.getY() && y < upperThumbRect.getY() && !thumbRect.contains(0, y) && !upperThumbRect.contains(0, y)) {
                slider.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            // Get the X and Y
            int x = e.getX();
            int y = e.getY();

            if(y > thumbRect.getY() && !thumbRect.contains(0, y) && y < upperThumbRect.getY() && !upperThumbRect.contains(0, y)) {
                windowSliding = true;
                previousY = y;
                return;
            }

            // double uy = upperThumbRect.getY();
            // double uh = upperThumbRect.getHeight();

            // double ly = thumbRect.getY();
            // double lh = thumbRect.getHeight();

            // Check if the cursor is over or not over one of the slider rectangles
            boolean upperPressed = false;
            boolean lowerPressed = false;
            if (upperThumbSelected || slider.getMinimum() == slider.getValue()) {
                if (upperThumbRect.contains(x, y)) {
                    upperPressed = true;
                } else if (thumbRect.contains(x, y)) {
                    lowerPressed = true;
                }
            } else {
                if (thumbRect.contains(x, y)) {
                    lowerPressed = true;
                } else if (upperThumbRect.contains(y, x)) {
                    upperPressed = true;
                }
            }

            if(upperPressed || lowerPressed) {
                slider.setCursor(new Cursor(Cursor.MOVE_CURSOR));
            } else {
                slider.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }

            super.mousePressed(e);
        }
    }
}