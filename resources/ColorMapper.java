package resources;

import java.awt.Color;

public abstract class ColorMapper {
    protected GhidraSrc cantordust;
    protected byte[] data;

    ColorMapper(GhidraSrc cantordust) {
        this.cantordust = cantordust;
        this.data = cantordust.getData();
    }

    abstract Color colorAtIndex(int index);
}
