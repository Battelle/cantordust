package resources;

import java.awt.Color;

public abstract class ColorMapper {
    protected Host cantordust;
    protected byte[] data;

    ColorMapper(Host cantordust) {
        this.cantordust = cantordust;
        this.data = cantordust.getData();
    }

    abstract Color colorAtIndex(int index);
}
