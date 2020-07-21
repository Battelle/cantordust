import java.awt.Color;
public class Color16bpp_ARGB1555 extends ColorSource { /* see binvis - ColorHilbert class */
    Hilbert map;
    double step;
    public Color16bpp_ARGB1555(Cantordust cantordust, byte[] data) {
        super(cantordust, data);
        this.type = "16bpp ARGB1555";
    }

    @Override
    public Rgb getPoint(int x) {
        if(x>=data.length-1){x = data.length-2;}
        int alpha = (data[x+1] & 0x80) >> 7;
        float red = ((data[x+1] & 0x7C) >> 2)/(0x1F);
        float green = (((data[x+1] & 0x03) << 3) + ((data[x] & 0xE0) >> 5))/(0x1F);
        float blue  = (data[x] & 0x1F)/(0x1F); 
        Color r = new Color(red, green, blue, alpha);
        Rgb rgb = new Rgb(r.getRed(), r.getGreen(), r.getBlue());
        return rgb;
    }
}