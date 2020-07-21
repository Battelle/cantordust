import java.awt.Color;

public abstract class ColorMapper {
    protected Cantordust cantordust;
    protected byte[] data;

    ColorMapper(Cantordust cantordust) {
        this.cantordust = cantordust;
        this.data = cantordust.getData();
    }

    abstract Color colorAtIndex(int index);
}
