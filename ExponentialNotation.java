/* A class for noromalized exponential notation. Allows for higher precision than using primitives. */

public class ExponentialNotation implements Comparable<ExponentialNotation>{
    // Exponential Notation with base 10
    private double coefficient;
    private long exponent;

    public ExponentialNotation(double coefficient, long exponent) {
        this.coefficient = coefficient;
        this.exponent = exponent;
        this.normalize();
    }

    public ExponentialNotation(double coefficient) {
        this.coefficient = coefficient;
        this.exponent = 0;
        this.normalize();
    }

    public ExponentialNotation(ExponentialNotation e) {
        coefficient = e.coefficient;
        exponent = e.exponent;
    }

    private void normalize() {
        while (true) {
            double ma = Math.abs(coefficient);
            if(ma == 0) {
                exponent = 0;
                break;
            }
            else if(ma >= 10) {
                coefficient /= 10;
                exponent++;
            }
            else if(ma < 1) {
                coefficient *= 10;
                exponent--;
            } else {
                break;
            }
        }
    }

    ExponentialNotation multiply(ExponentialNotation expNot) {
        return new ExponentialNotation(coefficient*expNot.coefficient, exponent+expNot.exponent);
    }

    ExponentialNotation divide(ExponentialNotation expNot) {
        return new ExponentialNotation(coefficient / expNot.coefficient, exponent - expNot.exponent);
    }

    ExponentialNotation minus(ExponentialNotation expNot) {
        if(exponent > expNot.exponent) {
            return new ExponentialNotation(coefficient - expNot.coefficient / Math.pow(10, (exponent - expNot.exponent)), exponent);
        } else if(expNot.exponent > exponent) {
            return new ExponentialNotation(expNot.coefficient - coefficient / Math.pow(10, expNot.exponent - exponent), expNot.exponent);
        } else {
            return new ExponentialNotation(coefficient - expNot.coefficient, exponent);
        }
    }

    ExponentialNotation plus(ExponentialNotation expNot) {
        if (exponent > expNot.exponent) {
            return new ExponentialNotation(coefficient + expNot.coefficient / Math.pow(10, (exponent - expNot.exponent)), exponent);
        }
        else if (expNot.exponent > exponent)
        {
            return new ExponentialNotation(expNot.coefficient + coefficient / Math.pow(10, (expNot.exponent - exponent)), expNot.exponent);
        }
        else
        {
            return new ExponentialNotation(coefficient + expNot.coefficient, exponent);
        }
    }

    @Override
    public String toString() {
        return String.format("%f*10^%d", coefficient, exponent);
    }

    @Override
    public int compareTo(ExponentialNotation right) {
        assert(right != null);
        ExponentialNotation left = this;
        // check for same object
        if(left == right) {
            return 0;
        }
        if (right.coefficient == 0)
        {
            if (left.coefficient < 0)
            {
                return -1;
            }
            else if (left.coefficient > 0)
            {
                return 1;
            }
            else
            {
                return 0;
            }
        }
        else if (left.coefficient == 0)
        {
            if (right.coefficient < 0)
            {
                return -1;
            }
            else if (right.coefficient > 0)
            {
                return 1;
            }
            else
            {
                return 0;
            }
        }
        // requirements for normalized numbers
        else if (left.coefficient < 0 && right.coefficient >= 0)
        {
            return -1;
        }
        else if (left.coefficient >= 0 && right.coefficient < 0)
        {
            return 1;
        }
        else if (left.exponent < right.exponent)
        {
            return -1;
        }
        else if (left.exponent > right.exponent)
        {
            return 1;
        }
        else if (left.coefficient < right.coefficient)
        {
            return -1;
        }
        else if (left.coefficient > right.coefficient)
        {
            return 1;
        }
        else
        {
            return 0;
        }
    }

    public boolean equalTo(ExponentialNotation expNot) {return this.compareTo(expNot) == 0;}
    public boolean lessThan(ExponentialNotation expNot) {return this.compareTo(expNot) < 0;}
    public boolean greaterThan(ExponentialNotation expNot) {return this.compareTo(expNot) > 0;}

    public static double getDouble(ExponentialNotation expNot) {
        double coefficient = expNot.coefficient;
        long exponent = expNot.exponent;

        if (exponent == 0)
        {
            coefficient = 1;
            exponent = 1;
        }
        else if (exponent < 0)
        {
            while (exponent <= -1)
            {
                exponent++;
                coefficient /= 10;
            }
        }
        else
        {
            while (exponent >= 1)
            {
                exponent--;
                coefficient *= 10;
            }
        }

        return coefficient;
    }

    @Override
    public int hashCode() {
        return (int)((((coefficient+exponent)*(coefficient+exponent+1))/2 + exponent));
    }
}
