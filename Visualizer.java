import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JPanel;
import javax.swing.JSlider;

public abstract class Visualizer extends JPanel {
    protected MainInterface mainInterface;
    protected Cantordust cantordust;
    protected double blockWidth;
    protected RangeSlider dataMacroSlider;
    protected RangeSlider dataMicroSlider;
    protected JSlider dataRangeSlider;
    protected ColorMapper colorMapper;

    public Visualizer(int windowSize, Cantordust cantordust) {
        this.mainInterface = cantordust.mainInterface;
        this.cantordust = cantordust;
        dataMacroSlider = mainInterface.macroSlider;
        dataMicroSlider = mainInterface.microSlider;
        dataRangeSlider = mainInterface.dataSlider;
        setBackground(Color.black);
        dataMacroSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if(!dataMacroSlider.getValueIsAdjusting()) {
                    repaint();
                }
            }
        });
        dataMicroSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if(!dataMicroSlider.getValueIsAdjusting()) {
                    repaint();
                }
            }
        });
        if(dataRangeSlider != null){
            dataRangeSlider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    if(!dataRangeSlider.getValueIsAdjusting()) {
                        repaint();
                    }
                }
            });
        }
    }

    // Special constructor for initialization of plugin
    public Visualizer(int windowSize, Cantordust cantordust, MainInterface mainInterface) {
        this.mainInterface = mainInterface;
        this.cantordust = cantordust;
        dataMacroSlider = mainInterface.macroSlider;
        dataMicroSlider = mainInterface.microSlider;
        dataRangeSlider = mainInterface.dataSlider;
        setBackground(Color.black);
        dataMacroSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if(dataMacroSlider.getValueIsAdjusting()) {
                    repaint();
                }
            }
        });
        dataMicroSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if(dataMicroSlider.getValueIsAdjusting()) {
                    repaint();
                }
            }
        });
        if(dataRangeSlider != null){
            dataRangeSlider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    if(!dataRangeSlider.getValueIsAdjusting()) {
                        repaint();
                    }
                }
            });
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
    }

    public void setColorMapper(ColorMapper mapper) {
        colorMapper = mapper;
    }
}