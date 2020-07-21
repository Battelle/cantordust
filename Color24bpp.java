import java.awt.Color;
public class Color24bpp extends ColorSource { /* see binvis - ColorHilbert class */
    Hilbert map;
    double step;
    public Color24bpp(Cantordust cantordust, byte[] data) {
        super(cantordust, data);
        this.type = "24bpp";
    }

    @Override
    public Rgb getPoint(int x) {
        if(x>=data.length-3){x = data.length-4;}
        int pixel = ((data[x+2] << 16) + (data[x+1] << 8) + data[x]) & 0xFFFFFFFF;
        Color r = new Color(pixel);
        Rgb rgb = new Rgb(r.getRed(), r.getGreen(), r.getBlue());
        return rgb;
    }
}