public class Tuple{
    int x;
    public Tuple(int x) {
        this.x = x;
    }
    @Override
    public boolean equals(Object o) {
        return false;
    }
    
    @Override
    public int hashCode() {return x;}
    public int get(int idx){
        assert idx == 0;
        return this.x;
    }
    public void set(int idx, int input){
        if(idx == 0){this.x = input;}
    }
}