import java.awt.*;
import java.util.HashMap;

public class ColorClassifierPrediction extends ColorSource {
    Hilbert map;
    double step;

    private ClassifierModel classifier;

    public ColorClassifierPrediction(Cantordust cantordust, byte[] data) {
        super(cantordust, data);
        this.type = "classifierPrediction";
        classifier = cantordust.getClassifier();
    }

    @Override
    public Rgb getPoint(int x) {
        int unsignedByte = data[x] & 0xFF;
        Color r = new Color(0, unsignedByte, 0);
        Rgb rgb = new Rgb(r.getRed(), r.getGreen(), r.getBlue());
        ClassifierModel classifier = cantordust.getClassifier();
        int classification = classifier.classAtIndex(x);
        double c = (double)classification / (double)classifier.classes.length;
        double waveLength = 400 + c*(800-400);
        Color color = WavelengthToRGB.waveLengthToRGB(waveLength);
        return new Rgb(color.getRed(), color.getGreen(), color.getBlue());
        //return new Rgb((int)Math.floor(c*255.0), (int)Math.floor(c*255.0), (int)Math.floor(c*255.0));
    }
}
