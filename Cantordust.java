// CantorDust
// @author Battelle Memorial Institute
// @category Binary Visualization
// @keybinding alt C
// @toolbar resources/icons/icon.png
import ghidra.app.script.GhidraScript;
import ghidra.program.model.address.Address;
import ghidra.program.model.address.AddressSet;
import ghidra.program.model.address.AddressIterator;
import ghidra.program.model.mem.Memory;
import ghidra.program.model.mem.MemoryBlock;
import ghidra.program.model.mem.MemoryBlockSourceInfo;
import ghidra.program.model.mem.MemoryAccessException;
import ghidra.program.database.mem.FileBytes;
import ghidra.program.database.mem.*;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

import javax.swing.*;

public class Cantordust extends GhidraScript {
    public MainInterface mainInterface;
    public String currentDirectory;
    public String name;
    private boolean DEBUG = false;

    private ClassifierModel classifier;
    private boolean classifierInitialized = false;

    @Override
    protected void run() throws Exception {
        currentDirectory = sourceFile.getAbsolutePath();
        currentDirectory = currentDirectory.substring(0, currentDirectory.length()-15);
        if(currentProgram==null){
            printf("Open a file to examine with CantorDust before continuing!\n");
            return;
        }
        name = currentProgram.getName();
        writeBinLocation();
        JFrame frame = new JFrame(String.format("..cantor.dust..    :   %s", name));
        mainInterface = new MainInterface(getData(), this, frame);
        frame.getContentPane().add(mainInterface);
        frame.setSize(MainInterface.getWindowWidth(), MainInterface.getWindowHeight());
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        /*JFrame frame2 = new JFrame();
        ThreeTupleVisualizer vis = new ThreeTupleVisualizer(this, null, frame2);
        frame2.setVisible(true);
        frame2.setSize(ThreeTupleVisualizer.getWindowSize(), ThreeTupleVisualizer.getWindowSize());
        vis.initGL();
        vis.paintGL();*/
    }

    public void cdprint(String s){
        if(DEBUG){
            print(s);
        }
    }

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
                    data[i] = bytes.get(0).getOriginalByte​((long) i);
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

    public void writeBinLocation(){ // run python cleanup.py to recompile program
        GhidraProvider mp = new GhidraProvider();
        String path = mp.getClass(sourceFile, "Cantordust").getAbsolutePath();
        path = path.substring(0, path.length()-16);
        String fileName = currentDirectory+"ghidra_bin_location.txt";
        try{
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(fileName), StandardCharsets.UTF_8);
            writer.write(path);
            writer.close();
        }catch(IOException e){}
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
