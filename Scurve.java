//scurve __init__
import java.lang.Error;

public class Scurve{
    protected Cantordust cantordust;
    protected int size = 512;
    protected byte[] data;
    protected String type;
    // private HashMap<String,Scurve> curveMap = new HashMap<String, Scurve>();
    public Scurve(Cantordust cantordust){
        this.cantordust = cantordust;
        data = cantordust.getData();
        // curveMap.put("hcurve", new Hcurve());
        // curveMap.put("zigzag", new ZigZag());
        // curveMap.put("zorder", new ZOrder());
        // curveMap.put("natural", new Natural());
        // curveMap.put("gray", new GrayCurve());
        // curveMap.put("hilbert", new Hilbert(cantordust));
    }
    public void setWidth(int width){}
    public void setHeight(int height){}
    public int getWidth(){return this.size;}
    public int getHeight(){return this.size;}
    public boolean isType(String t){
        if(this.type == t){
            return true;
        } else { return false; }
    }
    public Scurve fromSize(String curve, int dimension, int size){
        return new Hilbert(this.cantordust, dimension, size);
    }
    public int getLength(){
        return 0;
    }
    public Tuple point(int idx){
        if(idx >= getLength()){
            throw new Error("Index Error");
        }
        return new TwoIntegerTuple(0, 0);
    }
    public int index(TwoIntegerTuple p){
        return 0;
    }
    public TwoIntegerTuple dimensions(){
        return new TwoIntegerTuple(0, 0);
    }
}