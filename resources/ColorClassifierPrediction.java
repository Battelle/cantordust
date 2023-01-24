package resources;

import java.awt.*;

public class ColorClassifierPrediction extends ColorSource {
    Hilbert map;
    double step;

    private ClassifierModel classifier;

    public ColorClassifierPrediction(GhidraSrc cantordust, byte[] data) {
        super(cantordust, data);
        this.type = "classifierPrediction";
        this.classifier = cantordust.getClassifier();
    }

    @Override
    public Rgb getPoint(int x) {
        this.classifier = cantordust.getClassifier();
        int classification = this.classifier.classAtIndex(x);
        double c = (double)classification / (double)ClassifierModel.classes.length;
        double waveLength = 400 + c*(800-400);
        Color color = WavelengthToRGB.waveLengthToRGB(waveLength);
        return new Rgb(color.getRed(), color.getGreen(), color.getBlue());
    }
}
