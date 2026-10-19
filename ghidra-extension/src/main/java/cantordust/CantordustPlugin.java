package cantordust;

import java.io.IOException;
import java.io.InputStream;

import javax.swing.SwingUtilities;

import docking.ActionContext;
import docking.action.DockingAction;
import docking.action.MenuData;
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
        openAction.setEnabled(false);
        tool.addAction(openAction);
    }

    @Override
    protected void programActivated(Program program) {
        openAction.setEnabled(true);
        // A provider built for the previous program shows the previous program's
        // bytes; replace it rather than let it go stale.
        if (provider != null) {
            close();
            open();
        }
    }

    @Override
    protected void programDeactivated(Program program) {
        openAction.setEnabled(false);
        close();
    }

    private void open() {
        Program program = getCurrentProgram();
        if (program == null) {
            return;
        }
        close();
        host = new PluginHost(this, program);
        byte[] data = host.getData();
        if (data.length < 4) {
            tool.setStatusInfo("Cantordust: " + program.getName() + " has no bytes to visualize");
            return;
        }
        SwingUtilities.invokeLater(() -> {
            try {
                MainInterface mi = new MainInterface(data, host);
                host.setMainInterface(mi);
                provider = new CantordustProvider(tool, mi, program.getName(), host);
                tool.addComponentProvider(provider, true);
            } catch (IOException e) {
                tool.setStatusInfo("Cantordust: could not build the interface: " + e.getMessage());
            }
        });
    }

    private void close() {
        if (provider != null) {
            tool.removeComponentProvider(provider);
            provider = null;
        }
        host = null;
    }

    @Override
    protected void dispose() {
        close();
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
