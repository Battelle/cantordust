package resources;

/** The classifier: safe to use before it is built, builds, and labels every block. */
public class ClassFinal {
    public static void main(String[] a) throws Exception {
        T.boot(a[0]);
        byte[] data = T.host.getData();
        if (T.host.isClassifierReady() || T.host.getClassifier() != null) {
            System.out.println("  classifier reported ready before build");
            System.exit(1);
        }
        Rgb r = new ColorClassifierPrediction(T.host, data).getPoint(1234);
        System.out.printf("  getPoint before build: rgb(%d,%d,%d) - no exception%n", r.r, r.g, r.b);
        long t0 = System.nanoTime();
        T.host.initiateClassifier();
        System.out.printf("  %,d bytes classified in %.0f ms; ready=%s%n", data.length, (System.nanoTime() - t0) / 1e6, T.host.isClassifierReady());
        ClassifierModel m = T.host.getClassifier();
        int blocks = (data.length + ClassifierModel.BLOCK_SIZE - 1) / ClassifierModel.BLOCK_SIZE;
        int labelled = 0;
        for (int i = 0; i < data.length; i += ClassifierModel.BLOCK_SIZE) {
            if (m.classAtIndex(i) >= 0) {
                labelled++;
            }
        }
        System.out.println("  blocks labelled: " + labelled + " of " + blocks);
        System.exit(T.host.isClassifierReady() && labelled == blocks ? 0 : 1);
    }
}
