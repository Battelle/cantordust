// CantorDust
// @author Battelle Memorial Institute
// @category Binary Visualization
// @keybinding alt C
// @toolbar resources/icons/icon.png
import java.awt.Dimension;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import docking.ComponentProvider;
import ghidra.framework.plugintool.PluginTool;

import resources.CantordustProvider;
import resources.MainInterface;
import resources.GhidraSrc;

public class Cantordust extends GhidraSrc {
    public resources.MainInterface mainInterface;
    public resources.CantordustProvider provider;
    public String currentDirectory;
    public String name;

    @Override
    protected void run() throws Exception {
        // Resolve the script's own directory rather than trimming a hard-coded
        // filename length, so the resources/ folder is found wherever Ghidra
        // picked the script up from.
        this.currentDirectory = sourceFile.getParentFile().getAbsolutePath() + File.separator;
        if(currentProgram==null){
            printf("Open a file to examine with CantorDust before continuing!\n");
            return;
        }

        // When the Cantordust extension is installed, its jar already provides
        // every class under resources/, and the OSGi loader will not let this
        // script's copy of that package load beside it - the symptom is
        // "could not be found or loaded ... resources/GhidraSrc" with no further
        // explanation. The plugin is the extension; use it instead.
        if (isExtensionInstalled()) {
            printf("The Cantordust extension is installed; use Window > Cantordust (or Alt+C) in the CodeBrowser instead of this script.\n");
            printf("To use the script, uninstall the extension first (File > Install Extensions, untick Cantordust).\n");
            return;
        }

        PluginTool tool = getState().getTool();
        if(tool == null){
            printf("Cantordust needs a Ghidra tool window; run it from the GUI.\n");
            return;
        }

        this.name = currentProgram.getName();
        byte[] data = getData();

        // Building Swing components off the event thread is a race. Read the
        // program bytes first, then do the UI work where it belongs.
        try {
            SwingUtilities.invokeAndWait(() -> showProvider(tool, data));
        } catch (InvocationTargetException e) {
            throw new Exception("Could not open Cantordust", e.getCause());
        }
    }

    private void showProvider(PluginTool tool, byte[] data) {
        // A provider left from an earlier run holds classes from the previous
        // compile of this script, so replace it rather than reusing it.
        ComponentProvider existing = tool.getComponentProvider(CantordustProvider.NAME);
        if(existing != null){
            tool.removeComponentProvider(existing);
        }
        try {
            this.mainInterface = new resources.MainInterface(data, this);
        } catch (java.io.IOException e) {
            printf("Could not build the Cantordust interface: %s\n", e.toString());
            return;
        }
        this.mainInterface.setPreferredSize(new Dimension(
                resources.MainInterface.getWindowWidth(),
                resources.MainInterface.getWindowHeight()));
        this.provider = new CantordustProvider(tool, mainInterface, name, this);
        tool.addComponentProvider(provider, true);
    }

    /**
     * True if the Cantordust extension is installed in this Ghidra. Checked on
     * disk rather than by loading a class: the whole problem is that the
     * extension's classes and this script's live in separate loaders, so a
     * class lookup from here could miss it and give no warning at all.
     */
    private static boolean isExtensionInstalled() {
        try {
            File userDir = ghidra.framework.Application.getUserSettingsDirectory();
            return new File(userDir, "Extensions" + File.separator + "Cantordust").isDirectory();
        } catch (RuntimeException e) {
            return false;
        }
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
        if(provider != null){
            provider.setSubTitle(s);
        }
    }
}
