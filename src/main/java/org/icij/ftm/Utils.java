package org.icij.ftm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class Utils {
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
        List<Map<String, Object>> yamlFileList = new ObjectMapper().readValue(response.body(), new TypeReference<>() {});

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

    static Map<String, Map<String, Object>> findParents(File[] yamlFiles) throws FileNotFoundException {
        Set<String> parents = new LinkedHashSet<>();
        Map<String, Map<String, Object>> modelsMap = new HashMap<>();
        for (File file: yamlFiles) {
            Map<String, Object> yamlContent = SourceGenerator.getYamlContent(file);
            String modelName = yamlContent.keySet().iterator().next();
            Map<String, Object> model = (Map<String, Object>) yamlContent.values().iterator().next();
            List<String> extendz = (List<String>) ofNullable(model.get("extends")).orElse(new ArrayList<>());
            parents.addAll(extendz);
            modelsMap.put(modelName, model);
        }
        return modelsMap.entrySet().stream().filter(e -> parents.contains(e.getKey())).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    static String getJavaFileName(File yamlFile) {
        int dotIndex = yamlFile.getName().lastIndexOf('.');
        return yamlFile.getName().substring(0, dotIndex) + ".java";
    }
}
