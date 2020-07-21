public class ColorGradient extends ColorSource {
    public ColorGradient(Cantordust cantordust, byte[] data) {
        super(cantordust, data);
        this.type = "gradient";
    }

    @Override
    public Rgb getPoint(int x) {
        double c = (int)(data[x])/255.0;
        return new Rgb(
            (int)(255*c), 
            (int)(255*c), 
            (int)(255*c));
    }
}