// hilbert
import java.awt.*;
import java.util.ArrayList;

import java.lang.Error;

public class Hilbert extends Scurve{
    protected int dimension;
    protected int order;
    protected Utils utils;
    public Hilbert(Cantordust cantordust){
        super(cantordust);
        this.type = "hilbert";
    }
    public Hilbert(Cantordust cantordust, int dimension, int order){
        super(cantordust);
        this.type = "hilbert";
        this.dimension = dimension;
        this.utils = new Utils(this.cantordust);
        double size = Math.pow(2, dimension*order);
        float x = (float)Math.log(size)/(float)Math.log(2);
        if(!((float)(x)/dimension == (int)(x)/dimension)){
            throw new Error("Size does not fit Hilbert curve of dimension.");
        } else {
            this.order = order;
        }
        this.cantordust.cdprint("dimension: "+dimension+"\norder: "+order+"\n");
    }
    public Hilbert(Cantordust cantordust, int dimension, double size) {
        super(cantordust);
        this.type = "hilbert";
        this.dimension = dimension;
        this.utils = new Utils(cantordust);
        this.cantordust.cdprint("checking size.\n");
        float x = (float)Math.log(size)/(float)Math.log(2);
        if(!((float)(x)/dimension == (int)(x)/dimension)){
            throw new Error("Size does not fit Hilbert curve of dimension.");
        } else {
            this.order = (int)(x/this.dimension);
        }
    }
    @Override
    public Hilbert fromSize(String curve, int dimension, int size){
        this.cantordust.cdprint("checking size.\n");
        float x = (float)Math.log(size)/(float)Math.log(2);
        if(!((float)(x)/dimension == (int)(x)/dimension)){
            throw new Error("Size does not fit Hilbert curve of dimension.");
        }
        return new Hilbert(cantordust, dimension, (int)(x/dimension));
    }

    public int transform(int entry, int direction, int width, int x){
        assert x < (int)(Math.pow(2, width));
        assert entry < (int)(Math.pow(2, width));
        return this.utils.rrot((x^entry), direction+1, width);
    }

    public int itransform(int entry, int direction, int width, int x){
        /*
            Inverse transform - we simply reverse the operations in tranform.
        */
        assert x < Math.pow(2, width);
                assert entry < Math.pow(2, width);
        this.cantordust.cdprint("entry: "+entry+"\td:"+direction+"\n");
        return this.utils.lrot(x, direction+1, width)^entry;
        // There is an error in the Hamilton paper's formulation of the inverse
        // transform in Lemma 2.12. The correct restatement as a transform is as follows:
        // return transform(rrot(entry, direction+1, width), width-direction-2, width, x)
    }

    // Hilbert calculations
    public int direction(int x, int n){
        assert x < (int)Math.pow(2, n);
        if(x==0){
            return 0;
        } else if(x%2==0){
            return this.utils.tsb(x-1, n)%n;
        } else {
            return this.utils.tsb(x, n)%n;
        }
    }
    public int entry(int x){
        if(x==0){
            return 0;
        } else {
            return this.utils.graycode(2*((x-1)/2));
        }
    }
    public Tuple hilbert_point(int h) {
        /*
            Convert an index on the Hilbert curve of the specified dimension and
            order to a set of point coordinates.
        */
        //     The bit widths in this function are:
        //        p[*]  - order
        //        h     - order*dimension
        //        l     - dimension
        //        e     - dimension
        int hwidth = this.order*this.dimension;
        int e = 0;
        int d = 0;
        Tuple p;
        if(this.dimension == 2){p = new TwoIntegerTuple(0, 0);}
        else if(this.dimension == 3){p = new Rgb(0, 0, 0);}
        else { throw new Error("Dimension is incorrect"); }
        for(int i=0;i<order;i++){
            int w = this.utils.bitrange(h, hwidth, i*this.dimension, i*this.dimension+this.dimension);
            int l = this.utils.graycode(w);
            l = itransform(e, d, this.dimension, l);
            for(int j=0;j<this.dimension;j++){
                int b = this.utils.bitrange(l, this.dimension, j, j+1);
                p.set(j, this.utils.setbit(p.get(j), this.order, i, b));
            }
            e = e ^ this.utils.lrot(entry(w), d+1, this.dimension);
            d = (d + direction(w, this.dimension) + 1)%this.dimension;
        }
        return p;
    }

    public int hilbert_index(TwoIntegerTuple p){
        /*
            Convert a set of point coordinates to a point on the Hilbert curve
            given a specified order and dimension.
        */
        int h = 0; 
        int e = 0; 
        int d = 0;
        for(int i=0;i<this.order;i++){
            int l = 0;
            for(int x=0;x<this.dimension;x++){
                int b = this.utils.bitrange(p.get(this.dimension-x-1), this.order, i, i+1);
                l |= b<<x;
            }
            l = transform(e, d, this.dimension, l);
            int w = this.utils.igraycode(l);
            e = e ^ this.utils.lrot(entry(w), d+1, this.dimension);
            d = (d + direction(w, this.dimension) + 1)%this.dimension;
            h = (h<<this.dimension)|w;
        }
        return h;
    }

    // basic class functions
    @Override
    public int getLength(){
        return (int)Math.pow(2, this.dimension*this.order);
    }
    @Override
    public Tuple point(int idx){
        return this.hilbert_point(idx);
    }
    @Override
    public int index(TwoIntegerTuple p){
        return this.hilbert_index(p);
    }
    @Override
    public TwoIntegerTuple dimensions(){
        /*
            Size of this curve in each dimension.
        */
        int x = (int)Math.ceil(Math.pow(getLength(), 1/(float)(this.dimension)));
        return new TwoIntegerTuple(x, x);
    }
}