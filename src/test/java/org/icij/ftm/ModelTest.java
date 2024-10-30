package org.icij.ftm;


import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class ModelTest {

    @Test
    public void test_is_abstract() throws Exception {
        assertThat(new Model(loadYaml("Analyzable.yaml")).isAbstract()).isTrue();
        assertThat(new Model(loadYaml("Document.yaml")).isAbstract()).isFalse();
    }

    @Test
    public void test_get_attributes() throws Exception {
        assertThat(new Model(loadYaml("Document.yaml"), Utils.findParents(new File[]{
                getFile("Document.yaml"),
                getFile("Analyzable.yaml"),
                getFile("Thing.yaml")
        })).attributes()).isEqualTo(List.of("fileName"));
    }

    @Test
    public void test_get_attributes_featured() throws Exception {
        assertThat(new Model(loadYaml("Document.yaml"), Utils.findParents(new File[]{
                getFile("Document.yaml"),
                getFile("Analyzable.yaml"),
                getFile("Thing.yaml")
        }), Model.Mode.FEATURED).attributes()).isEqualTo(List.of("fileName", "title", "mimeType", "parent"));
    }

    @Test
    public void test_get_attributes_full() throws Exception {
        assertThat(new Model(loadYaml("Document.yaml"), Utils.findParents(new File[]{
                getFile("Document.yaml"),
                getFile("Analyzable.yaml"),
                getFile("Thing.yaml")
        }), Model.Mode.FULL).attributes()).isEqualTo(List.of(
                "fileName",
                "title",
                "mimeType",
                "parent",
                "contentHash",
                "author",
                "generator",
                "crawler",
                "fileSize",
                "extension",
                "encoding",
                "bodyText",
                "messageId",
                "language",
                "translatedLanguage",
                "translatedText",
                "date",
                "authoredAt",
                "publishedAt",
                "ancestors",
                "processingStatus",
                "processingError",
                "processingAgent",
                "processedAt"));
    }

    @Test
    public void test_get_recursive_property() throws Exception {
        Model document = new Model(loadYaml("Document.yaml"), Utils.findParents(new File[]{
                getFile("Document.yaml"),
                getFile("Analyzable.yaml"),
                getFile("Thing.yaml")
        }));
        assertThat(document.property("ibanMentioned")).isNotNull();
    }

    @Test
    public void test_get_property_type() throws Exception {
        Model document = new Model(loadYaml("Document.yaml"), Utils.findParents(new File[]{
                getFile("Document.yaml"),
                getFile("Analyzable.yaml"),
                getFile("Thing.yaml")
        }));
        assertThat(document.type("ibanMentioned")).isEqualTo("iban");
        assertThat(document.type("extension")).isEqualTo("string");
        assertThat(document.type("parent")).isEqualTo("Folder");
        assertThat(document.type("title")).isEqualTo("string");
    }

    private static Map<String, Object> loadYaml(String name) throws FileNotFoundException {
        return Utils.getYamlContent(getFile(name));
    }

    private static File getFile(String name) {
        return Paths.get(ClassLoader.getSystemResource(name).getPath()).toFile();
    }
}