package org.icij.ftm;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.toMap;
import static org.icij.ftm.Utils.propertiesFromMap;

/**
 * main class to be called for downloading yaml files and generating java source files.
 * Java source files are generated for maven project in target/generated-sources.
 */
public class Main {
    public static final String SCHEMA_URL = "https://api.github.com/repos/alephdata/followthemoney/contents/followthemoney/schema";

    public static void main(String[] args) throws Exception {
        Path destDir = Path.of("target", "generated-sources", "org", "icij", "ftm");
        destDir.toFile().mkdirs();

        Path yamlFilesDir = Utils.downloadYamlModels(URI.create(SCHEMA_URL));
        File[] yamlFiles = Objects.requireNonNull(yamlFilesDir.toFile().listFiles());
        SourceGenerator sourceGenerator = new SourceGenerator(propertiesFromMap(Map.of("parents", Utils.findParents(yamlFiles))));
        for (File yamlFile: yamlFiles) {
            String javaSource = sourceGenerator.generate(yamlFile.toPath());
            Files.writeString(destDir.resolve(Utils.getJavaFileName(yamlFile)), javaSource);
        }
    }

}
