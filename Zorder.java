import java.util.ArrayList;

// Zorder
public class Zorder extends Scurve{
    protected int dimension;
    protected int bits;
    protected Utils utils;
    public Zorder(Cantordust cantordust){
        super(cantordust);
        this.type = "zorder";
    }
    public Zorder(Cantordust cantordust, int dimension, double size) {
        super(cantordust);
        this.type = "zorder";
        this.utils = new Utils(this.cantordust);
        this.cantordust.cdprint("checking zorder size.\n");
        double x = Math.log(size)/Math.log(2);
        double bits = x/dimension;
        if(!(bits == (int)bits)){
            throw new Error("Size does not fit a square Zorder curve");
        }
        this.dimension = dimension;
        this.bits = (int)bits;
    }
    @Override
    public int getLength(){
        return (int)Math.pow(2, this.bits*this.dimension);
    }
    @Override
    public TwoIntegerTuple dimensions(){
        /*
            Size of this curve in each dimension.
        */
        int x = (int)Math.pow(2, this.bits);
        return new TwoIntegerTuple(x, x);
    }
    @Override
    public int index(TwoIntegerTuple p){
        int idx = 0;
        ArrayList<Integer> arrlist = new ArrayList<Integer>(2);
        p.reverse();
        arrlist.add(p.get(0));
        arrlist.add(p.get(1));
        int iwidth = this.bits*this.dimension;
        for(int i=0;i<iwidth;i++){
            int bitoff = this.bits-(i/this.dimension)-1;
            int poff = this.dimension-(i%this.dimension)-1;
            int b = this.utils.bitrange(arrlist.get(poff), this.bits, bitoff, bitoff+1) << i;
            idx |= b;
        }
        return idx;
    }
    @Override
    public TwoIntegerTuple point(int idx){
        // cantordust.cdprint("\n----\n");
        // TwoIntegerTuple p = new TwoIntegerTuple();
        ArrayList<Integer> arrlist = new ArrayList<Integer>(2);
        arrlist.add(0);
        arrlist.add(0);
        int iwidth = this.bits*this.dimension;
        for(int i=0;i<iwidth;i++){
            int b = this.utils.bitrange(idx, iwidth, i, i+1) << (iwidth-i-1)/this.dimension;
            int x = arrlist.get(i%this.dimension);
            arrlist.set(i%this.dimension, x |= b);
        }
        TwoIntegerTuple p = new TwoIntegerTuple(arrlist.get(0), arrlist.get(1));
        p.reverse();
        // cantordust.cdprint("p: "+p.get(0)+", "+p.get(1)+"\n");
        return p;
    }
}