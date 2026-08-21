package resources;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

public class OneTupleVisualizer extends Visualizer {
    private double blockHeight;
    private int groupLines = 16;
    private Color color = Color.GREEN;
    private OffsetJump jump;

    public OneTupleVisualizer(int windowSize, GhidraSrc cantordust) {
        super(windowSize, cantordust);
        blockHeight = blockWidth;
        cantordust.cdprint("about to execute createPopupMenu\n");
        createPopupMenu();
        addClickToJump();
    }

    // Special constructor for initialization of plugin
    public OneTupleVisualizer(int windowSize, GhidraSrc cantordust, MainInterface mainInterface) {
        super(windowSize, cantordust, mainInterface);
        blockHeight = blockWidth;
        createPopupMenu();
        addClickToJump();
    }

    /**
     * Each row is a run of 256 * groupLines bytes of the file and each column a
     * byte value, so a click identifies both a region and a value: jump to where
     * that value occurs inside that region rather than anywhere in the file.
     */
    private void addClickToJump() {
        jump = new OffsetJump(cantordust);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getButton() != MouseEvent.BUTTON1) {
                    return;
                }
                int w = getWidth(), h = getHeight();
                if(w <= 0 || h <= 0) {
                    return;
                }
                int value = e.getX() * 256 / w;
                int row = e.getY() * 256 / h;
                if(value < 0 || value > 255 || row < 0 || row > 255) {
                    return;
                }
                int low = dataMicroSlider.getValue();
                int high = dataMicroSlider.getUpperValue();
                int span = 256 * groupLines;
                int rowStart = low + row * span;
                int rowEnd = Math.min(high, rowStart + span);
                byte[] data = cantordust.getMainInterface().getData();
                String msg = jump.toPattern(data, rowStart, rowEnd, new int[]{value},
                        String.format("byte %02X in row %d", value, row));
                cantordust.getMainInterface().setStatus(msg);
            }
        });
    }

    public void createPopupMenu(){
        JPopupMenu popup = new JPopupMenu("test1");
        // add color options
        HashMap<String, Color> colorButtons = new HashMap<String, Color>() {{
            put("Green", Color.GREEN);
            put("Red", Color.RED);
            put("Blue", Color.BLUE);
            put("Magenta", Color.MAGENTA);
            put("Cyan", Color.CYAN);
            put("Yellow", Color.YELLOW);
            put("White", Color.WHITE);
            put("Orange", Color.ORANGE);
            put("Pink", Color.PINK);
        }};

        JMenu colors = new JMenu("Colors");
        for (Map.Entry<String, Color> entry : colorButtons.entrySet()) {
            String name = entry.getKey();
            Color c = entry.getValue();
            JMenuItem colorMenuItem = new JMenuItem(name);
            colorMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {    
                    color = c;
                    repaint();
                }
            });
            colors.add(colorMenuItem);
        }
        // add line options
        JMenu lines = new JMenu("Lines");
        for (int i=0;i<9;i++) {
            int j = (int)Math.pow(2,i);
            JMenuItem colorMenuItem = new JMenuItem(Integer.toString(j));
            colorMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {    
                    groupLines = j;
                    repaint();
                }
            });
            lines.add(colorMenuItem);
        }

        popup.add(colors);
        popup.add(lines);
        
        this.addMouseListener(new MouseAdapter() {  
            public void mouseReleased(MouseEvent e) {  
                if(e.getButton() == 3){
                    popup.show(OneTupleVisualizer.this, e.getX(), e.getY());
                }
            }                 
        }); 

        this.add(popup);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        // MainInterface nests the micro range inside the macro one as a single
        // atomic model update; a visualizer that also moved the bounds - from a
        // paint or a worker thread, as this did - fights that and re-fires the
        // listener that asked for this redraw.
        int low = dataMicroSlider.getValue();
        int high = dataMicroSlider.getUpperValue();
        gradientPlot((Graphics2D)g, low, high);
    }

    private void gradientPlot(Graphics2D g, int low, int high) {
        blockWidth = getWidth() / (double)0xff;
        blockHeight = getHeight() / (double)0xff;
        byte[] data = cantordust.getMainInterface().getData();
        byte[] byteArray = new byte[256*256];

        for(int i = 0; i < 256; i++){
            for(int j = 0; j < 256 * groupLines; j++){
                int dataIndex = low + i * 256 *groupLines + j;                
                if(dataIndex < high){
                    int p = i * 256 + (data[dataIndex] & 0xFF);
                    if(byteArray[p] == (j / 256 + 1) * (256 / groupLines) || byteArray[p] == 255){
                        continue;
                    }
                    byteArray[p] += (byte)(256 / groupLines);
                    if(byteArray[p] == 0){
                        byteArray[p] = (byte)255;
                    }
                }
            }
        }

        double x = 0;
        double y = 0;
        for(int i = 0; i < 256*256; i++) {
            // g.setColor(new Color(0, (int)byteArray[i] & 0xFF, 0));
            int red_rgb = color.getRed();
            int green_rgb = color.getGreen();
            int blue_rgb = color.getBlue();
            int diff = 256 - ((int)byteArray[i] & 0xFF);

            red_rgb = (red_rgb - diff) >= 0 ? (red_rgb-diff) : 0;
            green_rgb = (green_rgb - diff) >= 0 ? (green_rgb-diff) : 0;
            blue_rgb = (blue_rgb - diff) >= 0 ? (blue_rgb-diff) : 0;
                
            g.setColor(new Color(red_rgb, green_rgb, blue_rgb));
            g.fill(new Rectangle2D.Double(x*blockWidth, y*blockHeight, blockWidth, blockHeight));
            x++;
            if(x == 256){
                x = 0;
                y++;
            }
        }
    }

    public static int getWindowSize() {return 360;}
}