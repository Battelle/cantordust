// CantorDust
// @author Battelle Memorial Institute
// @category Binary Visualization
// @keybinding alt C
// @toolbar resources/icons/icon.png
import java.awt.Dimension;

import javax.swing.*;

import resources.MainInterface;
import resources.GhidraSrc;

// import ghidra.app.script.GhidraScriptUtil;

public class Cantordust extends GhidraSrc {
    public resources.MainInterface mainInterface;
    public String currentDirectory;
    public String name;
    public JFrame frame;

    @Override
    protected void run() throws Exception {
        this.currentDirectory = sourceFile.getAbsolutePath();
        this.currentDirectory = currentDirectory.substring(0, currentDirectory.length()-15);
        if(currentProgram==null){
            printf("Open a file to examine with CantorDust before continuing!\n");
            return;
        }
        this.name = currentProgram.getName();
        this.frame = new JFrame();
        this.frame.setTitle(String.format("..cantor.dust..    :   %s", name));

        // potentially could be used for cleanup replacement. currently deprecated...
        // print(""+GhidraScriptUtil.getExplodedCompiledSourceBundlePaths());

        this.mainInterface = new resources.MainInterface(getData(), this, frame);
        this.frame.getContentPane().add(mainInterface);
        this.frame.setSize(resources.MainInterface.getWindowWidth(), resources.MainInterface.getWindowHeight());
        this.frame.setMinimumSize(new Dimension(resources.MainInterface.getWindowWidth(), resources.MainInterface.getWindowHeight()));
        this.frame.setVisible(true);
        this.frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    @Override
    public String getName(){
        return currentProgram.getName();
    }
    
    @Override
    public String getCurrentDirectory(){
        return this.currentDirectory;
    }
    
    @Override
    public MainInterface getMainInterface(){
        return this.mainInterface;
    }

    @Override
    public void changeTitle(String s){
        this.frame.setTitle(String.format("Cantordust    :   %s",s));
        this.frame.repaint();
    }
}
