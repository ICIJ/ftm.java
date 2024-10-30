package org.icij.ftm;


import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Map.of;
import static org.fest.assertions.Assertions.assertThat;
import static org.icij.ftm.Utils.getYamlContent;
import static org.icij.ftm.Utils.pathFromLoader;
import static org.icij.ftm.Utils.propertiesFromMap;

public class SourceGeneratorTest {
    @Test(expected = IllegalStateException.class)
    public void test_generate_thing_illegal_definition() throws IOException {
        new SourceGenerator().generate(pathFromLoader("Illegal.yaml"));
    }

    @Test
    public void test_generate_value() throws IOException {
        Path path = pathFromLoader("Value.yaml");
        SourceGenerator sourceGenerator = new SourceGenerator(propertiesFromMap(of("interfaces", true)));
        assertThat(sourceGenerator.generate(path)).contains("public interface Value {");
    }
    @Test
    public void test_generate_thing() throws IOException {
        Path path = pathFromLoader("Thing.yaml");
        assertThat(new SourceGenerator().generate(path)).contains("package org.icij.ftm;");
        assertThat(new SourceGenerator().generate(path)).contains("public record Thing(String name) {};");
    }

    @Test
    public void test_generate_thing_with_thing_in_properties() throws IOException {
        Path path = pathFromLoader("Thing.yaml");
        SourceGenerator sourceGenerator = new SourceGenerator(propertiesFromMap(of("parents", Map.of("Thing", Map.of()))));
        assertThat(sourceGenerator.generate(path)).contains("package org.icij.ftm;");
        assertThat(sourceGenerator.generate(path)).contains("public abstract class Thing {");
        assertThat(sourceGenerator.generate(path)).contains("final String name;");
        assertThat(sourceGenerator.generate(path)).contains("public Thing (String name) {");
        assertThat(sourceGenerator.generate(path)).contains("this.name = name;");
    }

    @Test
    public void test_generate_thing_with_url_imported() throws IOException {
        Path path = pathFromLoader("Thing.yaml");
        SourceGenerator sourceGenerator = new SourceGenerator(propertiesFromMap(of("attributeMode", "FULL", "parents", Map.of("Thing", Map.of()))));
        assertThat(sourceGenerator.generate(path)).contains("final URL sourceUrl;");
        assertThat(sourceGenerator.generate(path)).contains("import java.net.URL;");
    }

    @Test
    public void test_call_super_in_daughter_class() throws IOException {
        Path path = pathFromLoader("CallForTenders.yaml");
        SourceGenerator sourceGenerator = new SourceGenerator(propertiesFromMap(of("parents", Utils.findParents(new File[] {
                pathFromLoader("Thing.yaml").toFile(),
                path.toFile()
        }), "models", List.of("LegalEntity"))));
        assertThat(sourceGenerator.generate(path)).contains("extends Thing");
        assertThat(sourceGenerator.generate(path)).contains("super(name);");
        assertThat(sourceGenerator.generate(path)).contains("public CallForTenders (String name, String title, LegalEntity authority) {");
        assertThat(sourceGenerator.generate(path)).contains("this.title = title;");
        assertThat(sourceGenerator.generate(path)).contains("this.authority = authority;");
    }

    @Test
    public void test_abstract_daughter_interface() throws IOException {
        Path path = pathFromLoader("CallForTenders.yaml");
        SourceGenerator sourceGenerator = new SourceGenerator(propertiesFromMap(of("parents", Utils.findParents(new File[] {
                pathFromLoader("Thing.yaml").toFile(),
                path.toFile()
        }), "models", List.of("LegalEntity"), "interfaces", true)));
        assertThat(sourceGenerator.generate(path)).contains("extends Thing, Interval");
        assertThat(sourceGenerator.generate(path)).contains("String title();");
        assertThat(sourceGenerator.generate(path)).contains("LegalEntity authority();");
    }

    @Test
    public void test_call_super_in_daughter_class_should_not_define_parent_property() throws IOException {
        Path path = pathFromLoader("LegalEntity.yaml");
        SourceGenerator sourceGenerator = new SourceGenerator(propertiesFromMap(of("parents", Utils.findParents(new File[] {
                pathFromLoader("Thing.yaml").toFile(),
                path.toFile()
        }))));
        assertThat(sourceGenerator.generate(path)).contains("public LegalEntity (String name) {");
        assertThat(sourceGenerator.generate(path)).contains("super(name);");
        assertThat(sourceGenerator.generate(path)).doesNotContain("this.name = name;");
    }

    @Test
    public void test_passport_bug() throws IOException {
        Path path = pathFromLoader("Passport.yaml");
        SourceGenerator sourceGenerator = new SourceGenerator(propertiesFromMap(of("parents", Utils.findParents(new File[] {
                pathFromLoader("Interval.yaml").toFile(),
                pathFromLoader("Identification.yaml").toFile(),
                path.toFile()
        }), "models", List.of("LegalEntity"))));
        assertThat(sourceGenerator.generate(path)).contains("public Passport (LegalEntity holder, String number, String passportNumber) {");
    }

    @Test
    public void test_generate_with_int_property() throws IOException {
        Path path = pathFromLoader("Int.yaml");
        assertThat(new SourceGenerator().generate(path)).contains("public record Int(int number) {};");
    }

    @Test
    public void test_generate_with_two_props() throws IOException {
        Path path = pathFromLoader("TwoProps.yaml");
        assertThat(new SourceGenerator().generate(path)).contains("public record TwoProps(String name, int number) {};");
    }

    @Test
    public void test_generate_abstract() throws IOException {
        Path path = pathFromLoader("Analyzable.yaml");
        assertThat(new SourceGenerator().generate(path)).contains("public interface Analyzable {");
    }

    @Test
    public void test_getting_attributes_from_inheritance() throws IOException {
        SourceGenerator sourceGenerator = new SourceGenerator(propertiesFromMap(of("parents", Utils.findParents(new File[]{
                pathFromLoader("Document.yaml").toFile(),
                pathFromLoader("PlainText.yaml").toFile(),
                pathFromLoader("HyperText.yaml").toFile(),
                pathFromLoader("Analyzable.yaml").toFile(),
                pathFromLoader("Thing.yaml").toFile(),
                pathFromLoader("Interval.yaml").toFile(),
                pathFromLoader("Folder.yaml").toFile(),
                pathFromLoader("Message.yaml").toFile()}), "models", List.of("LegalEntity"))));
        String actualJava = sourceGenerator.generate(pathFromLoader("Message.yaml"));
        assertThat(actualJava).contains(
                "public abstract class Message extends Document implements Interval, Folder, PlainText, HyperText {");
        assertThat(actualJava).contains(
                "public Message (String name, String fileName, String bodyText, LegalEntity sender)");
        assertThat(actualJava).contains("super(name, fileName);");
    }

    @Test
    public void test_fix_occupancy() throws IOException {
        Path path = pathFromLoader("Occupancy.yaml");
        SourceGenerator sg = new SourceGenerator(propertiesFromMap(of("parents", Utils.findParents(new File[]{
                pathFromLoader("Interval.yaml").toFile(),
                pathFromLoader("Occupancy.yaml").toFile(),
        }), "models", List.of("Person", "Position"))));
        assertThat(sg.generate(path)).contains(
                "public record Occupancy(Person holder, Position post) implements Interval {};");
    }

    @Test
    public void test_two_levels_inheritance() throws IOException {
        Path path = pathFromLoader("Organization.yaml");
        SourceGenerator sourceGenerator = new SourceGenerator(propertiesFromMap(of("parents", Utils.findParents(new File[]{
                pathFromLoader("LegalEntity.yaml").toFile(),
                pathFromLoader("Organization.yaml").toFile(),
                pathFromLoader("Thing.yaml").toFile()}))));
        assertThat(sourceGenerator.generate(path)).contains(
                "public Organization (String name) {");
    }

    @Test
    public void test_fix_call_for_tender() throws IOException {
        Path path = pathFromLoader("CallForTenders.yaml");
        SourceGenerator sourceGenerator = new SourceGenerator(propertiesFromMap(of("parents", Utils.findParents(new File[]{
                pathFromLoader("Interval.yaml").toFile(),
                pathFromLoader("CallForTenders.yaml").toFile(),
                pathFromLoader("Thing.yaml").toFile()}))));
        assertThat(sourceGenerator.generate(path)).contains(
                "public abstract class CallForTenders extends Thing implements Interval {");
    }

    @Test
    public void test_feat_remove_reserved_words() throws IOException {
        Path path = pathFromLoader("ReservedWords.yaml");
        assertThat(new SourceGenerator().generate(path)).contains(
                "public record ReservedWords(String caze) {};");
    }

    @Test
    public void test_license_bug() throws Exception {
        Path path = pathFromLoader("License.yaml");
        SourceGenerator sourceGenerator = new SourceGenerator(propertiesFromMap(of("parents", Utils.findParents(new File[]{
                pathFromLoader("License.yaml").toFile(),
                pathFromLoader("Contract.yaml").toFile(),
                pathFromLoader("Thing.yaml").toFile(),
                pathFromLoader("Asset.yaml").toFile()}), "models", List.of("LegalEntity"))));
        assertThat(sourceGenerator.generate(path)).contains(
                "public License (String name, String title, LegalEntity authority)");
        assertThat(sourceGenerator.generate(path)).contains("final LegalEntity authority;");
        assertThat(sourceGenerator.generate(path)).doesNotContain("final String name;");
    }

    @Test
    public void test_generate_class_if_extends_class_ex_Pages() throws Exception {
        Path path = pathFromLoader("Pages.yaml");
        SourceGenerator sourceGenerator = new SourceGenerator(propertiesFromMap(of("parents", Utils.findParents(new File[]{
                pathFromLoader("Document.yaml").toFile(),
                pathFromLoader("Thing.yaml").toFile(),
                pathFromLoader("Analyzable.yaml").toFile(),
                pathFromLoader("Pages.yaml").toFile()
        }))));

        assertThat(sourceGenerator.generate(path)).contains("public abstract class Pages extends Document {");
    }

    @Test
    public void test_generate_mixin_should_not_generate_class() throws Exception {
        Path path = pathFromLoader("Asset.yaml");
        SourceGenerator sourceGenerator = new SourceGenerator(propertiesFromMap(of("parents", Utils.findParents(new File[]{
                pathFromLoader("Thing.yaml").toFile(),
                pathFromLoader("Value.yaml").toFile(),
                path.toFile()
        }))));

        assertThat(sourceGenerator.generate(path)).contains("public interface Asset extends Value {");
    }

    @Test
    public void test_generate_methods() throws Exception {
        SourceGenerator sourceGenerator = new SourceGenerator(propertiesFromMap(Map.of("models", List.of("Folder"))));
        String code = sourceGenerator.generateMethods(new Model(getYamlContent(pathFromLoader("Document.yaml").toFile()), new HashMap<>(), Model.Mode.FEATURED));
        assertThat(code).contains("String fileName();");
        assertThat(code).contains("String mimeType();");
        assertThat(code).contains("Folder parent();");
    }
}