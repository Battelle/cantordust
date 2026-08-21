package resources;

import ghidra.app.script.GhidraScript;
import ghidra.app.util.exporter.BinaryExporter;
import ghidra.app.util.exporter.ExporterException;
import ghidra.util.task.TaskMonitor;
import ghidra.program.model.mem.Memory;
import ghidra.program.model.listing.Program;
import ghidra.program.model.address.Address;
import ghidra.program.model.address.AddressSet;
import ghidra.program.model.address.AddressIterator;
import ghidra.program.model.mem.MemoryBlock;
import ghidra.program.model.mem.MemoryBlockSourceInfo;
import ghidra.program.model.mem.MemoryAccessException;
import ghidra.program.database.mem.FileBytes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class GhidraSrc extends GhidraScript{
    public MainInterface mainInterface;
    public String currentDirectory;
    public String name;
    private boolean DEBUG = false;

    public volatile ClassifierModel classifier;
    public volatile boolean classifierInitialized = false;

    protected void run() throws Exception {
    }

    public String getName() {
        return "";
    }

    public void cdprint(String s){
        if(DEBUG){
            print(s);
        }
    }

    public String getCurrentDirectory(){
        return "";
    }

    public MainInterface getMainInterface(){
        return this.mainInterface;
    }
    
    public void createFile( File f ){
        BinaryExporter bexp = new BinaryExporter();
        Memory memory = currentProgram.getMemory();
        TaskMonitor monitor = getMonitor();
        Program domainObj = currentProgram;
        try{
            bexp.export(f, domainObj, memory, monitor);
        } catch (ExporterException e){
            cdprint("ERROR Saving File Locally\n"+e.toString());
        } catch (IOException e){
            cdprint("ERROR Saving File Locally\n"+e.toString());
        }
    }

    public void changeTitle(String s){}

    public byte[] getData(){
        byte[] data = getJavaData();
        // Reading the original file off disk is the most faithful source, but it
        // comes up short (or empty) whenever the recorded path is stale, points at
        // a container, or the project was moved to another machine. Only trust it
        // when it covers at least as much as Ghidra's own copy of the file bytes,
        // which we can measure from metadata alone.
        if(data.length < getFileBytesSize()){
            data = getGhidraData();
        }
        return data;
    }

    /**
     * Total size of every FileBytes the program holds, read from metadata only.
     */
    public long getFileBytesSize() {
        long size = 0;
        for(FileBytes fb : currentProgram.getMemory().getAllFileBytes()) {
            size += fb.getSize();
        }
        return size;
    }

    public byte[] getGhidraData() {
        List<FileBytes> bytes = currentProgram.getMemory().getAllFileBytes();
        if(bytes.isEmpty()) {
            return getDataThroughAddressIteration();
        }
        // A program can carry more than one FileBytes (containers, multi-segment
        // loaders, added files). Using only the first one silently truncated the
        // view to whatever that first entry happened to cover.
        long total = getFileBytesSize();
        if(total <= 0 || total > Integer.MAX_VALUE) {
            return getDataThroughAddressIteration();
        }
        byte[] data = new byte[(int) total];
        int pos = 0;
        for(FileBytes fb : bytes) {
            int len = (int) fb.getSize();
            try {
                // Bulk read; falls back to a byte at a time if the block read is short.
                int read = fb.getOriginalBytes(0L, data, pos, len);
                if(read < len) {
                    for(int i = read; i < len; i++) {
                        data[pos + i] = fb.getOriginalByte((long) i);
                    }
                }
            } catch (IOException e) {
                cdprint("ERROR reading file bytes\n" + e.toString());
            }
            pos += len;
        }
        return data;
    }

    public byte[] getDataThroughAddressIteration() {
        Memory mem = currentProgram.getMemory();
        AddressIterator iter = mem.getAddresses(true);
        byte[] data = new byte[(int)mem.getNumAddresses()];
        int i=0;
        for(Address addr : iter) {
            try {
                data[i] = mem.getByte(addr);
            } catch(MemoryAccessException e) {}
            i++;
        }
        return data;
    }

    public byte[] getJavaData() {
        byte[] data = {};
        String path = currentProgram.getExecutablePath();
        if(path == null || path.isEmpty()) {
            return data;
        }
        // On Windows, getExecutablePath() hands back a leading-separator path such
        // as "/C:/dir/file", which Paths.get rejects. This applied to every Windows
        // release, not just the one the original check named.
        if(System.getProperty("os.name", "").startsWith("Windows")
                && path.length() > 2 && (path.charAt(0) == '\\' || path.charAt(0) == '/')
                && path.charAt(2) == ':') {
            path = path.substring(1);
        }
        try {
            data = Files.readAllBytes(Paths.get(path));
        } catch (IOException e) {
            cdprint("Could not read original file at " + path + "\n" + e.toString());
        } catch (RuntimeException e) {
            // InvalidPathException and friends are unchecked; letting one escape
            // here aborted the whole script instead of falling back to Ghidra.
            cdprint("Invalid original file path " + path + "\n" + e.toString());
        }
        return data;
    }

    /**
     * Numeric offset of the program's lowest address.
     *
     * Callers used to do Long.parseLong(getMinAddress().toString(false), 16), which
     * throws on two real address formats: segmented addresses render as "1000:0000"
     * (16-bit DOS executables), and any address above Long.MAX_VALUE overflows the
     * signed parse (kernel-space images at 0xFFFF...). Asking the Address for its
     * offset avoids the string round-trip entirely.
     */
    public long getMinAddressOffset() {
        Address min = currentProgram.getMinAddress();
        return (min == null) ? 0L : min.getOffset();
    }

    /**
     * Gets the current selected address in Ghidra
     */
    public Address getCurrentAddress() {
        return this.currentAddress;
    }

    /**
     * Converts a file offset to a Ghidra address
     */
    public Address getGhidraAddress(long fileOffset) {
        // Get all the memory blocks of the program
        MemoryBlock[] memBlks = currentProgram.getMemory().getBlocks();

        for(MemoryBlock blk : memBlks) {
            // For each block, get the block's source infos
            for(MemoryBlockSourceInfo sourceInfo : blk.getSourceInfos()) {
                // For each source info, get the filebytesoffset and length
                long fileBytesOffset = sourceInfo.getFileBytesOffset();
                long length = sourceInfo.getLength();

                // Skip regions with a fileBytesOffset of -1
                if(fileBytesOffset == -1)
                    continue;

                // Check if fileOffset is in this block
                long offset = fileOffset - fileBytesOffset;
                if((offset > 0) && (offset < length)) {
                    // If it is, get an address for that fileOffset based on the block's start address
                    Address start = blk.getStart();

                    // Get a new address in start's address space using its offset and offet
                    Address addr = start.getNewAddress(offset + start.getOffset());
                    return addr;
                }
            }
        }
        
        return null;
    }

    /**
     * Sets the current address in Ghidra to a specific file offset
     */
    public boolean gotoFileAddress(long addr) {
        Address ghidraAddr = getGhidraAddress(addr);

        // Make an AddressSet based on ghidraAddr
        AddressSet set = new AddressSet(ghidraAddr);

        if(ghidraAddr != null) {
            // Set the current location and highlight to the current address to change
            // the current selected address in Ghidra
            setCurrentLocation(ghidraAddr);
            setCurrentHighlight(set);
            return true;
        }
		return false;
    }

    /**
     * Builds the classifier if it has not been built. Takes seconds and must not
     * be called from the event thread; MetricMap runs it on a worker.
     * Synchronized so two menu clicks cannot start two builds.
     */
    public synchronized void initiateClassifier() {
        if(classifierInitialized) {
            return;
        }
        ClassifierModel model = new ClassifierModel(this, ClassifierModel.DEFAULT_GRAMS);
        // Published only once it is usable, so getClassifier never hands back a
        // half-trained model.
        model.initialize();
        classifier = model;
        classifierInitialized = true;
    }

    /** True once the classifier is built and every block has a label. */
    public boolean isClassifierReady() {
        ClassifierModel c = classifier;
        return c != null && c.isReady();
    }

    public ClassifierModel getClassifier() {
        return classifier;
    }
}