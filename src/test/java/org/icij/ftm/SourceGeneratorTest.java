package org.icij.ftm;


import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

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
        assertThat(sourceGenerator.generate(path)).contains("public abstract class Thing {");
        assertThat(sourceGenerator.generate(path)).contains("final String name;");
        assertThat(sourceGenerator.generate(path)).contains("public Thing (String name) {");
        assertThat(sourceGenerator.generate(path)).contains("this.name = name;");
    }

    @Test
    public void test_generate_thing_with_url_imported() throws IOException {
        Path path = getPath("Thing.yaml");
        SourceGenerator sourceGenerator = new SourceGenerator(propertiesFromMap(of("attributeMode", "FULL", "parents", Map.of("Thing", Map.of()))));
        assertThat(sourceGenerator.generate(path)).contains("final URL sourceUrl;");
        assertThat(sourceGenerator.generate(path)).contains("import java.net.URL;");
    }

    @Test
    public void test_call_super_in_daughter_class() throws IOException {
        Path path = getPath("CallForTenders.yaml");
        SourceGenerator sourceGenerator = new SourceGenerator(propertiesFromMap(of("parents", Utils.findParents(new File[] {
                getPath("Thing.yaml").toFile(),
                path.toFile()
        }))));
        assertThat(sourceGenerator.generate(path)).contains("extends Thing");
        assertThat(sourceGenerator.generate(path)).contains("super(name);");
        assertThat(sourceGenerator.generate(path)).contains("public CallForTenders (String name, String title, LegalEntity authority) {");
        assertThat(sourceGenerator.generate(path)).contains("this.title = title;");
        assertThat(sourceGenerator.generate(path)).contains("this.authority = authority;");
    }

    @Test
    public void test_call_super_in_daughter_class_should_not_define_parent_property() throws IOException {
        Path path = getPath("LegalEntity.yaml");
        SourceGenerator sourceGenerator = new SourceGenerator(propertiesFromMap(of("parents", Utils.findParents(new File[] {
                getPath("Thing.yaml").toFile(),
                path.toFile()
        }))));
        assertThat(sourceGenerator.generate(path)).contains("public LegalEntity (String name) {");
        assertThat(sourceGenerator.generate(path)).contains("super(name);");
        assertThat(sourceGenerator.generate(path)).doesNotContain("this.name = name;");
    }

    @Test
    public void test_passport_bug() throws IOException {
        Path path = getPath("Passport.yaml");
        SourceGenerator sourceGenerator = new SourceGenerator(propertiesFromMap(of("parents", Utils.findParents(new File[] {
                getPath("Interval.yaml").toFile(),
                getPath("Identification.yaml").toFile(),
                path.toFile()
        }))));
        assertThat(sourceGenerator.generate(path)).contains("public Passport (LegalEntity holder, String number, String passportNumber) {");
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
    public void test_getting_attributes_from_inheritance() throws IOException {
        SourceGenerator sourceGenerator = new SourceGenerator(propertiesFromMap(of("parents", Utils.findParents(new File[]{
                getPath("Document.yaml").toFile(),
                getPath("PlainText.yaml").toFile(),
                getPath("HyperText.yaml").toFile(),
                getPath("Analyzable.yaml").toFile(),
                getPath("Thing.yaml").toFile(),
                getPath("Interval.yaml").toFile(),
                getPath("Folder.yaml").toFile(),
                getPath("Message.yaml").toFile()}))));
        String actualJava = sourceGenerator.generate(getPath("Message.yaml"));
        assertThat(actualJava).contains(
                "public class Message extends Document implements Interval, Folder, PlainText, HyperText {");
        assertThat(actualJava).contains(
                "public Message (String name, String fileName, String bodyText, LegalEntity sender)");
        assertThat(actualJava).contains("super(name, fileName);");
    }

    @Test
    public void test_fix_occupancy() throws IOException {
        Path path = getPath("Occupancy.yaml");
        SourceGenerator sg = new SourceGenerator(propertiesFromMap(of("parents", Utils.findParents(new File[]{
                getPath("Interval.yaml").toFile(),
                getPath("Occupancy.yaml").toFile(),
        }))));
        assertThat(sg.generate(path)).contains(
                "public record Occupancy(Person holder, Position post) implements Interval {};");
    }

    @Test
    public void test_two_levels_inheritance() throws IOException {
        Path path = getPath("Organization.yaml");
        SourceGenerator sourceGenerator = new SourceGenerator(propertiesFromMap(of("parents", Utils.findParents(new File[]{
                getPath("LegalEntity.yaml").toFile(),
                getPath("Organization.yaml").toFile(),
                getPath("Thing.yaml").toFile()}))));
        assertThat(sourceGenerator.generate(path)).contains(
                "public Organization (String name) {");
    }

    @Test
    public void test_fix_call_for_tender() throws IOException {
        Path path = getPath("CallForTenders.yaml");
        SourceGenerator sourceGenerator = new SourceGenerator(propertiesFromMap(of("parents", Utils.findParents(new File[]{
                getPath("Interval.yaml").toFile(),
                getPath("CallForTenders.yaml").toFile(),
                getPath("Thing.yaml").toFile()}))));
        assertThat(sourceGenerator.generate(path)).contains(
                "public class CallForTenders extends Thing implements Interval {");
    }

    @Test
    public void test_feat_remove_reserved_words() throws IOException {
        Path path = getPath("ReservedWords.yaml");
        assertThat(new SourceGenerator().generate(path)).contains(
                "public record ReservedWords(String caze) {};");
    }

    @Test
    public void test_license_bug() throws Exception {
        Path path = getPath("License.yaml");
        SourceGenerator sourceGenerator = new SourceGenerator(propertiesFromMap(of("parents", Utils.findParents(new File[]{
                getPath("License.yaml").toFile(),
                getPath("Contract.yaml").toFile(),
                getPath("Thing.yaml").toFile(),
                getPath("Asset.yaml").toFile()}))));
        assertThat(sourceGenerator.generate(path)).contains(
                "public License (String name, String title, LegalEntity authority)");
        assertThat(sourceGenerator.generate(path)).contains("final LegalEntity authority;");
        assertThat(sourceGenerator.generate(path)).doesNotContain("final String name;");
    }

    @Test
    public void test_generate_class_if_extends_class_ex_Pages() throws Exception {
        Path path = getPath("Pages.yaml");
        SourceGenerator sourceGenerator = new SourceGenerator(propertiesFromMap(of("parents", Utils.findParents(new File[]{
                getPath("Document.yaml").toFile(),
                getPath("Thing.yaml").toFile(),
                getPath("Analyzable.yaml").toFile(),
                getPath("Pages.yaml").toFile()
        }))));

        assertThat(sourceGenerator.generate(path)).contains("public class Pages extends Document {");
    }

    @Test
    public void test_generate_mixin_should_not_generate_class() throws Exception {
        Path path = getPath("Asset.yaml");
        SourceGenerator sourceGenerator = new SourceGenerator(propertiesFromMap(of("parents", Utils.findParents(new File[]{
                getPath("Thing.yaml").toFile(),
                getPath("Value.yaml").toFile(),
                path.toFile()
        }))));

        assertThat(sourceGenerator.generate(path)).contains("public interface Asset extends Value {");
    }

    private static Path getPath(String name) {
        return Paths.get(ClassLoader.getSystemResource(name).getPath());
    }
}