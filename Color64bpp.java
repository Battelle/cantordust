import java.awt.Color;
public class Color64bpp extends ColorSource { /* see binvis - ColorHilbert class */
    Hilbert map;
    double step;
    public Color64bpp(Cantordust cantordust, byte[] data) {
        super(cantordust, data);
        this.type = "64bpp";
    }

    @Override
    public Rgb getPoint(int x) {
        if(x>=data.length-7){x = data.length-8;}
        int pixel = ((data[x+7] << 56) + (data[x+6] << 48) + (data[x+5] << 40) + (data[x+4] << 32) + (data[x+3] << 24) + (data[x+2] << 16) + (data[x+1] << 8) + data[x]) & 0xFFFFFFFF;
        Color r = new Color(pixel, true);
        Rgb rgb = new Rgb(r.getRed(), r.getGreen(), r.getBlue());
        return rgb;
    }
}