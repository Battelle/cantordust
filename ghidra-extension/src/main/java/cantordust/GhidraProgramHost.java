package cantordust;

import java.io.IOException;
import java.util.List;

import ghidra.program.database.mem.FileBytes;
import ghidra.program.model.address.Address;
import ghidra.program.model.address.AddressIterator;
import ghidra.program.model.listing.Program;
import ghidra.program.model.mem.Memory;
import ghidra.program.model.mem.MemoryAccessException;
import ghidra.program.model.mem.MemoryBlock;
import ghidra.program.model.mem.MemoryBlockSourceInfo;
import resources.ClassifierModel;
import resources.Host;
import resources.MainInterface;

/**
 * The parts of being a Ghidra host that depend only on a Program, shared by
 * the plugin. The script keeps its own copy of this logic in GhidraSrc, where
 * it predates the Host interface; the two read bytes and map offsets the same
 * way, so a fix in one belongs in the other.
 */
abstract class GhidraProgramHost implements Host {

    protected final Program program;
    private MainInterface mainInterface;
    private volatile ClassifierModel classifier;
    private volatile boolean classifierInitialized;

    GhidraProgramHost(Program program) {
        this.program = program;
    }

    void setMainInterface(MainInterface mi) {
        this.mainInterface = mi;
    }

    @Override
    public MainInterface getMainInterface() {
        return mainInterface;
    }

    @Override
    public long getMinAddressOffset() {
        Address min = program.getMinAddress();
        return (min == null) ? 0L : min.getOffset();
    }

    @Override
    public String getCurrentDirectory() {
        return "";
    }

    /**
     * The program's original file bytes if Ghidra kept them, otherwise every
     * initialized byte in address order.
     */
    @Override
    public byte[] getData() {
        Memory memory = program.getMemory();
        List<FileBytes> all = memory.getAllFileBytes();
        long total = 0;
        for (FileBytes fb : all) {
            total += fb.getSize();
        }
        if (all.isEmpty() || total <= 0 || total > Integer.MAX_VALUE) {
            return readAddresses(memory);
        }
        byte[] data = new byte[(int) total];
        int pos = 0;
        for (FileBytes fb : all) {
            int len = (int) fb.getSize();
            try {
                int read = fb.getOriginalBytes(0L, data, pos, len);
                for (int i = read; i < len; i++) {
                    data[pos + i] = fb.getOriginalByte(i);
                }
            } catch (IOException e) {
                return readAddresses(memory);
            }
            pos += len;
        }
        return data;
    }

    private static byte[] readAddresses(Memory memory) {
        long n = memory.getNumAddresses();
        byte[] data = new byte[(int) Math.min(n, Integer.MAX_VALUE - 8)];
        AddressIterator it = memory.getAddresses(true);
        int i = 0;
        while (it.hasNext() && i < data.length) {
            try {
                data[i] = memory.getByte(it.next());
            } catch (MemoryAccessException e) {
                // uninitialized; leave zero
            }
            i++;
        }
        return data;
    }

    /** File offset to the address of the block that was loaded from it, or null. */
    protected Address toAddress(long fileOffset) {
        for (MemoryBlock blk : program.getMemory().getBlocks()) {
            for (MemoryBlockSourceInfo info : blk.getSourceInfos()) {
                long base = info.getFileBytesOffset();
                if (base < 0) {
                    continue;
                }
                long offset = fileOffset - base;
                if (offset >= 0 && offset < info.getLength()) {
                    return info.getMinAddress().add(offset);
                }
            }
        }
        return null;
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
