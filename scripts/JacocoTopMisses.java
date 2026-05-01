import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Prints top classes by LINE_MISSED for a package prefix (from JaCoCo CSV).
 *
 * Usage (from project root):
 *   javac scripts\\JacocoTopMisses.java
 *   java -cp scripts JacocoTopMisses target/site/jacoco/jacoco.csv com.appointmentscheduler.presentation 20
 */
public class JacocoTopMisses {

    record Row(String pkg, String clazz, double lineMissed, double lineCovered) {
        double total() { return lineMissed + lineCovered; }
        double ratio() { return total() == 0 ? 0.0 : (lineCovered / total() * 100.0); }
    }

    public static void main(String[] args) throws Exception {
        String csvPath = args.length >= 1 && args[0] != null && !args[0].isBlank()
                ? args[0]
                : "target/site/jacoco/jacoco.csv";

        String prefix = args.length >= 2 && args[1] != null && !args[1].isBlank()
                ? args[1]
                : "com.appointmentscheduler.presentation";

        int topN = args.length >= 3 ? Integer.parseInt(args[2].trim()) : 20;

        Path p = Path.of(csvPath);
        if (!Files.exists(p)) {
            System.err.println("Missing file: " + p.toAbsolutePath());
            System.exit(2);
        }

        try (BufferedReader br = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            String header = br.readLine();
            if (header == null) {
                System.err.println("Empty CSV: " + p.toAbsolutePath());
                System.exit(3);
            }

            Map<String, Integer> idx = parseHeader(header);
            int packageIdx = require(idx, "PACKAGE");
            int classIdx = require(idx, "CLASS");
            int lineMissedIdx = require(idx, "LINE_MISSED");
            int lineCoveredIdx = require(idx, "LINE_COVERED");

            List<Row> rows = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] cols = line.split(",", -1);
                if (packageIdx >= cols.length || classIdx >= cols.length) continue;

                String pkg = safeGet(cols, packageIdx);
                String clazz = safeGet(cols, classIdx);
                if (pkg == null || clazz == null) continue;

                if (!pkg.startsWith(prefix)) continue;

                double missed = parseDouble(safeGet(cols, lineMissedIdx));
                double covered = parseDouble(safeGet(cols, lineCoveredIdx));
                rows.add(new Row(pkg, clazz, missed, covered));
            }

            rows.sort(Comparator.comparingDouble((Row r) -> r.lineMissed).reversed());
            int limit = Math.min(topN, rows.size());
            for (int i = 0; i < limit; i++) {
                Row r = rows.get(i);
                System.out.printf(
                        "%02d) %s.%s  line=%.0f/%.0f (%.2f%%)%n",
                        i + 1,
                        r.pkg,
                        r.clazz,
                        r.lineCovered,
                        r.total(),
                        r.ratio()
                );
            }
        }
    }

    private static Map<String, Integer> parseHeader(String headerLine) {
        Map<String, Integer> m = new HashMap<>();
        String[] cols = headerLine.split(",", -1);
        for (int i = 0; i < cols.length; i++) {
            String key = cols[i].trim();
            if (!key.isEmpty()) m.put(key, i);
        }
        return m;
    }

    private static int require(Map<String, Integer> idx, String key) {
        Integer v = idx.get(key);
        if (v == null) throw new IllegalArgumentException("Missing column: " + key);
        return v;
    }

    private static String safeGet(String[] cols, int idx) {
        if (idx < 0 || idx >= cols.length) return null;
        return cols[idx];
    }

    private static double parseDouble(String s) {
        if (s == null) return 0.0;
        if (s.isBlank()) return 0.0;
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}

