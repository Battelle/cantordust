import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
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
    private BufferedImage img;

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
        return new Dimension(100, 10);
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
        // Draw track.
        Rectangle trackBounds = trackRect;

        if (img != null) {
            g.drawImage(img, 0, 5, trackBounds.width + 50, trackBounds.height, null);
        }
    }

    /**
     * Makes a bitmap in a new thread, this makes it so updating the slider does not cause everything else to hang
     */
    public void makeBitmapAsync(int low, int high) {
        new Thread(() -> {
            while(((BitMapSlider) this.slider).data == null) {
            	// Wait for the data field to be populated if it isn't
            }
            makeBitmap(low, high);
        }).start();
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

        // Calculate width and height
        int width = 400;
        int height = (high-low)/width-1;

        // Create a new image
        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int count = low;

        // Populate the image
        for(int y=0; y < img.getHeight(); y++) {
            for(int x=0; x < img.getWidth(); x++) {
                img.setRGB(x, y, (new Color(0, data[count++] & 0xff, 0)).getRGB());
            }
        }

        this.slider.repaint();
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
            if(((BitMapSlider) slider) == ((BitMapSlider) slider).cd.mainInterface.macroSlider) {
                int low = ((BitMapSlider) slider).getValue()-1;
                int high = ((BitMapSlider) slider).getUpperValue();
                ((BitMapSliderUI) ((BitMapSlider) slider).cd.mainInterface.microSlider.getUI()).makeBitmapAsync(low, high);
            }

            super.mouseReleased(e);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            // Get the X and Y
            int x = e.getX();
            int y = e.getY();

            double uy = upperThumbRect.getY();
            double uh = upperThumbRect.getHeight();

            double ly = thumbRect.getY();
            double lh = thumbRect.getHeight();

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

            double uy = upperThumbRect.getY();
            double uh = upperThumbRect.getHeight();

            double ly = thumbRect.getY();
            double lh = thumbRect.getHeight();

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