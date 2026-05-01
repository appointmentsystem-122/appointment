package com.appointmentscheduler.application;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link AppConfig} private load paths: merge of {@code application-local.properties}
 * and exception branches when malformed files are present on the test classpath (target/test-classes).
 */
@ResourceLock("AppConfigProps")
class AppConfigClasspathLoadBranchesTest {

    private static final Path SRC_TEST_RESOURCES = Path.of("src", "test", "resources");

    private Path testClassesDir;
    private Path classpathProps;
    private Path localProps;

    @BeforeEach
    void setupPathsAndRestoreGoodFiles() throws IOException {
        testClassesDir = Path.of("target", "test-classes");
        Assumptions.assumeTrue(
                Files.isDirectory(testClassesDir),
                "Run tests from module root so target/test-classes exists");
        Assumptions.assumeTrue(
                Files.isDirectory(SRC_TEST_RESOURCES),
                "src/test/resources must exist");
        classpathProps = testClassesDir.resolve("application.properties");
        localProps = testClassesDir.resolve("application-local.properties");
        copyStableTestResourcesToTarget();
        AppConfig.reloadClasspathPropertiesForTest();
    }

    @AfterEach
    void restoreTargetTestClassesFromSourceTree() throws IOException {
        if (classpathProps != null && localProps != null) {
            copyStableTestResourcesToTarget();
            AppConfig.reloadClasspathPropertiesForTest();
        }
    }

    private void copyStableTestResourcesToTarget() throws IOException {
        Path mainProps = SRC_TEST_RESOURCES.resolve("application.properties");
        Path localSrc = SRC_TEST_RESOURCES.resolve("application-local.properties");
        Assumptions.assumeTrue(Files.exists(mainProps), "Missing " + mainProps);
        Files.createDirectories(testClassesDir);
        Files.copy(mainProps, classpathProps, StandardCopyOption.REPLACE_EXISTING);
        if (Files.exists(localSrc)) {
            Files.copy(localSrc, localProps, StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.deleteIfExists(localProps);
        }
    }

    @Test
    void reload_mergesApplicationLocalProperties() {
        assertThat(AppConfig.get("local.test.merge", "")).isEqualTo("ok");
    }

    @Test
    void reload_malformedApplicationLocalProperties_catchesAndKeepsClasspathConfig() throws Exception {
        Files.writeString(
                localProps,
                "bad=\\u00G0\n",
                StandardCharsets.ISO_8859_1);

        AppConfig.reloadClasspathPropertiesForTest();

        assertThat(AppConfig.get("app.name", "")).isNotBlank();
    }

    @Test
    void reload_malformedApplicationProperties_catchesThenMayMergeLocal() throws Exception {
        Files.writeString(
                classpathProps,
                "bad=\\u00G0\n",
                StandardCharsets.ISO_8859_1);

        AppConfig.reloadClasspathPropertiesForTest();

        assertThat(AppConfig.get("local.test.merge", "")).isEqualTo("ok");
    }

    @Test
    void loadPropertiesFromStream_nullInput_isNoOp() {
        AppConfig.loadPropertiesFromStreamForTest(null, "dbg", "warn {}");
    }
}
