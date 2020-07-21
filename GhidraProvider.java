import ghidra.app.script.JavaScriptProvider;
import generic.jar.ResourceFile;
import java.io.File;

public class GhidraProvider extends JavaScriptProvider {
    public GhidraProvider(){
        super();
    }
    public File getClass(ResourceFile sourceFile, String className){
        return getClassFile(sourceFile, className);
        }
}