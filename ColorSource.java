import java.util.HashMap;
import java.util.TreeSet;

@SuppressWarnings("unchecked")
public abstract class ColorSource {
    protected byte[] data;
    protected Cantordust cantordust;
    protected HashMap<Byte, Integer> symbol_map;
    protected String type;

    ColorSource(Cantordust cantordust, byte[] data/* , block */) {
        this.cantordust = cantordust;
        this.data = data;
        this.symbol_map = new HashMap<Byte, Integer>();
        TreeSet sorted_uniques = new TreeSet();
        for (byte b : data) {
            sorted_uniques.add(b);
        }
        Object[] listed_uniques = sorted_uniques.toArray();
        /*
            we are ignoring unsafe casting to create the symbol_map
            the symbol_map is an array of every unique byte within the loaded program
            mapped in a HashMap to the unsigned byte value.
        */
        for (int i = 0; i < listed_uniques.length; i++) {
            int var = (int)((Byte)listed_uniques[i] & 0xff);
            this.symbol_map.put((Byte)listed_uniques[i], var);
            cantordust.cdprint(String.format("b: %02x : "+var+"\n", (Byte)listed_uniques[i]));
        }
    }

    public boolean isType(String t){
        if(this.type == t){
            return true;
        } else { return false; }
    }

    public void setData(byte[] data){
        this.data = data;
    }

    public int getLength() {
        return data.length;
    }

    public Rgb point(int x) {
        // implement blocksize
        /*
            * if self.block and (self.block[0]<=x<self.block[1]): return self.block[2]
            * else:
            */
        return getPoint(x);
    }

    public abstract  Rgb getPoint(int x);
    }