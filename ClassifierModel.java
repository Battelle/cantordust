import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ClassifierModel {
    private Cantordust cantordust;
    public final static String[] classes = {"arm4", "arm7", "ascii_english", "compressed", "java", "mips", "msil", "ones", "png",
                                "powerpc", "sparc_32", "utf_16_english", "x64", "x86", "x86_padding", "zeros", "embedded_image"};
    private NGramModel[] nGramModels = new NGramModel[classes.length];
    private String basePath;
    private int grams;
    private int[] blockClassifications;
    public static int DEFAULT_GRAMS = 4;
    public static int BLOCK_SIZE = 12;



    public ClassifierModel(Cantordust cantordust, int grams) {
        basePath = cantordust.currentDirectory + "resources/templates/";
        this.grams = grams;
        this.cantordust = cantordust;
    }

    public void initialize(){
        for(int i=0; i < classes.length; i++) {
            byte[] data = null;
            try {
                Path p = (new File(basePath + classes[i])).toPath();
                data = Files.readAllBytes((new File(basePath + classes[i] + ".template")).toPath());
            } catch(IOException e) {
                e.printStackTrace();
            }
            cantordust.cdprint(String.format("generated "+classes[i]+" ngram\n"));
            cantordust.cdprint("My stuff {\ndata: "+data.length+"\n");
            cantordust.cdprint("grams: "+this.grams+"\n}\n");
            nGramModels[i] = new NGramModel(data, this.grams);
        }
        classifyData();
    }

    public int classify(byte[] data, int low, int high) {
        NGramModel model = new NGramModel(data, low, high - low, grams);
        ExponentialNotation p = model.EvaluateClassification(nGramModels[0]);
        int classification = 0;
        int i;
        for(i = 1; i < classes.length; i++) {
            ExponentialNotation p1 = model.EvaluateClassification(nGramModels[i]);
            if(p1.greaterThan(p)) {
                p = p1;
                classification = i;
            }
        }
        return classification;
    }

    public void classifyData() {
        byte[] data = cantordust.getData();
        /*if(data.length % DEFAULT_BLOCK_SIZE > 0) {
            blockClassifications = new String[(data.length / DEFAULT_BLOCK_SIZE) + 1];
        } else {
            blockClassifications = new String[data.length / DEFAULT_BLOCK_SIZE];
        }*/
        blockClassifications = new int[(data.length / BLOCK_SIZE)];
        for(int i=0; i < blockClassifications.length; i++) {
            blockClassifications[i] = classify(data, i* BLOCK_SIZE, i* BLOCK_SIZE + BLOCK_SIZE);
            cantordust.cdprint(String.format("block %d-%d : %s\n", i, i+ BLOCK_SIZE, blockClassifications[i]));
        }

        /*for(int i = 0; i < 5000; i++) {
            cantordust.cdprint(String.format("class at index %d: %s\n", i, classAtIndex(i)));
        }*/
    }

    public int classAtIndex(int index) {
        cantordust.cdprint(String.format("calling classAtIndex for index: %d and getting block %d\n", index, index / BLOCK_SIZE));
        try {
            return blockClassifications[index / BLOCK_SIZE];
        } catch(IndexOutOfBoundsException e) {
            // Temporary. classifyData should account for last section of data. Will have to fix that.
            return 0;
        }
    }
}
