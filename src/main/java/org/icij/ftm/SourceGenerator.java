package org.icij.ftm;

import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;

public class SourceGenerator {
    private final Properties properties;
    private final Load yaml;
    private final Map<String, String> typeMapping = Map.of(
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
        StringBuffer stringProperties = new StringBuffer();

        List<String> required = (List<String>) modelDesc.get("required");
        for (String prop: required) {
            Map<String, Object> property = (Map<String, Object>) properties.get(prop);
            stringProperties.append(ofNullable(typeMapping.get(property.get("type"))).orElse("String"))
                    .append(" ")
                    .append(property.get("label").toString().toLowerCase(Locale.getDefault()));
            if (!prop.equals(required.get(required.size() - 1))) {
                stringProperties.append(", ");
            }
        }

        return format("""
                package org.icij.ftm;
                public record %s(%s);
                """, modelName, stringProperties);
    }
}
