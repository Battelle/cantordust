public class Rgb extends Tuple{
    int r;
    int g;
    int b;
    public Rgb(int r, int g, int b) {
        super(r);
        assert r <= 255 && g <= 255 && b <= 255;
        assert r >= 0 && g >= 0 && b >= 0;
        this.r =r; this.g = g; this.b = b;
    }
    @Override
    public int get(int idx){
        if(idx == 0){return this.r;}
        else if(idx == 1){return this.g;}
        else if(idx == 2){return this.b;}
        else {throw new Error("Index Error for Rgb: "+idx);}
    }
    @Override
    public void set(int idx, int input){
        assert idx >= 0 && idx <= 255;
        if(idx == 0){this.r = input;}
        else if(idx == 1){this.g = input;}
        else if(idx == 2){this.b = input;}
        else {throw new Error("Index Error for Rgb: "+idx);}
    }
    @Override
    public boolean equals(Object o) {
        Rgb _o = (Rgb)o;
        return r == _o.r && g == _o.g && b == _o.b;
    }
}