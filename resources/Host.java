package resources;

import java.io.IOException;
import java.io.InputStream;

/**
 * Everything the visualizations need from whatever is hosting them.
 *
 * Cantordust runs in two places: inside Ghidra, where the bytes come from the
 * open program and a click can drive the listing; and on its own, where the bytes
 * come from a file and a click drives a hex view. Only three classes know which
 * one they are in. Everything else - every visualizer, the classifier, the
 * interface - talks to this, and was written against nine methods before the
 * interface existed to name them.
 */
public interface Host {

    /** Debug output. Goes to the Ghidra console or stderr, or nowhere. */
    void cdprint(String s);

    /** The bytes being visualized. */
    byte[] getData();

    MainInterface getMainInterface();

    /**
     * Offset of the first byte, for the address readouts. A program loaded at
     * 0x400000 reports that; a file on disk reports 0.
     */
    long getMinAddressOffset();

    /**
     * Take the user to this file offset - the listing in Ghidra, a hex view when
     * standalone. Returns false if the offset could not be shown, for example
     * because it falls outside every mapped block.
     */
    boolean gotoFileAddress(long fileOffset);

    /**
     * Directory the script or application lives in, with a trailing separator.
     * Kept for callers that still build paths by hand; new code should use
     * openResource.
     */
    String getCurrentDirectory();

    /**
     * Opens a bundled resource such as an icon or a classifier template.
     * The path is relative to the project root, e.g. "resources/icons/icon.png",
     * and the same string works whether the resource is a file beside the script
     * or an entry inside a jar. The caller closes the stream.
     */
    InputStream openResource(String relativePath) throws IOException;

    // ---- classifier lifecycle -------------------------------------------------

    /** Builds the classifier if it is not already built. Slow; not for the EDT. */
    void initiateClassifier();

    /** True once every block has a label. */
    boolean isClassifierReady();

    /** The classifier, or null until initiateClassifier has completed. */
    ClassifierModel getClassifier();
}
