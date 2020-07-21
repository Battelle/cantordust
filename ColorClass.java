public class ColorClass extends ColorSource {
    public ColorClass(Cantordust cantordust, byte[] data) {
        super(cantordust, data);
        this.type = "class";
    }

    @Override
    public Rgb getPoint(int x) {
        int c = (int)this.data[x];
        if(c == 0){
            return new Rgb(0, 0, 0);
        } else if(c == 255){
            return new Rgb(255, 255, 255);
        } else if ((char)c >= 32 && (char)c < 127){
            return new Rgb(55, 126, 184);
        } else { return new Rgb(228, 26, 28); }
    }
}