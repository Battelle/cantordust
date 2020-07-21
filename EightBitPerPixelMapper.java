import java.awt.Color;

public class EightBitPerPixelMapper extends ColorMapper {
    EightBitPerPixelMapper(Cantordust cantordust) {
        super(cantordust);
    }
    public Color colorAtIndex(int index) {
        return new Color(0, data[index] & 0xff,0);
    }
}
