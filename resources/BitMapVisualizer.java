package resources;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class BitMapVisualizer extends Visualizer {
    private JSlider dataWidthSlider;
    private JSlider dataOffsetSlider;
    private JButton dataWidthDownButton;
    private JButton dataWidthUpButton;
    private JButton dataOffsetDownButton;
    private JButton dataOffsetUpButton;
    private JButton dataMicroUpButton;
    private int mode;
    private OffsetJump jump;
    /**
     * Geometry of the last image built, so a click can be turned back into a
     * file offset. This map is linear, so the pixel position is the offset -
     * no searching needed, unlike the tuple plots.
     */
    private int lastCols, lastRows, lastStep, lastLow, lastShift;

    private Image img;

    public BitMapVisualizer(int windowSize, GhidraSrc cantordust) {
        super(windowSize, cantordust);
        MainInterface mainInterface = cantordust.getMainInterface();
        dataWidthSlider = mainInterface.widthSlider;
        dataOffsetSlider = mainInterface.offsetSlider;
        dataWidthDownButton = mainInterface.widthDownButton;
        dataWidthUpButton = mainInterface.widthUpButton;
        dataOffsetDownButton = mainInterface.offsetDownButton;
        dataOffsetUpButton = mainInterface.offsetUpButton;
        dataMicroUpButton = mainInterface.microUpButton;
        mode = 0;
        this.img = new BufferedImage(1,1,1);
        createPopupMenu();

        dataMacroSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if(!dataMacroSlider.getValueIsAdjusting()) {
                    constructImageAsync();
                }
            }
        });
        dataMicroSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if(!dataMicroSlider.getValueIsAdjusting() && !dataMacroSlider.getValueIsAdjusting()) {
                    constructImageAsync();
                }
            }
        });

        dataWidthSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if(!dataWidthSlider.getValueIsAdjusting()) {
                    constructImageAsync();
                }
            }
        });
        dataOffsetSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if(!dataOffsetSlider.getValueIsAdjusting()) {
                    constructImageAsync();
                }
            }
        });
        dataWidthDownButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                constructImageAsync();
            }
        });
        dataWidthUpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                constructImageAsync();
            }
        });
        dataOffsetDownButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                constructImageAsync();
            }
        });
        dataOffsetUpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                constructImageAsync();
            }
        });        
        dataMicroUpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                constructImageAsync();
            }
        });

        this.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                constructImageAsync();
            }
        });

        // The first build is driven by componentResized above, which fires once
        // the panel is laid out. This used to be a thread spinning on
        // getVisibleRect().getWidth() == 0, which burned a whole core until the
        // window appeared.
    }
    
    public void createPopupMenu(){
        JPopupMenu popup = new JPopupMenu("test1");
        JMenuItem bpp_8 = new JMenuItem("8bpp");
        bpp_8.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {    
                mode = 0;
                constructImageAsync();
            }
        });
        popup.add(bpp_8);
        
        JMenuItem argb_32 = new JMenuItem("32bpp ARGB");
        argb_32.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mode = 1;
                constructImageAsync();
            }
        });
        popup.add(argb_32);
        
        JMenuItem bpp_24 = new JMenuItem("24bpp RGB");
        bpp_24.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {    
                mode = 2;
                constructImageAsync();
            }
        });
        popup.add(bpp_24);

        JMenuItem bpp_16 = new JMenuItem("16bpp ARGB1555");
        bpp_16.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {    
                mode = 3;
                constructImageAsync();
            }
        });
        popup.add(bpp_16);

        JMenuItem entropy = new JMenuItem("Entropy");
        entropy.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {    
                mode = 4;
                constructImageAsync();
            }
        });
        popup.add(entropy);
        
        this.addMouseListener(new MouseAdapter() {  
            public void mouseReleased(MouseEvent e) {  
                if(e.getButton() == 3){
                    popup.show(BitMapVisualizer.this, e.getX(), e.getY());
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getButton() == MouseEvent.BUTTON1){
                    jumpToPixel(e.getX(), e.getY());
                }
            }
        });  

        this.add(popup);
    }
        
    /**
     * Every pixel of a linear map is a known run of bytes, so a click resolves
     * to one offset directly.
     */
    private void jumpToPixel(int mx, int my) {
        int cols, rows, step, low, shift;
        synchronized(this) {
            cols = lastCols; rows = lastRows; step = lastStep; low = lastLow; shift = lastShift;
        }
        int w = getWidth(), h = getHeight();
        if(cols <= 0 || rows <= 0 || w <= 0 || h <= 0) {
            return;
        }
        int bx = mx * cols / w;
        int by = my * rows / h;
        if(bx < 0 || bx >= cols || by < 0 || by >= rows) {
            return;
        }
        byte[] data = mainInterface.getData();
        long offset = (long) low + (long)(by * cols + bx) * step + shift;
        if(offset < 0 || offset >= data.length) {
            return;
        }
        if(jump == null) {
            jump = new OffsetJump(cantordust);
        }
        mainInterface.setStatus(jump.toOffset(offset,
                String.format("%02X", data[(int) offset] & 0xff)));
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (img != null)
            g.drawImage(img, 0, 0, this);
    }

    public void constructImageAsync() {
        new Thread(() -> {
            constructImage();
        }).start();
    }

    private void constructImage() {
        // MainInterface nests the micro range inside the macro one as a single
        // atomic model update; a visualizer that also moved the bounds - from a
        // paint or a worker thread, as this did - fights that and re-fires the
        // listener that asked for this redraw.
        int low = dataMicroSlider.getValue();
        int high = dataMicroSlider.getUpperValue();
        int width = dataWidthSlider.getValue();
        int offset = dataOffsetSlider.getValue();

        byte[] data = mainInterface.getData();
        byte[] data_offset = new byte[data.length];
        int xMax = width;
        int y = 0;
        int x = 0;
        int i = 0;

        // Size from the component, not its visible rectangle: the two agree in a
        // free-floating window but not once the panel is docked.
        Rectangle window = new Rectangle(0, 0, getWidth(), getHeight());
        if(window.width <= 0 || window.height <= 0) {
            return;
        }

        // offset comes from a slider capped at 255, so it can exceed the length of
        // a small file. Clamping keeps the shift loops from running with a negative
        // bound and indexing out of the array.
        int shift = Math.min(offset, data.length);
        for(i = 0; i < data.length - shift; i++){
            data_offset[i] = data[i + shift];
        }
        for(i = data.length - shift; i < data.length; i++){
            data_offset[i] = 0;
        }

        Graphics2D g;
        BufferedImage bimg;

        switch(mode) {
            //32bpp ARGB
            case 1:
                bimg = new BufferedImage(width, (high-low)/xMax/4 + 1, BufferedImage.TYPE_INT_ARGB);
                g = bimg.createGraphics();
                for(i = low; i < high-4; i+=4) {
                    int pixel = ((data_offset[i+3] << 24) + (data_offset[i+2] << 16) + (data_offset[i+1] << 8) + data_offset[i]) & 0xFFFFFFFF;
                    g.setColor(new Color(pixel, true));
                    g.fill(new Rectangle2D.Double(x, y, 1, 1));
                    x++;
                    if(x == xMax) {
                        y++;
                        x = 0;
                    }
                }
                g.dispose();
                break;

            //24bpp RGB
            case 2:
                bimg = new BufferedImage(width, (high-low)/xMax/3 + 1, BufferedImage.TYPE_INT_ARGB);
                g = bimg.createGraphics();
                for (i=low; i < high-3; i+=3){
                    int pixel = ((data_offset[i+2] << 16) + (data_offset[i+1] << 8) + data_offset[i]) & 0xFFFFFFFF;
                    g.setColor(new Color(pixel));
                    g.fill(new Rectangle2D.Double(x, y, 1, 1));
                    x++;
                    if(x == xMax) {
                        y++;
                        x = 0;
                    }
                }
                g.dispose();
                break;

            //16bpp ARGB1555 color is too saturated
            case 3:
                bimg = new BufferedImage(width, (high-low)/xMax/2 + 1, BufferedImage.TYPE_INT_ARGB);
                g = bimg.createGraphics();
                for (i = low; i < high-2; i+=2){
                    int alpha = (data_offset[i+1] & 0x80) >> 7;
                    // These were integer divisions by 0x1F, so every channel collapsed
                    // to 0 unless the 5-bit field was exactly 31. Divide as float, and
                    // read the green low bits from data_offset like the other channels.
                    float red = ((data_offset[i+1] & 0x7C) >> 2)/(float)(0x1F);
                    float green = (((data_offset[i+1] & 0x03) << 3) + ((data_offset[i] & 0xE0) >> 5))/(float)(0x1F);
                    float blue  = (data_offset[i] & 0x1F)/(float)(0x1F); 
                    g.setColor(new Color(red,green,blue,alpha));
                    g.fill(new Rectangle2D.Double(x, y, 1, 1));
                    x++;
                    if(x == xMax) {
                        y++;
                        x = 0;
                    }
                }
                g.dispose();
                break;

            // entropy
            case 4:
                bimg = new BufferedImage(width, (high-low)/xMax + 1, BufferedImage.TYPE_INT_ARGB);
                g = bimg.createGraphics();
                ColorEntropy entropy = new ColorEntropy(cantordust, data);
                for (i = low; i < high; i++){
                    Rgb rgb = entropy.getPoint(i);
                    g.setColor(new Color(rgb.r, rgb.g, rgb.b));
                    g.fill(new Rectangle2D.Double(x, y, 1, 1));
                    x++;
                    if(x == xMax) {
                        y++;
                        x = 0;
                    }
                }
                g.dispose();
                break;

            // 8bpp
            default:
                bimg = new BufferedImage(width, (high-low)/xMax + 1, BufferedImage.TYPE_INT_ARGB);
                g = bimg.createGraphics();
                for(i = low; i < high; i++){
                    int unsignedByte = data_offset[i] & 0xFF;
                    g.setColor(new Color(0,unsignedByte,0));
                    g.fill(new Rectangle2D.Double(x, y, 1, 1));
                    x++;
                    if(x == xMax) {
                        y++;
                        x = 0;
                    }
                }
                g.dispose();
            }
        // Remember how this image was laid out; a click maps back through it.
        int step = (mode == 1) ? 4 : (mode == 2) ? 3 : (mode == 3) ? 2 : 1;
        synchronized(this) {
            lastCols = xMax;
            lastRows = bimg.getHeight();
            lastStep = step;
            lastLow = low;
            lastShift = shift;
        }

        // Scale the image
        this.img = bimg.getScaledInstance((int) window.getWidth(), (int) window.getHeight(), Image.SCALE_SMOOTH);
        repaint();
    }
    
    public static int getWindowSize() {
        return 800;
    }
}
