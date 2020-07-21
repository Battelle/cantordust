/* byte vector of fixed length */

public class VectorN {
    private byte[] v;
    private int n;

    public VectorN(byte[] data) {
        this.n = data.length;
        v = new byte[data.length];
        System.arraycopy(data, 0, v, 0, data.length);
    }

    public VectorN(int n) {
        this.n = n;
        v = new byte[n];
    }

    @Override
    public int hashCode() {
        int hash = n;
        for(byte b : v) {
            hash = (hash << 5) ^ (hash >> 27) ^ b;
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        assert(obj instanceof VectorN);
        VectorN right = (VectorN)obj;
        if(n == right.n) {
            for(int i = 0; i < n; i++) {
                if(v[i] != right.v[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public byte[] toArray() {
        byte[] ret = new byte[n];
        System.arraycopy(v, 0, ret, 0, n);
        return ret;
    }

    public byte getAt(int index) {
        assert(index >= 0 && index < n);
        return v[index];
    }

    public void setAt(int index, byte val) {
        assert(index >= 0 && index < n);
        v[index] = val;
    }

    public int getN() {return n;}

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("< ");
        for(int i = 0; i < v.length; i++) {
            builder.append(String.format("%x ", v[i]));
        }
        builder.append(">");
        return builder.toString();
    }
}
