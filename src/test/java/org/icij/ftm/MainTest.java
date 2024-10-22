package org.icij.ftm;

import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.fest.assertions.Assertions.assertThat;

public class MainTest {
    @Test
    public void test_download_files() throws Exception {
        Path sourceFiles = new Main().downloadYamlModels();
        assertThat(sourceFiles.toFile().listFiles()).hasSize(70);
    }

    @Test
    public void test_find_parents() throws Exception {
        List<String> models = List.of("Interval.yaml", "CallForTenders.yaml", "Thing.yaml");
        List<File> modelFiles = getFiles(models);

        assertThat(Main.findParents(modelFiles.toArray(new File[]{}))).hasSize(2);
        assertThat(Main.findParents(modelFiles.toArray(new File[]{})).keySet()).contains("Interval", "Thing");
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
