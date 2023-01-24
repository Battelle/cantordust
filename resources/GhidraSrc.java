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

import javax.swing.JFrame;
import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class GhidraSrc extends GhidraScript{
    public MainInterface mainInterface;
    public String currentDirectory;
    public String name;
    public JFrame frame;
    private boolean DEBUG = false;

    public ClassifierModel classifier;
    public boolean classifierInitialized = false;

    public GhidraSrc(){
        this.frame = new JFrame();
    }

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
        if(data.length == 0){
            data = getGhidraData();
        }
        return data;
    }

    public byte[] getGhidraData() {
        List<FileBytes> bytes = currentProgram.getMemory().getAllFileBytes();
        if(bytes.size() > 0) {
            long size = bytes.get(0).getSize();
            byte[] data = new byte[(int) size];
            try {
                for (int i = 0; i < size; i++) {
                    data[i] = bytes.get(0).getOriginalByte((long) i);
                }
            } catch (IOException e) {
            }
            return data;
        } else {
            return getDataThroughAddressIteration();
        }
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
        String path = currentProgram.getExecutablePath();
        if(System.getProperty("os.name").equals("Windows 10") && (path.charAt(0) == '\\' || path.charAt(0) == '/')) {
            path = path.substring(1);
        }
        Path path_p = Paths.get(path);
        byte[] data = {};
        try {
            data = Files.readAllBytes(path_p);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
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

    public void initiateClassifier() {
        if(!classifierInitialized) {
            classifier = new ClassifierModel(this, ClassifierModel.DEFAULT_GRAMS);
            classifier.initialize();
            classifierInitialized = true;
        }
    }

    public ClassifierModel getClassifier() {
        return classifier;
    }
}