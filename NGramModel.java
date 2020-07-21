import java.util.HashMap;

public class NGramModel {
    ExponentialNotation MINIMUM_FREQUENCY_MULTIPLIER =  new ExponentialNotation(1, -20);

    private int n;
    private int modelEntries;
    private HashMap<VectorN, Double> model = new HashMap<>();

    public NGramModel(byte[] data, int startIndex, int length, int n) {
        GenerateModel(data, startIndex, length, n);
    }

    public NGramModel(byte[] data, int n) {
        GenerateModel(data, 0, data.length, n );
    }

    private void recordInstance(VectorN v) {
        if(model.containsKey(v)) {
            model.put(v, model.get(v) + 1);
        } else {
            model.put(v, 1.0);
        }
    }

    public void GenerateModel(byte[] data, int startIndex, int length, int n) {
        this.n = n;

        // count occurrences of each existing n-gram
        for(int i = startIndex; i < startIndex + length; i++) {
            if(i + n < startIndex + length) {
                VectorN v = new VectorN(n);
                for(int k = 0; k < n; k++) {
                    v.setAt(k, data[i+k]);
                }
                if(model.containsKey(v)) {
                    model.put(v, model.get(v) + 1);
                } else {
                    model.put(v, 1.0);
                }
            }
        }

        // calculate probabilities
        modelEntries = data.length - n + 1;
        for(VectorN entry : model.keySet()) {
            model.put(entry, model.get(entry) / (double)modelEntries);
        }
    }

    public ExponentialNotation EvaluateClassification(NGramModel templateModel) {
        // ensure dimensionality is consistent
        if(n != templateModel.n) {throw new IllegalArgumentException("inconsistent dimensions");}

        ExponentialNotation p = new ExponentialNotation(1);

        for(VectorN v : model.keySet()) {
            int k = (int)(model.get(v) * modelEntries + 0.5);
            if(templateModel.model.containsKey(v)) {
                double pClass = templateModel.model.get(v);
                p = p.multiply(MathUtils.fastPow(new ExponentialNotation(pClass), k));

            } else {
                p = p.multiply((new ExponentialNotation(1).divide(new ExponentialNotation(modelEntries))).multiply(MINIMUM_FREQUENCY_MULTIPLIER));
            }
        }
        return p;
    }

    // mostly for debug purposes
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("%d entries: \n", modelEntries));
        for(VectorN key : model.keySet()) {
            builder.append(String.format("\t%s : %d (%f)\n", key.toString(), (int)(model.get(key)*modelEntries), model.get(key)));
        }
        return builder.toString();
    }
}