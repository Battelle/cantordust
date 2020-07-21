public class TwoByteTuple {
    byte x;
    byte y;
    public TwoByteTuple(byte x, byte y) {this.x = x; this.y = y;}
    @Override
    public boolean equals(Object o) {
        TwoByteTuple _o = (TwoByteTuple)o;
        return x == _o.x && y == _o.y;
    }
    @Override
    public int hashCode() {
        return ((x+y)*(x+y+1))/2 + y;
    }
}
