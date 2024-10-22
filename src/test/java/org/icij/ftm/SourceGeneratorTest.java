package org.icij.ftm;


import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static java.util.Map.of;
import static org.fest.assertions.Assertions.assertThat;
import static org.icij.ftm.Utils.propertiesFromMap;

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
    public void test_generate_thing_with_thing_in_properties() throws IOException {
        Path path = getPath("Thing.yaml");
        SourceGenerator sourceGenerator = new SourceGenerator(propertiesFromMap(of("parents", Map.of("Thing", Map.of()))));
        assertThat(sourceGenerator.generate(path)).contains("package org.icij.ftm;");
        assertThat(sourceGenerator.generate(path)).contains("public class Thing {");
        assertThat(sourceGenerator.generate(path)).contains("final String name;");
        assertThat(sourceGenerator.generate(path)).contains("public Thing (String name) {");
        assertThat(sourceGenerator.generate(path)).contains("this.name = name;");
    }

    @Test
    public void test_call_super_in_daughter_class() throws IOException {
        Path path = getPath("CallForTenders.yaml");
        SourceGenerator sourceGenerator = new SourceGenerator(propertiesFromMap(of("parents", Map.of(
                "Thing", Map.of("required", List.of("name"), "properties", Map.of("name", Map.of("type", "name")))),
                "Interval", Map.of()
        )));
        assertThat(sourceGenerator.generate(path)).contains("extends Thing");
        assertThat(sourceGenerator.generate(path)).contains("super(name);");
        assertThat(sourceGenerator.generate(path)).contains("public CallForTenders (String name, String title, LegalEntity authority) {");
        assertThat(sourceGenerator.generate(path)).contains("this.title = title;");
        assertThat(sourceGenerator.generate(path)).contains("this.authority = authority;");
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

    @Test
    public void test_feat_remove_reserved_words() throws IOException {
        Path path = getPath("ReservedWords.yaml");
        assertThat(new SourceGenerator().generate(path)).contains(
                "public record ReservedWords(String caze) {};");
    }

    private static Path getPath(String name) {
        return Paths.get(ClassLoader.getSystemResource(name).getPath());
    }
}