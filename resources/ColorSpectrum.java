package resources;

public class ColorSpectrum extends ColorSource { /* see binvis - ColorHilbert class */
    Hilbert map;
    double step;
    public ColorSpectrum(GhidraSrc cantordust, byte[] data) {
        super(cantordust, data);
        this.type = "spectrum";
        this.map = new Hilbert(this.cantordust, 3, (Math.pow(256, 3)));
        this.step = map.getLength()/(double)(symbol_map.size());
    }

    @Override
    public Rgb getPoint(int x) {
        int c = symbol_map.get(data[x]);
        return (Rgb)map.point((int)(c*this.step));
    }
}