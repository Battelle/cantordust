import java.util.ArrayList;

// Linear
public class Linear extends Scurve{
    protected int dimension;
    protected int width;
    protected int height;
    protected int size;
    public Linear(Cantordust cantordust){
        super(cantordust);
        this.type = "linear";
    }
    public Linear(Cantordust cantordust, int dimension, double size) {
        super(cantordust);
        this.type = "linear";
        this.cantordust.cdprint("checking zig zag size.\n");
        double x = Math.ceil(Math.pow(size, 1/(double)dimension));
        double y = Math.pow(x, dimension);
        if(!(Math.pow(x, dimension) == size)){
            throw new Error("Size does not fit a square Linear curve");
        }
        this.dimension = dimension;
        this.size = (int)x;
        this.width = this.size;
        this.height = this.size;
    }
    @Override
    public void setWidth(int width){
        this.width = width;
    }
    @Override
    public void setHeight(int height){
        this.height = height;
    }
    @Override
    public int getWidth(){
        return this.width;
    }
    @Override
    public int getHeight(){
        return this.height;
    }
    @Override
    public int getLength(){
        return (int)Math.pow(this.size, this.dimension);
    }
    @Override
    public TwoIntegerTuple dimensions(){
        /*
            Size of this curve in each dimension.
        */
        return new TwoIntegerTuple(this.width, this.height);
    }
    @Override
    public int index(TwoIntegerTuple p){
        int idx = 0;
        boolean flip = false;
        int fi;
        ArrayList<Integer> arrlist = new ArrayList<Integer>(2);
        p.reverse();
        arrlist.add(p.get(0));
        arrlist.add(p.get(1));
        for(int power=0;power<2;power++){
            int i = arrlist.get(power);
            power = this.dimension-power-1;
            if(flip){
                fi = this.size-i-1;
            } else{
                fi = i;
            }
            int v = fi * (int)Math.pow(this.size, power);
            idx += v;
            if(i%2==1){
                flip = !flip;
            }
        }
        return idx;
    }
    @Override
    public TwoIntegerTuple point(int idx){
        // cantordust.cdprint("\n----\n");
        // TwoIntegerTuple p = new TwoIntegerTuple();
        ArrayList<Integer> arrlist = new ArrayList<Integer>(2);
        boolean flip = false;
        for(int i=this.dimension-1;i>-1;i-=1){
            int v = idx/(int)(Math.pow(this.size, i));
            if(i>0){
                idx = idx - (int)(Math.pow(this.size, i)*v);
            }
            if(flip){
                v = this.size-1-v;
            }
            arrlist.add((int)v);
            if(v%2==1){
                flip = !flip;
            }
        }
        TwoIntegerTuple p = new TwoIntegerTuple(arrlist.get(0), arrlist.get(1));
        p.reverse();
        // cantordust.cdprint("p: "+p.get(0)+", "+p.get(1)+"\n");
        return p;
    }
}