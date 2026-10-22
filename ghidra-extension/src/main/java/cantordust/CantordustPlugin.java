package cantordust;

import java.awt.Image;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import docking.ActionContext;
import docking.action.DockingAction;
import docking.action.KeyBindingData;
import docking.action.MenuData;
import docking.action.ToolBarData;
import ghidra.app.plugin.PluginCategoryNames;
import ghidra.app.plugin.ProgramPlugin;
import ghidra.framework.plugintool.PluginInfo;
import ghidra.framework.plugintool.PluginTool;
import ghidra.framework.plugintool.util.PluginStatus;
import ghidra.program.model.listing.Program;
import resources.CantordustProvider;
import resources.Host;
import resources.MainInterface;

/**
 * Cantordust as a Ghidra plugin: installed once through File > Install
 * Extensions, opened from the Window menu, no script directory to map.
 *
 * The visualizations themselves are the same classes the script runs. This
 * class is only the host - it hands them the open program's bytes, tells the
 * listing where to go when a point is clicked, and keeps the provider in step
 * with which program is active.
 */
@PluginInfo(
    status = PluginStatus.STABLE,
    packageName = CantordustPluginPackage.NAME,
    category = PluginCategoryNames.ANALYSIS,
    shortDescription = "Binary visualization",
    description = "Visualizes the bytes of the current program as byte-value, digraph, trigram and "
            + "space-filling-curve plots, with an n-gram classifier. A click in any visualization "
            + "jumps the listing to the bytes behind it."
)
public class CantordustPlugin extends ProgramPlugin {

    private DockingAction openAction;
    private CantordustProvider provider;
    private PluginHost host;

    public CantordustPlugin(PluginTool tool) {
        super(tool);
        openAction = new DockingAction("Open Cantordust", getName()) {
            @Override
            public void actionPerformed(ActionContext context) {
                open();
            }
        };
        openAction.setMenuBarData(new MenuData(new String[]{"Window", "Cantordust"}, "Cantordust"));
        // The same two ways in the script offered: its toolbar icon and Alt+C.
        openAction.setToolBarData(new ToolBarData(toolbarIcon(), "Cantordust"));
        openAction.setKeyBindingData(new KeyBindingData(KeyEvent.VK_C, InputEvent.ALT_DOWN_MASK));
        openAction.setDescription("Open Cantordust (binary visualization)");
        openAction.setEnabled(false);
        tool.addAction(openAction);
    }

    /** The toolbar icon, from this jar. ImageIO rather than ResourceManager - see CantordustPluginPackage. */
    private static Icon toolbarIcon() {
        try (InputStream in = CantordustPlugin.class.getClassLoader().getResourceAsStream("resources/icons/icon.png")) {
            if (in != null) {
                return new ImageIcon(ImageIO.read(in).getScaledInstance(16, 16, Image.SCALE_SMOOTH));
            }
        } catch (IOException | RuntimeException e) {
            // fall through
        }
        return new ImageIcon();
    }

    @Override
    protected void programActivated(Program program) {
        openAction.setEnabled(true);
        // A provider built for another program shows that program's bytes.
        // Drop it; the next Window > Cantordust builds one for this program.
        if (provider != null && host != null && host.program != program) {
            closeProvider();
        }
    }

    @Override
    protected void programDeactivated(Program program) {
        openAction.setEnabled(false);
    }

    /**
     * Shows the Cantordust window for the current program, building it on
     * first use. Everything here runs on the event thread, which is where the
     * action fires, so there is no hand-off to lose an exception in.
     */
    private void open() {
        Program program = getCurrentProgram();
        if (program == null) {
            tool.setStatusInfo("Cantordust: open a program first");
            return;
        }
        if (provider != null && host != null && host.program == program) {
            tool.showComponentProvider(provider, true);
            return;
        }
        closeProvider();
        PluginHost newHost = new PluginHost(this, program);
        byte[] data = newHost.getData();
        if (data.length < 4) {
            tool.setStatusInfo("Cantordust: " + program.getName() + " has no bytes to visualize");
            return;
        }
        try {
            MainInterface mi = new MainInterface(data, newHost);
            newHost.setMainInterface(mi);
            host = newHost;
            provider = new CantordustProvider(tool, mi, program.getName(), newHost, false);
            tool.addComponentProvider(provider, true);
            tool.showComponentProvider(provider, true);
        } catch (IOException | RuntimeException e) {
            // Say so where the user is looking, and keep the stack for the log.
            tool.setStatusInfo("Cantordust: could not open - " + e);
            ghidra.util.Msg.error(this, "Cantordust could not open", e);
        }
    }

    /**
     * Not named close(): Plugin already has a protected close() with its own
     * lifecycle meaning, and a private method of that name is an illegal
     * narrowing override rather than a new method.
     */
    private void closeProvider() {
        if (provider != null) {
            tool.removeComponentProvider(provider);
            provider = null;
        }
        host = null;
    }

    @Override
    protected void dispose() {
        closeProvider();
        super.dispose();
    }

    /** What the visualizations need, answered from the plugin rather than a script. */
    static final class PluginHost extends GhidraProgramHost {
        private final CantordustPlugin plugin;

        PluginHost(CantordustPlugin plugin, Program program) {
            super(program);
            this.plugin = plugin;
        }

        @Override
        public void cdprint(String s) {
            // Nothing to print to; the script printed to its console.
        }

        @Override
        public boolean gotoFileAddress(long fileOffset) {
            ghidra.program.model.address.Address addr = toAddress(fileOffset);
            return addr != null && plugin.goTo(addr);
        }

        @Override
        public InputStream openResource(String relativePath) throws IOException {
            // Inside the extension jar, resources live at the same relative path
            // the script reads from disk: "resources/icons/x.png".
            InputStream in = CantordustPlugin.class.getClassLoader().getResourceAsStream(relativePath);
            if (in == null) {
                throw new IOException("not in the extension jar: " + relativePath);
            }
            return in;
        }
    }
}
