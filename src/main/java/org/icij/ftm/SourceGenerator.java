package org.icij.ftm;

import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;

public class SourceGenerator {
    private final Properties properties;
    private final Load yaml;
    private final Map<String, String> nativeTypeMapping = Map.of(
            "number", "int",
            "url", "Url"
    );

    public SourceGenerator() {
        this(new Properties());
    }

    public SourceGenerator(Properties properties) {
        this.properties = properties;
        yaml = new Load(LoadSettings.builder().build());
    }

    public String generate(Path path) throws IOException {
        Map<String, Object> model = (Map<String, Object>) yaml.loadFromInputStream(new FileInputStream(path.toFile()));
        if (model.size() > 1) {
            throw new IllegalStateException(format("model should contain one definition, found %s", model.keySet()));
        }
        String modelName = model.keySet().iterator().next();
        Map<String, Object> modelDesc = (Map<String, Object>) model.get(modelName);

        Map<String, Object> properties = (Map<String, Object>) modelDesc.get("properties");
        StringBuilder stringProperties = new StringBuilder();

        List<String> required = (List<String>) modelDesc.get("required");
        if (required != null) {
            for (String prop : required) {
                Map<String, Object> property = (Map<String, Object>) ofNullable(properties).map(p -> p.get(prop)).orElse(null);
                if (property != null) {
                    if ("entity".equals(property.get("type"))) {
                        stringProperties.append(ofNullable(property.get("range")).orElse("String"))
                                .append(" ")
                                .append(prop);
                    } else {
                        stringProperties.append(ofNullable(nativeTypeMapping.get(property.get("type"))).orElse("String"))
                                .append(" ")
                                .append(prop);
                    }
                } else {
                    // it seems that there are some fields that are not listed but in the required list
                    // we should do a PR to fix that but for now we are putting a String property
                    stringProperties.append(format("String %s", prop));
                }
                if (!prop.equals(required.get(required.size() - 1))) {
                    stringProperties.append(", ");
                }
            }

            List<String> extendz = (List<String>) ofNullable(modelDesc.get("extends")).orElse(new ArrayList());
            String implementList = extendz.isEmpty() ? "" : format("implements %s " , String.join(", ", extendz));

            return format("""
                package org.icij.ftm;
                
                public record %s(%s) %s{};
                """, modelName, stringProperties, implementList);
        } else {
            return format("""
                package org.icij.ftm;
                
                public interface %s {};
                """, modelName);
        }
    }
}
