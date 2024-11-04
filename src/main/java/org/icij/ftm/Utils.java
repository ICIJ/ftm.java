package org.icij.ftm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * Utils methods, that would need at some point to be in
 * other classes.
 */
public class Utils {
    private static final Load yaml = new Load(LoadSettings.builder().build());

    public static Properties propertiesFromMap(Map<String, Object> map) {
        Properties props = new Properties();
        props.putAll(map);
        return props;
    }

    static Path downloadYamlModels(URI downloadUri) throws IOException, InterruptedException {
        Path tempDirectory = Files.createTempDirectory("ftm.java");

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(downloadUri).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        List<Map<String, Object>> yamlFileList = new ObjectMapper().readValue(response.body(), new TypeReference<>() {
        });

        List<HttpRequest> requests = yamlFileList.stream()
                .map(yamlFileDesc -> HttpRequest.newBuilder().uri(URI.create((String) yamlFileDesc.get("download_url"))))
                .map(HttpRequest.Builder::build)
                .collect(toList());

        // parallelize downloads
        CompletableFuture.allOf(requests.stream()
                .map(r -> client.sendAsync(r, HttpResponse.BodyHandlers.ofFile(tempDirectory.resolve(Path.of(r.uri().getPath()).getFileName()))))
                .toArray(CompletableFuture<?>[]::new)).join();

        return tempDirectory;
    }

    static Map<String, Model> findParents(File[] yamlFiles) throws FileNotFoundException {
        return findParents(yamlFiles, Model.Mode.REQUIRED);
    }

    static Map<String, Model> findParents(File[] yamlFiles, Model.Mode attributeMode) throws FileNotFoundException {
        Set<String> parentNames = new LinkedHashSet<>();
        Map<String, Map<String, Object>> modelsMap = new HashMap<>();
        for (File file : yamlFiles) {
            Map<String, Object> yamlContent = getYamlContent(file);
            Model model = new Model(yamlContent);
            parentNames.addAll(model.getExtends());
            modelsMap.put(model.name(), yamlContent);
        }
        Map<String, Map<String, Object>> mapOfMap = modelsMap.entrySet().stream().filter(e -> parentNames.contains(e.getKey())).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<String, Model> parents = new HashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : mapOfMap.entrySet()) {
            parents.put(entry.getKey(), new Model(entry.getValue(), parents, attributeMode));
        }
        return parents;
    }

    static String getJavaFileName(File yamlFile) {
        int dotIndex = yamlFile.getName().lastIndexOf('.');
        return yamlFile.getName().substring(0, dotIndex) + ".java";
    }

    static Map<String, Object> getYamlContent(File yamlFile) throws FileNotFoundException {
        return (Map<String, Object>) yaml.loadFromInputStream(new FileInputStream(yamlFile));
    }

    static Path pathFromLoader(String name) {
        return Paths.get(ClassLoader.getSystemResource(name).getPath());
    }

    public static Map<String, String> parseArgs(String[] args) {
        List<String> argumentList = List.of("attributeMode", "interfaces", "help");
        Scanner scanner = new Scanner(String.join(" ", args));
        Map<String, String> properties = new HashMap<>();
        while (scanner.hasNext()) {
            String arg = scanner.next();
            String argName = arg.substring(2);
            if (arg.startsWith("--") && argumentList.contains(argName)) {
                if ("help".equals(argName)) {
                    throw new IllegalArgumentException("Help:");
                }
                if (!scanner.hasNext()) {
                    throw new IllegalArgumentException("arg " + argName + " should have a value ");
                }

                String value = scanner.next();
                properties.put(argName, value);
            } else {
                throw new IllegalArgumentException("unknown arg " + argName);
            }
        }
        return properties;
    }
}
