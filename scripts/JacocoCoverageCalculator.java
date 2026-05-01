import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Standalone calculator for JaCoCo CSV coverage.
 *
 * Usage (from project root):
 *   javac scripts\\JacocoCoverageCalculator.java
 *   java -cp scripts JacocoCoverageCalculator
 *
 * Optional args:
 *   1) jacocoCsvPath (default: target/site/jacoco/jacoco.csv)
 *   2) packagePrefix (default: com.appointmentscheduler.presentation)
 */
public class JacocoCoverageCalculator {

    public static void main(String[] args) throws Exception {
        String csvPath = args.length >= 1 && args[0] != null && !args[0].isBlank()
                ? args[0]
                : "target/site/jacoco/jacoco.csv";

        String packagePrefix = args.length >= 2 && args[1] != null && !args[1].isBlank()
                ? args[1]
                : "com.appointmentscheduler.presentation";

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
            Integer lineMissedIdx = idx.get("LINE_MISSED");
            Integer lineCoveredIdx = idx.get("LINE_COVERED");
            Integer packageIdx = idx.get("PACKAGE");

            if (lineMissedIdx == null || lineCoveredIdx == null || packageIdx == null) {
                System.err.println("Unexpected jacoco.csv header. Required columns: PACKAGE, LINE_MISSED, LINE_COVERED");
                System.exit(4);
            }

            double missedPkg = 0;
            double coveredPkg = 0;
            int classesPkg = 0;

            double missedAll = 0;
            double coveredAll = 0;

            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] cols = line.split(",", -1);

                String pkg = safeGet(cols, packageIdx);
                double missed = parseDouble(safeGet(cols, lineMissedIdx));
                double covered = parseDouble(safeGet(cols, lineCoveredIdx));

                missedAll += missed;
                coveredAll += covered;

                if (pkg != null && pkg.startsWith(packagePrefix)) {
                    missedPkg += missed;
                    coveredPkg += covered;
                    classesPkg++;
                }
            }

            double totalPkg = missedPkg + coveredPkg;
            double ratioPkg = totalPkg == 0 ? 0.0 : (coveredPkg / totalPkg * 100.0);

            double totalAll = missedAll + coveredAll;
            double ratioAll = totalAll == 0 ? 0.0 : (coveredAll / totalAll * 100.0);

            System.out.printf("%.2f%% line coverage for '%s'%n", ratioPkg, packagePrefix);
            System.out.printf("classes=%d missed=%.0f covered=%.0f%n", classesPkg, missedPkg, coveredPkg);
            System.out.printf("%.2f%% overall line coverage%n", ratioAll);
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

    private static String safeGet(String[] cols, int idx) {
        if (idx < 0 || idx >= cols.length) return null;
        String v = cols[idx];
        return v == null ? null : v;
    }

    private static double parseDouble(String s) {
        if (s == null || s.isBlank()) return 0.0;
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}

