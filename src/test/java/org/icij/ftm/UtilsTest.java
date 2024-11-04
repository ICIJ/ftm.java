package org.icij.ftm;

import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.fest.assertions.Assertions.assertThat;
import static org.icij.ftm.Model.Mode.FEATURED;
import static org.icij.ftm.Utils.propertiesFromMap;

public class UtilsTest {
    @Test
    public void test_download_files() throws Exception {
        Path sourceFiles = Utils.downloadYamlModels(URI.create(Main.SCHEMA_URL));
        assertThat(sourceFiles.toFile().listFiles()).hasSize(70);
    }

    @Test
    public void test_find_parents() throws Exception {
        List<String> models = List.of("Interval.yaml", "CallForTenders.yaml", "Thing.yaml");
        List<File> modelFiles = getFiles(models);

        assertThat(Utils.findParents(modelFiles.toArray(new File[]{}))).hasSize(2);
        assertThat(Utils.findParents(modelFiles.toArray(new File[]{})).keySet()).contains("Interval", "Thing");
    }

    @Test
    public void test_parse_args() {
        assertThat(Utils.parseArgs(new String[] {"--attributeMode", "FEATURED"})).isEqualTo(propertiesFromMap(Map.of("attributeMode", "FEATURED")));
    }

    @Test
    public void test_parse_two_args() {
        assertThat(Utils.parseArgs(new String[] {"--attributeMode", "FEATURED", "--interfaces", "true"}))
                .isEqualTo(propertiesFromMap(Map.of(
                        "attributeMode", "FEATURED",
                        "interfaces", "true"
                )));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_parse_unknown_args() {
        Utils.parseArgs(new String[] {"--foo", "bar"});
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_parse_missing_two_dashes() {
        Utils.parseArgs(new String[] {"attributeMode", "bar"});
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_parse_missing_value() {
        Utils.parseArgs(new String[] {"--interfaces"});
    }

    @Test
    public void test_parse_help() {
        try {
            Utils.parseArgs(new String[]{"--help"});
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage()).isEqualTo("Help:");
        }
    }

    private static List<File> getFiles(List<String> models) {
        return models.stream().map(ClassLoader::getSystemResource).map(url -> {
            try {
                return url.toURI();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }).map(File::new).collect(Collectors.toList());
    }
}
