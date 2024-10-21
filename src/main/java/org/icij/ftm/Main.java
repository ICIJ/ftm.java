package org.icij.ftm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static java.util.stream.Collectors.toList;

public class Main {
    public static final String SCHEMA_URL = "https://api.github.com/repos/alephdata/followthemoney/contents/followthemoney/schema";

    Path getSourceFiles() throws IOException, InterruptedException {
        Path tempDirectory = Files.createTempDirectory("ftm.java");

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(SCHEMA_URL)).build();
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

    public static void main(String[] args) throws Exception {
        Path destDir = Path.of("target", "generated-sources", "org", "icij", "ftm");
        destDir.toFile().mkdirs();
        Path yamlFiles = new Main().getSourceFiles();
        for (File yamlFile: Objects.requireNonNull(yamlFiles.toFile().listFiles())) {
            String javaSource = new SourceGenerator().generate(yamlFile.toPath());
            Files.writeString(destDir.resolve(getJavaFileName(yamlFile)), javaSource);
        }
    }

    private static String getJavaFileName(File yamlFile) {
        int dotIndex = yamlFile.getName().lastIndexOf('.');
        String name = yamlFile.getName().substring(0, dotIndex);
        return name + ".java";
    }
}
