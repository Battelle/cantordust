package resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.LongPredicate;

/**
 * The host when Cantordust runs on its own: bytes come from a file, and a click
 * goes to whatever the application registered to show an offset - a hex view.
 *
 * Resources are looked up on the classpath first, which is where they are inside
 * the jar, then beside the application, which is where they are in a checkout run
 * from the command line.
 */
public class StandaloneHost implements Host {

    private final byte[] data;
    private final String name;
    private final String baseDir;
    private MainInterface mainInterface;
    private LongPredicate onGoto = offset -> false;
    private volatile ClassifierModel classifier;
    private volatile boolean classifierInitialized;

    public StandaloneHost(byte[] data, String name, String baseDir) {
        this.data = data;
        this.name = name;
        this.baseDir = baseDir.endsWith(File.separator) ? baseDir : baseDir + File.separator;
    }

    public String getName() {
        return name;
    }

    public void setMainInterface(MainInterface mi) {
        this.mainInterface = mi;
    }

    /** Called with a file offset on click; return true if it was shown. */
    public void setGotoHandler(LongPredicate handler) {
        this.onGoto = handler;
    }

    @Override
    public void cdprint(String s) {
        if (Boolean.getBoolean("cantordust.debug")) {
            System.err.print(s);
        }
    }

    @Override
    public byte[] getData() {
        return data;
    }

    @Override
    public MainInterface getMainInterface() {
        return mainInterface;
    }

    @Override
    public long getMinAddressOffset() {
        // A file's addresses are its offsets.
        return 0;
    }

    @Override
    public boolean gotoFileAddress(long fileOffset) {
        return onGoto.test(fileOffset);
    }

    @Override
    public String getCurrentDirectory() {
        return baseDir;
    }

    @Override
    public InputStream openResource(String relativePath) throws IOException {
        InputStream in = StandaloneHost.class.getClassLoader().getResourceAsStream(relativePath);
        if (in != null) {
            return in;
        }
        File f = new File(baseDir + relativePath.replace('/', File.separatorChar));
        if (f.isFile()) {
            return new FileInputStream(f);
        }
        throw new IOException("resource not found on classpath or under " + baseDir + ": " + relativePath);
    }

    @Override
    public synchronized void initiateClassifier() {
        if (classifierInitialized) {
            return;
        }
        ClassifierModel model = new ClassifierModel(this, ClassifierModel.DEFAULT_GRAMS);
        model.initialize();
        classifier = model;
        classifierInitialized = true;
    }

    @Override
    public boolean isClassifierReady() {
        ClassifierModel c = classifier;
        return c != null && c.isReady();
    }

    @Override
    public ClassifierModel getClassifier() {
        return classifier;
    }
}
