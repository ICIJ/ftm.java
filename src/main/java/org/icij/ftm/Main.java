package org.icij.ftm;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import static org.icij.ftm.Utils.propertiesFromMap;

/**
 * main class to be called for downloading yaml files and generating java source files.
 * Java source files are generated for maven project in target/generated-sources.
 */
public class Main {
    public static final String SCHEMA_URL = "https://api.github.com/repos/alephdata/followthemoney/contents/followthemoney/schema";
    public static final String ATTRIBUTE_MODE_KEY = "attributeMode";
    public static final String DEFAULT_ATTRIBUTE_MODE = "FEATURED";
    public static final String INTERFACES_KEY = "interfaces";
    public static final String DEFAULT_INTERFACES_VALUE = "false";

    public static void main(String[] args) throws Exception {
        Path destDir = Path.of("target", "generated-sources", "org", "icij", "ftm");
        destDir.toFile().mkdirs();

        try {
            Map<String, String> argsMap = Utils.parseArgs(args);

            Model.Mode attributeMode = Model.Mode.valueOf(argsMap.getOrDefault(ATTRIBUTE_MODE_KEY, DEFAULT_ATTRIBUTE_MODE));
            boolean interfaces = Boolean.parseBoolean(argsMap.getOrDefault(INTERFACES_KEY, DEFAULT_INTERFACES_VALUE));

            Path yamlFilesDir = Utils.downloadYamlModels(URI.create(SCHEMA_URL));
            File[] yamlFiles = Objects.requireNonNull(yamlFilesDir.toFile().listFiles());
            Properties properties = propertiesFromMap(Map.of(
                    "parents", Utils.findParents(yamlFiles, attributeMode),
                    "models", Arrays.stream(yamlFiles).map(File::getName).map(s -> s.substring(0, s.indexOf("."))).toList(),
                    "attributeMode", attributeMode.name(),
                    "interfaces", interfaces
            ));

            System.out.printf("generating classes into %s for FtM with %s%n", destDir, properties);
            SourceGenerator sourceGenerator = new SourceGenerator(properties);

            for (File yamlFile: yamlFiles) {
                String javaSource = sourceGenerator.generate(yamlFile.toPath());
                Files.writeString(destDir.resolve(Utils.getJavaFileName(yamlFile)), javaSource);
            }
        } catch (IllegalArgumentException argex) {
            System.out.println(argex.getMessage());
            System.out.println("usage: Main <properties>");
            System.out.printf("\t--%s: FtM properties mode (REQUIRED, FEATURED, FULL default %s)%n", ATTRIBUTE_MODE_KEY, DEFAULT_ATTRIBUTE_MODE);
            System.out.printf("\t--%s: only generate interfaces (default %s)%n", INTERFACES_KEY, DEFAULT_INTERFACES_VALUE);
        }
    }
}
