import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class TwoTupleVisualizer extends Visualizer {
    private BufferedImage img;
    private int divisions = 20;
    private ArrayList<HashMap<TwoByteTuple, Integer>> cachedFreqMaps;
    HashSet<TwoByteTuple> existingTuples;
    private String colorMode = "g";
    private Boolean gradientMode = true;
    private int cycles = 0;

    public TwoTupleVisualizer(int windowSize, Cantordust cantordust, JFrame frame) {
        super(windowSize, cantordust);
        this.img = null;
        initializeCaches();
        constructImageAsync();
        addChangeListeners();
        createPopupMenu(frame);
    }

    // Special constructor for initialization of plugin
    public TwoTupleVisualizer(int windowSize, Cantordust cantordust, MainInterface mainInterface, JFrame frame) {
        super(windowSize, cantordust, mainInterface);
        this.img = null;
        initializeCaches();
        constructImageAsync();
        addChangeListeners();
        createPopupMenu(frame);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Rectangle window = getVisibleRect();

        if (img != null)
            g.drawImage(img, 0, 0, (int)window.getWidth(), (int)window.getHeight(), this);
    }

    public void constructImageAsync() {
        new Thread(() -> {
            //initializeCaches();
            dataMicroSlider.setMinimum(dataMacroSlider.getValue());
            dataMicroSlider.setMaximum(dataMacroSlider.getUpperValue());
            int low = dataMicroSlider.getValue();
            int high = dataMicroSlider.getUpperValue();

            this.img = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);

            gradientPlot(img.createGraphics(), low, high);
            repaint();
        }).start();
    }

    private HashMap<TwoByteTuple, Integer> countedByteFrequencies(int low, int high) {
        // data needs fixed for large file sizes
        byte[] data = cantordust.mainInterface.getData();
        HashMap<TwoByteTuple, Integer> tuples = new HashMap<>();
        for(int tupleIdx=low; tupleIdx < high-1; tupleIdx++) {
            TwoByteTuple tuple = new TwoByteTuple(data[tupleIdx], data[tupleIdx+1]);
            Integer freq = tuples.get(tuple);
            if(freq != null) {
                existingTuples.add(tuple);
                tuples.put(tuple, freq + 1);
            } else {
                tuples.put(tuple,  1);
            }
        }
        return tuples;
    }

    private void initializeCaches() {
        cachedFreqMaps = new ArrayList<HashMap<TwoByteTuple, Integer>>();
        existingTuples = new HashSet<TwoByteTuple>();
        // data needs fixed for large file sizes
        byte[] data = cantordust.mainInterface.getData();
        int cachedSize = data.length / divisions;
        for(int div = 0; div < divisions - 1; div++) {
            cachedFreqMaps.add(countedByteFrequencies(div*cachedSize, (div+1)*cachedSize));
        }
        cachedFreqMaps.add(countedByteFrequencies((divisions-1)*cachedSize, data.length));
    }

    private void gradientPlot(Graphics2D g, int low, int high) {
        cycles += 1;
        // data needs fixed for large file sizes
        int cachedSize = cantordust.mainInterface.getData().length / divisions;
        HashMap<TwoByteTuple, Integer> totalFreqs = new HashMap<>();
        HashMap<TwoByteTuple, Integer> leftStraggler = null;
        HashMap<TwoByteTuple, Integer> rightStraggler = null;
        int firstCacheBlockStart = nextBlock(low, cachedSize);
        int lastCacheBlockEnd = lastBlock(high, cachedSize);
        if(firstCacheBlockStart != low) {
            leftStraggler = countedByteFrequencies(low, firstCacheBlockStart-1);
            for(TwoByteTuple tuple: leftStraggler.keySet()) {
                if(totalFreqs.containsKey(tuple)) {
                    totalFreqs.put(tuple, leftStraggler.get(tuple) + totalFreqs.get(tuple));
                } else {
                    totalFreqs.put(tuple, leftStraggler.get(tuple));
                }
            }
        }
        for(int currentBlock = firstCacheBlockStart / cachedSize; currentBlock <= lastCacheBlockEnd / cachedSize; currentBlock++) {
            mergeFreqCounts(cachedFreqMaps.get(currentBlock-1), totalFreqs);
        }

        int colorStep = 5;
        int min = 0;
        for(TwoByteTuple twoTuple: totalFreqs.keySet()) {
            int freq = totalFreqs.get(twoTuple);
            //g.setColor(new Color(0, min + (freq*colorStep > 255 - min ? 255 - min : freq*colorStep), 0));
            g.setColor(getColor(freq, colorMode));
            //int colorVal = min + (freq*colorStep > 255 - min ? 255 - min : freq*colorStep);
            //g.setColor(new Color(colorVal, colorVal, colorVal));
            int x = twoTuple.x & 0xff;
            int y = twoTuple.y & 0xff;
            // g.fill(new Rectangle2D.Double(x*blockWidth, y*blockWidth, blockWidth, blockWidth));
            g.fill(new Rectangle2D.Double(y, x, 1, 1));

        }
        g.dispose();
    }

    private void mergeFreqCounts(HashMap<TwoByteTuple, Integer> sender, HashMap<TwoByteTuple, Integer> reciever) {
        for(TwoByteTuple tuple: sender.keySet()) {
            reciever.put(tuple, sender.get(tuple) + (reciever.containsKey(tuple) ? reciever.get(tuple) : 0));
        }
    }

    private int nextBlock(int x, int size) {
        if (x % size == 0) {
            return x;
        } else if (x < size) {
            return size;
        } else {
            return Math.floorDiv(x, size)*size + size;
        }
    }

    private int lastBlock(int x, int size) {
        return x - (x % size);
    }
        /*int colorStep = 10;
        int min = 60;
        for(TwoByteTuple twoTuple: tuples.keySet()) {
            freq = tuples.get(twoTuple);
            g.setColor(new Color(0, min + (freq*colorStep > 255 - min ? 255 - min : freq*colorStep), 0));
            //int colorVal = min + (freq*colorStep > 255 - min ? 255 - min : freq*colorStep);
            //g.setColor(new Color(colorVal, colorVal, colorVal));
            int x = twoTuple.x & 0xff;
            int y = twoTuple.y & 0xff;
            g.fill(new Rectangle2D.Double(x, y, 1, 1));
        }
    }*/

    public static int getWindowSize() {
        return 500;
    }

    private void addChangeListeners() {
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
    }

    private Color getColor(int freq, String rgbPosition) {
        int colorStep = 5;
        int min = 10;
        switch(rgbPosition) {
            case "r":
                if(gradientMode)
                    return new Color(min + (freq*colorStep > 255 - min ? 255 - min : freq*colorStep), 0, 0);
                else {
                    return Color.RED;
                }
            case "g":
                if(gradientMode) {
                    return new Color(0, min + (freq * colorStep > 255 - min ? 255 - min : freq * colorStep), 0);
                } else {
                    return Color.GREEN;
                }
            case "b":
                if(gradientMode) {
                    return new Color(0, 0, min + (freq * colorStep > 255 - min ? 255 - min : freq * colorStep));
                } else {
                    return Color.BLUE;
                }
            default:
                return null;
        }
    }

    public void createPopupMenu(JFrame frame){
        JPopupMenu popup = new JPopupMenu("colors");
        JMenuItem redItem = new JMenuItem("red");
        redItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                colorMode = "r";
                constructImageAsync();
            }
        });
        popup.add(redItem);

        JMenuItem greenItem = new JMenuItem("green");
        greenItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                colorMode = "g";
                constructImageAsync();
            }
        });
        popup.add(greenItem);

        JMenuItem blueItem = new JMenuItem("blue");
        blueItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                colorMode = "b";
                constructImageAsync();
            }
        });
        popup.add(blueItem);


        JMenuItem gradientToggle = new JMenuItem("toggle gradient");
        gradientToggle.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                gradientMode = !gradientMode;
                constructImageAsync();
            }
        });
        popup.add(gradientToggle);

        this.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if(e.getButton() == 3){
                    popup.show(frame, TwoTupleVisualizer.this.getX() + e.getX(), TwoTupleVisualizer.this.getY() + e.getY());
                }
            }
        });

        this.add(popup);
    }

}
