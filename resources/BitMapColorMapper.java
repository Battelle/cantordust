package resources;

import java.awt.Color;

public class BitMapColorMapper extends ColorMapper {

    public BitMapColorMapper(GhidraSrc cantordust) {
    	super(cantordust);
        data = cantordust.getData();
    }

    public Color colorAtIndex(int index) {
        int unsignedByte = data[index] & 0xFF;
        return new Color(0, unsignedByte, 0);
    }
}
