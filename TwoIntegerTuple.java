import java.lang.Error;
public class TwoIntegerTuple extends Tuple{
    Integer x;
    Integer y;
    public TwoIntegerTuple(){
        super(0);
        this.x = null;
        this.y = null;
    }
    public TwoIntegerTuple(Integer x, Integer y) {
        super(x);
        this.x = x; 
        this.y = y;}
    @Override
    public boolean equals(final Object obj) {
        TwoIntegerTuple _o = (TwoIntegerTuple)obj;
        return this.get(0) == _o.get(0) && this.get(1) == _o.get(1);
    }
    @Override
    public String toString(){
        return "["+this.x+", "+this.y+"]";
    }
    
    @Override
    public int hashCode() {return ((x+y)*(x+y+1))/2 + y;}
    @Override
    public int get(int idx){
        if(idx == 0){return this.x;}
        else if(idx == 1){return this.y;}
        else {throw new Error("Index Error for Two Integer Tuple");}
    }
    @Override
    public void set(int idx, int input){
        if(idx == 0){this.x = input;}
        else if(idx == 1){this.y = input;}
        else {throw new Error("Index Error for Two Integer Tuple");}
    }
    public void reverse(){
        int tmp = this.x;
        this.x = this.y;
        this.y = tmp;
    }
}