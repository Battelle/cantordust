public class ColorEntropy extends ColorSource {
    protected Utils utils;
    public ColorEntropy(Cantordust cantordust, byte[] data) {
        super(cantordust, data);
        this.type = "entropy";
        this.utils = new Utils(this.cantordust);
    }

    public double curve(double v) {
        double f = Math.pow((4 * v - 4*Math.pow(v, 2)), 4);
        f = Math.max(f, 0);
        return f;
    }

    @Override
    public Rgb getPoint(int x) {
        double e = this.utils.entropy(this.data, 32, x, this.symbol_map.size());
        this.cantordust.cdprint("offset: " + x + "\n");
        this.cantordust.cdprint("entropy: " + e + "\n");
        double r;
        if (e > 0.5) {
            r = curve(e - 0.5);
        } else {
            r = 0;
        }
        double b = Math.pow(e, 2);
        this.cantordust.cdprint("r: "+r+"\n");
        this.cantordust.cdprint("b: "+b+"\n");
        return new Rgb(
            (int)(255 * r), 
            0, 
            (int)(255 * b)
        );
    }
}