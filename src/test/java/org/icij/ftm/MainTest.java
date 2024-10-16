package org.icij.ftm;

import org.junit.Test;

import java.nio.file.Path;

import static org.fest.assertions.Assertions.assertThat;

public class MainTest {
    @Test
    public void test_download_files() throws Exception {
        Path sourceFiles = new Main().getSourceFiles();
        assertThat(sourceFiles.toFile().listFiles()).hasSize(70);
    }
}
