package org.icij.ftm;


import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.fest.assertions.Assertions.assertThat;

public class SourceGeneratorTest {
    @Test(expected = IllegalStateException.class)
    public void test_generate_thing_illegal_definition() throws IOException {
        new SourceGenerator().generate(getPath("Illegal.yaml"));
    }

    @Test
    public void test_generate_thing() throws IOException {
        Path path = getPath("Thing.yaml");
        assertThat(new SourceGenerator().generate(path)).contains("package org.icij.ftm;");
        assertThat(new SourceGenerator().generate(path)).contains("public record Thing(String name);");
    }

    private static Path getPath(String name) {
        return Paths.get(ClassLoader.getSystemResource(name).getPath());
    }
}