public class ThreeByteTuple {
    byte x;
    byte y;
    byte z;
    public ThreeByteTuple(byte x, byte y, byte z) {
        this.x = x; this.y = y; this.z = z;
    }

    @Override
    public boolean equals(Object o) {
        ThreeByteTuple _o = (ThreeByteTuple)o;
        return x == _o.x && y == _o.y && z == _o.z;
    }
    
    @Override
    public int hashCode() {
        int xy = ((x+y)*(x+y+1))/2 + y;
        return ((xy + z)*(xy+z+1))/2 + z;
    }
}
