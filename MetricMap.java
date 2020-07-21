// metricMap
import javax.swing.*;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.*;
import java.util.HashMap;
import java.util.TreeSet;
import java.awt.image.*;
import java.awt.Color;

public class MetricMap extends Visualizer{
    protected byte[] data;
    protected static int size_hilbert = 512;
    protected int[][] pixelMap2D;
    protected int[] pixelMap1D;
    protected HashMap<Integer, Integer> memLoc = new HashMap<Integer, Integer>();
    protected Popup popupAddr;
    protected JPopupMenu popupMenu;
    protected Scurve map;
    protected JPanel panel = new JPanel();
    protected String type_plot = "square";
    protected ColorSource csource;
    private JSlider dataWidthSlider;
    private boolean inMainInterface;
    private JLabel label;
    private boolean isClassifier = false;

    public MetricMap(int windowSize, Cantordust cantordust, JFrame frame, Boolean isCurrentView) {
        super(windowSize, cantordust);
        data = this.cantordust.mainInterface.getData();
        dataWidthSlider = mainInterface.widthSlider;
        createPopupMenu(frame);
        sliderConfig();
        mouseConfig(frame, isCurrentView);
        this.csource = new ColorEntropy(this.cantordust, getCurrentData());
        this.map = new Hilbert(this.cantordust, 2, (int)(Math.log(getWindowSize())/Math.log(2)));
        draw();
    }
    
    // Special constructor for initialization of plugin
    public MetricMap(int windowSize, Cantordust cantordust, MainInterface mainInterface, JFrame frame, Boolean isCurrentView) {
        super(windowSize, cantordust, mainInterface);
        data = this.mainInterface.getData();
        dataWidthSlider = mainInterface.widthSlider;
        createPopupMenu(frame);
        sliderConfig();
        mouseConfig(frame, isCurrentView);
        this.csource = new ColorEntropy(this.cantordust, getCurrentData());
        this.map = new Hilbert(this.cantordust, 2, (int)(Math.log(getWindowSize())/Math.log(2)));
        draw();
    }

    public void sliderConfig(){
        this.dataMicroSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent changeEvent) {
                if(!dataMicroSlider.getValueIsAdjusting() && !dataMacroSlider.getValueIsAdjusting()) {
                    draw();
                }
            }
        });
        this.dataMacroSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent changeEvent) {
                if(!dataMacroSlider.getValueIsAdjusting()) {
                    draw();
                }
            }
        });
        if(dataRangeSlider != null){
            this.dataRangeSlider.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent changeEvent) {
                    if(!dataRangeSlider.getValueIsAdjusting()) {
                        data = cantordust.mainInterface.getData();
                        draw();
                    }
                }
            });
        }
        dataWidthSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if(!dataWidthSlider.getValueIsAdjusting()) {
                    if(map.isType("linear")){
                        cantordust.cdprint("in linear, sliding\n");
                        cantordust.cdprint("ch:"+dataWidthSlider.getValue()+"\n");
                        int change = dataWidthSlider.getValue();
                        if(change < size_hilbert){
                            cantordust.cdprint("less\n");
                            int inc = (size_hilbert-change)*2;
                            change = size_hilbert+inc;
                            map.setWidth(change);
                            map.setHeight(size_hilbert);
                        }else if(change > size_hilbert){
                            cantordust.cdprint("more\n");
                            int inc = (change-size_hilbert)*2;
                            change = size_hilbert+inc;
                            map.setHeight(change);
                            map.setWidth(size_hilbert);
                        }
                        else{
                            map.setWidth(size_hilbert);
                            map.setHeight(size_hilbert);
                        }
                        draw();
                    }
                }
            }
        });
    }

    public void mouseConfig(JFrame frame, boolean isCurrentView){
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                // cantordust.cdprint("dragged\n");
                int b1 = MouseEvent.BUTTON1_DOWN_MASK;
                int b2 = MouseEvent.BUTTON2_DOWN_MASK;
                if ((e.getModifiersEx() & (b1 | b2)) == b1) {
                    if(popupAddr != null) {
                        popupAddr.hide();
                    }
                    if(e.getX() < size_hilbert && e.getY() < size_hilbert){
                        if(e.getX() >= 0 && e.getY() >= 0){
                            mousePressed(e);
                        }
                    }
                }
            }
            @Override
            public void mousePressed(MouseEvent e) {
                MouseListener[] mA = getMouseListeners();
                if(mA.length >= 1) {
                    mA[0].mousePressed(e);
                }
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int b1 = MouseEvent.BUTTON1_DOWN_MASK;
                int b2 = MouseEvent.BUTTON2_DOWN_MASK;
                if ((e.getModifiersEx() & (b1 | b2)) == b1) {
                    JPanel bv = MetricMap.this;
                    JFrame metricMap = frame;
                    int x_point=e.getX();
                    int y_point=e.getY();
                    int xf = metricMap.getX()+x_point;
                    int yf = metricMap.getY()+y_point;
                    if(isCurrentView){
                        xf = (int)bv.getLocationOnScreen().getX()+x_point;
                        yf = (int)bv.getLocationOnScreen().getY()+y_point-26;
                    }
                    TwoIntegerTuple p = new TwoIntegerTuple(x_point, y_point);
                    int currentLow = dataMicroSlider.getValue();
                    int loc = map.index(p);
                    if(isClassifier){
                        ClassifierModel classifier = cantordust.getClassifier();
                        //cantordust.cdprint(String.format("class at index %d : %s\n", loc, classifier.classes[classifier.classAtIndex(loc)]));
                    }
                    int memoryLocation = memLoc.get(loc)+currentLow;
                    if(dataRangeSlider != null){
                        memoryLocation = memoryLocation + cantordust.mainInterface.dataSlider.getValue();
                    }
                    long minGhidraAddress = Long.parseLong(cantordust.getCurrentProgram().getMinAddress().toString(false), 16);
                    String currentAddress = Long.toHexString(minGhidraAddress+(long)memoryLocation).toUpperCase();
                    JLabel l;
                    if(isClassifier) {
                        ClassifierModel classifier = cantordust.getClassifier();
                        l = new JLabel(classifier.classes[classifier.classAtIndex(memoryLocation)]);
                    } else {
                        l = new JLabel(currentAddress);
                    }
                    PopupFactory pf = new PopupFactory(); 
                    JPanel p2 = new JPanel();
                    if(mainInterface.theme == 1) {
                        l.setForeground(Color.white);
                        p2.setBackground(Color.black);
                    }
                    p2.add(l);
                    popupAddr = pf.getPopup(metricMap, p2, xf, yf);
                    popupAddr.show();
                    
                    try{
                        // Set current location in Ghidra to this address
                        cantordust.gotoFileAddress(memoryLocation);
                    } catch(IllegalArgumentException exception){
                    }
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if(popupAddr != null) {
                    popupAddr.hide();
                }
                if(e.getButton() == 3){
                    popupMenu.show(frame, MetricMap.this.getX() + e.getX(), MetricMap.this.getY() + e.getY());
                }
            }
        });
    }
    
    public byte[] getCurrentData(){
        int lowerBound = dataMicroSlider.getValue();
        int upperBound = dataMicroSlider.getUpperValue();
        byte[] currentData = new byte[upperBound-lowerBound];
        // this.cantordust.cdprint(String.format("%d %d\n", lowerBound, upperBound));
        for(int i=lowerBound; i <= upperBound-2; i++) {
            currentData[i-lowerBound] = data[i];
        }
        return currentData;
    }

    public void createPopupMenu(JFrame frame){
        popupMenu = new JPopupMenu("Menu");
        JMenuItem pause = new JMenuItem("Pause");

        pause.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {    
                cantordust.cdprint("clicked pause\n");
            }
        });
        
        JMenuItem hilbert = new JMenuItem("Hilbert");
        hilbert.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(!csource.type.equals("classifierPrediction")) {
                    isClassifier = false;
                }
                if(!map.isType("hilbert")) {
                    map = new Hilbert(cantordust, 2, (int)(Math.log(getWindowSize())/Math.log(2)));
                    draw();
                    cantordust.cdprint("clicked hilbert\n");
                } else { cantordust.cdprint("clicked hilbert\nAlready set\n"); }
            }
        });

        JMenuItem linear = new JMenuItem("Linear");
        linear.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {   
                if(!map.isType("linear")) {
                    cantordust.cdprint("clicked linear\n");
                    double x = Math.pow(getWindowSize(),2);
                    map = new Linear(cantordust, 2, x);
                    draw();
                } else { cantordust.cdprint("clicked linear\nAlready set\n"); }
            }
        });

        JMenuItem zorder = new JMenuItem("Zorder");
        zorder.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {   
                if(!map.isType("zorder")) {
                    cantordust.cdprint("clicked zorder\n");
                    double x = Math.pow(getWindowSize(),2);
                    map = new Zorder(cantordust, 2, x);
                    draw();
                } else { cantordust.cdprint("clicked zorder\nAlready set\n"); }
            }
        });

        JMenuItem hcurve = new JMenuItem("HCurve");
        hcurve.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {   
                if(!map.isType("hcurve")) {
                    cantordust.cdprint("clicked hcurve\n");
                    double x = Math.pow(getWindowSize(),2);
                    map = new HCurve(cantordust, 2, x);
                    draw();
                } else { cantordust.cdprint("clicked hcurve\nAlready set\n"); }
            }
        });

        JMenuItem _8bpp = new JMenuItem("8bpp");
        _8bpp.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {   
                if(!csource.isType("8bpp")) {
                    csource = new Color8bpp(cantordust, getCurrentData());
                    draw();
                    cantordust.cdprint("clicked 8bpp\n");
                } else { cantordust.cdprint("clicked 8bpp\nAlready set\n"); }
            }
        });

        JMenuItem _16bpp = new JMenuItem("16bpp ARGB1555");
        _16bpp.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {   
                if(!csource.isType("16bpp ARGB1555")) {
                    csource = new Color16bpp_ARGB1555(cantordust, getCurrentData());
                    draw();
                    cantordust.cdprint("clicked 16bpp\n");
                } else { cantordust.cdprint("clicked 16bpp\nAlready set\n"); }
            }
        });

        JMenuItem _24bpp = new JMenuItem("24bpp Rgb");
        _24bpp.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {   
                if(!csource.isType("24bpp")) {
                    csource = new Color24bpp(cantordust, getCurrentData());
                    draw();
                    cantordust.cdprint("clicked 24bpp\n");
                } else { cantordust.cdprint("clicked 24bpp\nAlready set\n"); }
            }
        });

        JMenuItem _32bpp = new JMenuItem("32bpp Rgb");
        _32bpp.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {   
                if(!csource.isType("32bpp")) {
                    csource = new Color32bpp(cantordust, getCurrentData());
                    draw();
                    cantordust.cdprint("clicked 32bpp\n");
                } else { cantordust.cdprint("clicked 32bpp\nAlready set\n"); }
            }
        });
        
        JMenuItem _64bpp = new JMenuItem("64bpp Rgb");
        _64bpp.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {   
                if(!csource.isType("64bpp")) {
                    csource = new Color64bpp(cantordust, getCurrentData());
                    draw();
                    cantordust.cdprint("clicked 64bpp\n");
                } else { cantordust.cdprint("clicked 64bpp\nAlready set\n"); }
            }
        });

        JMenuItem entropy = new JMenuItem("Entropy");
        entropy.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {   
                if(!csource.isType("entropy")) {
                    csource = new ColorEntropy(cantordust, getCurrentData());
                    draw();
                    cantordust.cdprint("clicked entropy\n");
                } else { cantordust.cdprint("clicked entropy\nAlready set\n"); }
            }
        });

        JMenuItem byteClass = new JMenuItem("Byte Class");
        byteClass.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {   
                if(!csource.isType("class")) {
                    csource = new ColorClass(cantordust, getCurrentData());
                    draw();
                    cantordust.cdprint("clicked byte class\n");
                } else { cantordust.cdprint("clicked byte class\nAlready set\n"); }
            }
        });

        JMenuItem gradient = new JMenuItem("Gradient");
        gradient.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {   
                if(!csource.isType("gradient")) {
                    csource = new ColorGradient(cantordust, getCurrentData());
                    draw();
                    cantordust.cdprint("clicked gradient\n");
                } else { cantordust.cdprint("clicked gradient\nAlready set\n"); }
            }
        });

        JMenuItem spectrum = new JMenuItem("Spectrum");
        spectrum.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {   
                if(!csource.isType("spectrum")) {
                    csource = new ColorSpectrum(cantordust, getCurrentData());
                    draw();
                    cantordust.cdprint("clicked spectrum\n");
                } else { cantordust.cdprint("clicked spectrum\nAlready set\n"); }
            }
        });

        JMenuItem prediction = new JMenuItem("Classifier prediction");
        prediction.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(!csource.isType("classifierPrediction")) {
                    isClassifier = true;
                    csource = new ColorClassifierPrediction(cantordust, getCurrentData());
                    draw();
                    cantordust.cdprint("clicked classifier prediction\n");
                } else { cantordust.cdprint("clicked classifier prediction\nAlready set\n"); }
            }
        });
        
        /*JMenuItem stopClassifier = new JMenuItem("Stop Classifier");
        stopClassifier.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                isClassifier = false;
                popupMenu.remove(stopClassifier);
                cantordust.cdprint("Classifier Stopped\n");
            }
        });*/

        JMenuItem close = new JMenuItem("Close");
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {    
                cantordust.cdprint("clicked close\n");
            }
        });

        JMenuItem classGen = new JMenuItem("Generate Classifier");
        classGen.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {    
                // draw();
                cantordust.cdprint("clicked Generate Classifier\n");
                //isClassifier = true;
                cantordust.initiateClassifier();
                popupMenu.remove(close);
                //popupMenu.add(stopClassifier);
                popupMenu.add(close);
                cantordust.cdprint("generated classifier\n");
            }
        });

        JMenu locality = new JMenu("Locality");
        locality.add(hilbert);
        locality.add(linear);
        locality.add(zorder);
        locality.add(hcurve);
        
        JMenu byteColor = new JMenu("Byte");
        byteColor.add(_8bpp);
        byteColor.add(_16bpp);
        byteColor.add(_24bpp);
        byteColor.add(_32bpp);
        byteColor.add(_64bpp);
        
        JMenu shading = new JMenu("Shading");
        shading.add(byteColor);
        shading.add(byteClass);
        shading.add(entropy);
        shading.add(gradient);
        shading.add(spectrum);
        shading.add(prediction);
        
        popupMenu.add(pause);
        popupMenu.add(locality);
        popupMenu.add(shading);
        popupMenu.add(classGen);
        popupMenu.add(close);
        this.add(popupMenu);
    }

    private void draw() {
        if(!csource.type.equals("classifierPrediction")) {
            isClassifier = false;
        }
        new Thread(() -> {
            // prog = progress.Progress(None);
            this.csource.setData(getCurrentData());
            if(csource.isType("spectrum")){
                this.csource = new ColorSpectrum(cantordust, getCurrentData());
            }
            if(type_plot.equals("unrolled")) {
                this.cantordust.cdprint("building unrolled "+this.map.type+" curve\n");
                drawMap_unrolled(this.map.type, size_hilbert, csource/*, dst, prog*/);
            }
            else if(type_plot.equals("square")){
                this.cantordust.cdprint("Building square "+this.map.type+" curve\n");
                drawMap_square(csource/*, dst, prog*/);
            }
       }).start();
    }

    public void drawMap_square(ColorSource csource/*, String name, prog */) {
        // prog.set_target(Math.pow(size, 2))
        // if(this.map.isType("hilbert")){
        //     cantordust.cdprint("")
        //     this.map = new Hilbert(this.cantordust, 2, (int)(Math.log(getWindowSize())/Math.log(2)));
        // } else if (this.map.isType("zigzag")){
        //     this.map = new ZigZag(this.cantordust, 2, (double)getWindowSize());
        // }
        create2DPixelMap(this.map.dimensions());
        memLoc = new HashMap<Integer, Integer>();
        float step = (float)csource.getLength()/(float)(this.map.getLength());
        for(int i=0;i<this.map.getLength();i++){
            TwoIntegerTuple p = (TwoIntegerTuple)this.map.point(i);
            Rgb c = csource.point((int)(i*step));
            add2DPixel(p, c);
            addMemLoc(i, (int)(i*step));
        }
        convertPixelMapTo1D();
        //c.save(name);
        plotMap(this.map.dimensions());
    }

    public void drawMap_unrolled(String map_type, int size, ColorSource csource/*, String name, prog */) {
        cantordust.cdprint("draw unrolled map in-progress");
    }
    
    public void create2DPixelMap(TwoIntegerTuple dimensions){
        int width = dimensions.get(0);
        int height = dimensions.get(1);
        this.pixelMap2D = new int[height][width*3];
    }
    
    public void add2DPixel(TwoIntegerTuple point, Rgb color){
        /*
        adds 2D pixel to pixelMap2D
        */
        int x = point.get(0)*3;
        int y = point.get(1);
        this.pixelMap2D[y][x] = color.r;
        this.pixelMap2D[y][x+1] = color.g;
        this.pixelMap2D[y][x+2] = color.b;
    }

    public void addMemLoc(int idx, int rloc) {
        /*
        adds Memory relative memory location to XY coordinate
        */
        this.memLoc.put(idx, rloc);
    }
    
    public void flip2DPixlMap(TwoIntegerTuple dimensions){
        /*
        flips every other row in pixels to fit raster image
        */
        int width = dimensions.get(0);
        int height = dimensions.get(1);
        for(int row = 0; row < height; row++){
            if(!(row%2==0)){
                int[] temp = this.pixelMap2D[row];
                int j = width*3-4;
                for(int i = 0; i < width*3; i+=3){
                    this.pixelMap2D[row][i] = temp[j];
                    this.pixelMap2D[row][i+1] = temp[j+1];
                    this.pixelMap2D[row][i+2] = temp[j+2];
                    j-=3;
                }
            }
        }
    }
    
    public void convertPixelMapTo1D(){
        /*
        convert 2D Pixel Map to 1D Pixel Map (pixels)
        */
        this.pixelMap1D = new int[this.pixelMap2D.length*this.pixelMap2D[0].length];
        int x = 0;
        for(int i=0; i<this.pixelMap2D.length; i++) {
            for(int j=0; j<pixelMap2D[i].length; j++) {
                this.pixelMap1D[x] = this.pixelMap2D[i][j];
                x++;
            }
        }
    }
    
    public void plotMap(TwoIntegerTuple dimensions){
        int width = dimensions.get(0);
        int height = dimensions.get(1);
        // int imageSize = width * height * 3;
        // JPanel panel = new JPanel();
        // getContentPane().removeAll();
        // getContentPane().add(panel);
        // panel.add( createImageLabel(this.pixelMap1D, width, height) );
        // panel.revalidate();
        // panel.repaint();
        removeAll();
        add( createImageLabel(this.pixelMap1D, width, height) );
        revalidate();
        repaint();
    }
    
    private JLabel createImageLabel(int[] pixels, int width, int height)
    {
        // int change = size_hilbert - (int)((width - size_hilbert)/2);
        // cantordust.cdprint("ch: "+change+"\n");
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        // cantordust.cdprint("w: "+width+"\nh: "+height+"\n");
        WritableRaster raster = image.getRaster();
        raster.setPixels(0, 0, width, height, pixels);
        label = new JLabel( new ImageIcon(image) );
        return label;
    }

    public static int getWindowSize() {
        return size_hilbert;
    }
}