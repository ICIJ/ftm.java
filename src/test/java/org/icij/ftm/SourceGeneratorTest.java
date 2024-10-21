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
        assertThat(new SourceGenerator().generate(path)).contains("public record Thing(String name) {};");
    }

    @Test
    public void test_generate_with_int_property() throws IOException {
        Path path = getPath("Int.yaml");
        assertThat(new SourceGenerator().generate(path)).contains("public record Int(int number) {};");
    }

    @Test
    public void test_generate_with_two_props() throws IOException {
        Path path = getPath("TwoProps.yaml");
        assertThat(new SourceGenerator().generate(path)).contains("public record TwoProps(String name, int number) {};");
    }

    @Test
    public void test_generate_abstract() throws IOException {
        Path path = getPath("Analyzable.yaml");
        assertThat(new SourceGenerator().generate(path)).contains("public interface Analyzable {};");
    }

    @Test
    public void test_bug_missing_first_prop() throws IOException {
        Path path = getPath("Message.yaml");
        assertThat(new SourceGenerator().generate(path)).contains(
                "public record Message(String bodyText, LegalEntity sender) implements Interval, Folder, PlainText, HyperText {};");
    }

    @Test
    public void test_fix_occupancy() throws IOException {
        Path path = getPath("Occupancy.yaml");
        assertThat(new SourceGenerator().generate(path)).contains(
                "public record Occupancy(Person holder, Position post) implements Interval {};");
    }

    @Test
    public void test_fix_call_for_tender() throws IOException {
        Path path = getPath("CallForTenders.yaml");
        assertThat(new SourceGenerator().generate(path)).contains(
                "public record CallForTenders(String title, LegalEntity authority) implements Thing, Interval {};");
    }

    private static Path getPath(String name) {
        return Paths.get(ClassLoader.getSystemResource(name).getPath());
    }
}