import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class OneTupleVisualizer extends Visualizer {
    private double blockHeight;
    private int groupLines = 16;
    private Popup popup;
    private Color color = Color.GREEN;

    public OneTupleVisualizer(int windowSize, Cantordust cantordust, JFrame frame) {
        super(windowSize, cantordust);
        blockHeight = blockWidth;
        cantordust.cdprint("about to execute createPopupMenu\n");
        createPopupMenu(frame);
    }

    public void createPopupMenu(JFrame frame){
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
        
        frame.addMouseListener(new MouseAdapter() {  
            public void mouseReleased(MouseEvent e) {  
                if(e.getButton() == 3){
                    popup.show(frame , e.getX(), e.getY());
                }
            }                 
        }); 

        this.add(popup);
        //frame.setVisible(false);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        dataMicroSlider.setMinimum(dataMacroSlider.getValue());
        dataMicroSlider.setMaximum(dataMacroSlider.getUpperValue());
        int low = dataMicroSlider.getValue();
        int high = dataMicroSlider.getUpperValue();
        gradientPlot((Graphics2D)g, low, high);
    }

    private void gradientPlot(Graphics2D g, int low, int high) {
        blockWidth = getWidth() / (double)0xff;
        blockHeight = getHeight() / (double)0xff;
        byte[] data = mainInterface.getData();
        byte[] byteArray = new byte[256*256];
        Integer freq;

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