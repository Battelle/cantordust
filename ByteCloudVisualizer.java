import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.HashMap;

public class ByteCloudVisualizer extends Visualizer {

    public ByteCloudVisualizer(int windowSize, Cantordust cantordust) {
        super(windowSize, cantordust);
    }

    public void paintComponent(Graphics g) {
        // JOptionPane.showMessageDialog(null, String.format("length = %d", mainInterface.getData().length), "InfoBox: " + "test", JOptionPane.INFORMATION_MESSAGE);
        super.paintComponent(g);
        dataMicroSlider.setMinimum(dataMacroSlider.getValue());
        dataMicroSlider.setMaximum(dataMacroSlider.getUpperValue());
        int low = dataMicroSlider.getValue();
        int high = dataMicroSlider.getUpperValue();
        byteCloud((Graphics2D)g, low, high);
        dataMacroSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                RangeSlider slider = (RangeSlider) e.getSource();
                int rlow = slider.getValue();
                int rhigh = slider.getUpperValue();
                byteCloud((Graphics2D)g, rlow, rhigh);
                repaint();
            }
        });
        dataMicroSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                RangeSlider slider = (RangeSlider) e.getSource();
                int rlow = slider.getValue();
                int rhigh = slider.getUpperValue();
                byteCloud((Graphics2D)g, rlow, rhigh);
                repaint();
            }
        });
    }

    private void byteCloud(Graphics2D g, int low, int high) {
        byte[] data = mainInterface.getData();
        int distance;
        HashMap<Byte, Integer> bytes = new HashMap<Byte, Integer>();
        Integer freq;
        int i = 0;
        int j = 0;
        float fontSize = 18.0f;
        float alpha = 1.0f;
        byte b;
        int maxFreq = 0;
        //Integer[] freqs;

        //initialize map so every byte is there with a frequency of 0
        for(i = 0; i < 256; i++){
            b = (byte) i;
            bytes.put(b, 0);
        }
        
        distance = low + 9999;
        if(low + 9999 > high){
            distance = high;
        }

        //Set the frequency for each byte within the specified area
        b = (byte)255;
        for(int byteIdx=low; byteIdx <= distance; byteIdx++) {
            if(data[byteIdx] != b && data[byteIdx] != 0x00){
                freq = bytes.get(data[byteIdx]);
                bytes.put(data[byteIdx], freq + 1);
                if(freq+1 > maxFreq){
                    maxFreq = freq+1;
                }
            }
        }

        //Set font details
        g.setColor(Color.GREEN);
        g.setFont(new Font("Courier New", Font.BOLD, 12));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        for(i = 0; i < 16; i++){
            for(j = 0; j < 16; j++){
                int textByte = i * 16 + j;
                float frequency = (float) bytes.get((byte)textByte)/maxFreq;
                fontSize *= frequency;
                String s = String.format("%02X", textByte);

                //set transparity
                alpha *= frequency;
                if(alpha >= 1.0f){
                    alpha = 1.0f;
                }
                if(alpha <= 0.0f){
                    alpha = 0.1f;
                }

                try {
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                } catch (IllegalArgumentException e) {
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC, 0));
                }
                if(fontSize < 7.0f){
                    fontSize = 0;
                }
                g.setFont(g.getFont().deriveFont(fontSize));    
                g.drawString(s,20 * (j+1),20 * (i+1));
                fontSize = 18;
                alpha = 1.0f;
            }
        }
        // String z = String.format("%d", maxFreq);
        // g.drawString(z, 720, 720);
    }

    public static int getWindowSize() {
        return 360;
    }
}