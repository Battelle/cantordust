public class MathUtils {
    public static int clamp(int value, int min, int max) {return Math.min(Math.max(min, value), max);}

    public static double fastPow(double x, int n) {
        if(n == 0) return 1;
        if(n == 1) return x;

        if(n == Integer.MIN_VALUE) {throw new IllegalArgumentException("n");}

        boolean invert = false;
        if(n < 0) {
            n = -n;
            invert = true;
        }

        int k = 31;

        while((n <<= 1) >= 0) {k--;}

        double r = x;
        while(--k > 0) {
            r = r * r * (((n <<= 1) < 0) ? x : 1);
        }

        if(invert) {
            return 1 / r;
        }
        return r;
    }

    public static ExponentialNotation fastPow(ExponentialNotation x, int n) {
        if(n == 0) return new ExponentialNotation(1, 0); // 1
        if(n == 1) return x;

        if(n == Integer.MIN_VALUE) {throw new IllegalArgumentException("n");}

        boolean invert = false;
        if(n < 0) {
            n = -n;
            invert = true;
        }

        int k = 31;
        while((n <<= 1) >= 0) {k--;}

        ExponentialNotation r = new ExponentialNotation(x);
        while(--k > 0) {
            r = r.multiply(r);
            if ((n <<= 1) < 0) {
                r = r.multiply(x);
            } else {
                r = r.multiply(new ExponentialNotation(1));
            }
        }

        if(invert) {
            return new ExponentialNotation(1).divide(r);
        }
        return r;
    }
}
