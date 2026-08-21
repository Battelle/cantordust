package resources;

import java.awt.*;

/**
 * Shades each block by the content class the classifier assigned it.
 *
 * The classifier takes seconds to build and may not exist at all - selecting this
 * shading used to dereference it unconditionally, so choosing it before
 * generating the classifier threw a NullPointerException out of the paint. An
 * unclassified block is now simply drawn dark.
 */
public class ColorClassifierPrediction extends ColorSource {

    private static final Rgb UNCLASSIFIED = new Rgb(28, 28, 28);

    public ColorClassifierPrediction(GhidraSrc cantordust, byte[] data) {
        super(cantordust, data);
        this.type = "classifierPrediction";
    }

    @Override
    public Rgb getPoint(int x) {
        ClassifierModel model = cantordust.getClassifier();
        if(model == null) {
            return UNCLASSIFIED;
        }
        int classification = model.classAtIndex(x);
        if(classification < 0) {
            return UNCLASSIFIED;
        }
        double c = (double) classification / (double) ClassifierModel.classes.length;
        double waveLength = 400 + c * (800 - 400);
        Color color = WavelengthToRGB.waveLengthToRGB(waveLength);
        return new Rgb(color.getRed(), color.getGreen(), color.getBlue());
    }
}
