package com.generic.appointments.ui;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatCode;

class GenericMainConsoleTest {

    @Test
    void mainRunsWithoutCancel() {
        runMainWithInput("n\n");
    }

    @Test
    void mainRunsWithCancel() {
        runMainWithInput("y\n");
    }

    private static void runMainWithInput(String input) {
        InputStream oldIn = System.in;
        PrintStream oldOut = System.out;
        try {
            System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(OutputStream.nullOutputStream(), true, StandardCharsets.UTF_8));
            assertThatCode(() -> Main.main(new String[0])).doesNotThrowAnyException();
        } finally {
            System.setIn(oldIn);
            System.setOut(oldOut);
        }
    }
}
