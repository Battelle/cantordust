package resources;

import java.io.IOException;
import java.io.InputStream;

/**
 * Labels each block of the program with the content class it most resembles,
 * from a set of n-gram models trained on the templates in resources/templates.
 */
public class ClassifierModel {

    /**
     * One entry per file in resources/templates. The originals came from the C#
     * tool; arm64, riscv64, s390x, ppc64el and mips64el were added later from
     * Debian binaries, and cover architectures the original set had no class for
     * at all - the closest it had to 64-bit ARM was 32-bit arm7.
     */
    public final static String[] classes = {"arm4", "arm7", "ascii_english", "compressed", "java", "mips", "msil", "ones", "png",
                                "powerpc", "sparc_32", "utf_16_english", "x64", "x86", "x86_padding", "zeros", "embedded_image",
                                "arm64", "riscv64", "s390x", "ppc64el", "mips64el"};
    public static int DEFAULT_GRAMS = 4;
    /** Resolution of the shading: every BLOCK_SIZE bytes gets its own label. */
    public static int BLOCK_SIZE = 12;
    /**
     * Bytes of surrounding context each label is decided from. Twelve bytes is
     * nine four-grams, which is thin evidence for a seventeen-way decision:
     * measured against held-out template data it is right 77% of the time,
     * against 91% at sixty-four. Widening the window rather than the block keeps
     * the shading fine-grained while giving each decision enough to work with.
     */
    public static int CONTEXT = 64;

    private final Host cantordust;
    private final int grams;
    private final NGramModel[] nGramModels = new NGramModel[classes.length];

    private volatile int[] blockClassifications;
    /** 0..1 while building, for the caller to report. */
    private volatile double progress;
    private volatile String stage = "";

    public ClassifierModel(Host cantordust, int grams) {
        this.grams = grams;
        this.cantordust = cantordust;
    }

    public double getProgress() {
        return progress;
    }

    public String getStage() {
        return stage;
    }

    /** True once every block has a label and classAtIndex means something. */
    public boolean isReady() {
        return blockClassifications != null;
    }

    public void initialize() {
        for (int i = 0; i < classes.length; i++) {
            String templatePath = "resources/templates/" + classes[i] + ".template";
            byte[] data;
            try (InputStream in = cantordust.openResource(templatePath)) {
                data = in.readAllBytes();
            } catch (IOException e) {
                // Without this, an unreadable template left data null and the next
                // line failed with an unrelated NPE, hiding the real cause.
                throw new IllegalStateException("Could not read classifier template: " + templatePath, e);
            }
            stage = "training " + classes[i];
            progress = i / (double) (classes.length + 1);
            cantordust.cdprint(String.format("training %s from %d bytes%n", classes[i], data.length));
            nGramModels[i] = new NGramModel(data, this.grams);
        }
        stage = "classifying";
        progress = classes.length / (double) (classes.length + 1);
        classifyData();
        stage = "";
        progress = 1;
    }

    /** Index into classes of whichever model best explains data[low, high). */
    public int classify(byte[] data, int low, int high) {
        int best = 0;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < classes.length; i++) {
            NGramModel model = nGramModels[i];
            if (model == null) {
                continue;
            }
            // Log likelihood, so classes combine by addition and a 12-byte block
            // does not underflow the way a product of probabilities does.
            double score = model.logLikelihood(data, low, high);
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        return best;
    }

    public void classifyData() {
        final byte[] data = cantordust.getData();
        // Round up: the old size dropped the final partial block, so every index
        // inside it fell through classAtIndex's catch and reported class 0.
        final int blocks = (data.length + BLOCK_SIZE - 1) / BLOCK_SIZE;
        final int[] result = new int[blocks];

        // Every block is independent, and a large program is millions of them.
        int threads = Math.max(1, Math.min(8, Runtime.getRuntime().availableProcessors() - 1));
        if (blocks < 4096 || threads == 1) {
            classifyRange(data, result, 0, blocks);
        } else {
            Thread[] workers = new Thread[threads];
            final int span = (blocks + threads - 1) / threads;
            for (int t = 0; t < threads; t++) {
                final int from = t * span;
                final int to = Math.min(blocks, from + span);
                workers[t] = new Thread(() -> classifyRange(data, result, from, to),
                        "cantordust-classify-" + t);
                workers[t].setDaemon(true);
                workers[t].start();
            }
            for (Thread w : workers) {
                try {
                    w.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        blockClassifications = result;
    }

    private void classifyRange(byte[] data, int[] result, int fromBlock, int toBlock) {
        int half = CONTEXT / 2;
        for (int i = fromBlock; i < toBlock; i++) {
            // Centre the evidence window on the block, clamped to the program.
            int centre = i * BLOCK_SIZE + BLOCK_SIZE / 2;
            int low = Math.max(0, centre - half);
            int high = Math.min(data.length, low + CONTEXT);
            low = Math.max(0, high - CONTEXT);
            result[i] = classify(data, low, high);
        }
    }

    /**
     * Class of the block containing this offset, or -1 while the classifier is
     * still being built or if the offset is outside the program.
     */
    public int classAtIndex(int index) {
        int[] labels = blockClassifications;
        if (labels == null || index < 0) {
            return -1;
        }
        int block = index / BLOCK_SIZE;
        return (block < labels.length) ? labels[block] : -1;
    }

    /** Name for a class index, or "unclassified" for -1. */
    public static String nameOf(int classification) {
        return (classification >= 0 && classification < classes.length)
                ? classes[classification] : "unclassified";
    }
}
