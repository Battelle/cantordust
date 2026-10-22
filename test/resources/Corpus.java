package resources;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/**
 * Generates the inputs the checks run against, the same on every platform.
 *
 * Nothing here depends on what happens to be installed: the synthetic files
 * come from a seeded generator, the real executable is this JDK's own launcher,
 * and the large real input is a classifier template from the repository.
 */
public final class Corpus {

    private Corpus() {
    }

    public static void main(String[] a) throws Exception {
        Path dir = Path.of(a[0]);
        Files.createDirectories(dir);
        Random rnd = new Random(11);
        int q = 262144;

        byte[] zeros = new byte[65536];
        byte[] ascii = ("the quick brown fox jumps over the lazy dog. ".repeat(6000)).substring(0, q).getBytes(StandardCharsets.US_ASCII);
        byte[] random = new byte[666457];
        rnd.nextBytes(random);
        byte[] code = code();

        Files.write(dir.resolve("zeros-64k.bin"), zeros);
        Files.write(dir.resolve("ascii-256k.txt"), ascii, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
        Files.write(dir.resolve("random-666k.bin"), random);
        Files.write(dir.resolve("jdk-launcher.bin"), code);
        Files.copy(Path.of("resources", "templates", "x86.template"), dir.resolve("x86.template"),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        // Four known 256KB regions in a known order, for the entropy check.
        // Four regions of exactly known entropy, in increasing order: zeros
        // (0 bits), repeated English text (~4.3), bytes uniform over 64 values
        // (exactly log2 64 = 6.0), and uniform random (8). Synthetic rather than
        // a real code section so the expected values hold on every platform.
        byte[] composite = new byte[4 * q];
        System.arraycopy(ascii, 0, composite, q, q);
        Random six = new Random(5);
        for (int i = 0; i < q; i++) {
            composite[2 * q + i] = (byte) (0x20 + six.nextInt(64));
        }
        byte[] rq = new byte[q];
        new Random(7).nextBytes(rq);
        System.arraycopy(rq, 0, composite, 3 * q, q);
        Files.write(dir.resolve("composite.bin"), composite);

        System.out.println("corpus in " + dir.toAbsolutePath() + " (launcher " + code.length + " bytes)");
    }

    /** The running JDK's launcher: a real native executable on any platform. */
    private static byte[] code() throws Exception {
        String home = System.getProperty("java.home");
        for (String name : new String[]{"java", "java.exe"}) {
            File f = new File(home, "bin" + File.separator + name);
            if (f.isFile()) {
                return Files.readAllBytes(f.toPath());
            }
        }
        throw new IllegalStateException("no launcher under " + home);
    }
}
