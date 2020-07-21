import java.awt.Color;
public class Color8bpp extends ColorSource { /* see binvis - ColorHilbert class */
    Hilbert map;
    double step;
    public Color8bpp(Cantordust cantordust, byte[] data) {
        super(cantordust, data);
        this.type = "8bpp";
    }

    @Override
    public Rgb getPoint(int x) {
        int unsignedByte = data[x] & 0xFF;
        Color r = new Color(0, unsignedByte, 0);
        Rgb rgb = new Rgb(r.getRed(), r.getGreen(), r.getBlue());
        return rgb;
    }
}