package org.icij.ftm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;

public class SourceGenerator {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Properties properties;
    private static final Load yaml = new Load(LoadSettings.builder().build());
    ;
    private final Map<String, String> nativeTypeMapping = Map.of(
            "number", "int",
            "url", "Url"
    );
    private final Map<String, String> jvmReservedWords = Map.of(
            "case", "caze"
    );

    public SourceGenerator() {
        this(new Properties());
    }

    public SourceGenerator(Properties properties) {
        this.properties = properties;
    }

    public String generate(Path path) throws IOException {
        logger.info("generating java class for {} model", path.getFileName());
        Map<String, Object> model = getYamlContent(path.toFile());
        if (model.size() > 1) {
            throw new IllegalStateException(format("model should contain one definition, found %s", model.keySet()));
        }
        String modelName = model.keySet().iterator().next();
        Map<String, Object> modelDesc = (Map<String, Object>) model.get(modelName);
        Map<String, Map<String, Object>> parents = (Map<String, Map<String, Object>>)
                ofNullable(this.properties.get("parents")).orElse(new HashMap<>());

        Map<String, Object> properties = (Map<String, Object>) modelDesc.get("properties");
        StringBuilder stringProperties = new StringBuilder();

        List<String> required = getRequired(modelDesc);
        if (!required.isEmpty()) {
            List<String> extendz = (List<String>) ofNullable(modelDesc.get("extends")).orElse(new ArrayList());
            String inheritanceString = getInheritanceString(extendz, parents);

            StringBuilder classAttributes = new StringBuilder();
            String classAttributesAssignation = getConstructor(modelDesc, parents);

            List<String> parentAttributes = getParentsAttributes(modelDesc, parents);
            List<String> modelAttributes = required.stream().filter(a -> !parentAttributes.contains(a)).collect(Collectors.toList());

            for (String prop: parentAttributes) {
                Map<String, Object> property = getProperty(prop, modelDesc, parents);
                if (property != null) {
                    if ("entity".equals(property.get("type"))) {
                        stringProperties.append(ofNullable(property.get("range")).orElse("String"))
                                .append(" ")
                                .append(sanitizedProp(prop));
                    } else {
                        // should have a type but CallForTenders.title has no type
                        String type = (String) ofNullable(property.get("type")).orElse("");
                        stringProperties.append(ofNullable(nativeTypeMapping.get(type)).orElse("String"))
                                .append(" ")
                                .append(sanitizedProp(prop));
                    }
                    if (!prop.equals(required.get(required.size() - 1)) || !modelAttributes.isEmpty()) {
                        stringProperties.append(", ");
                    }
                }
            }
            for (String prop: modelAttributes) {
                Map<String, Object> property = (Map<String, Object>) ofNullable(properties).map(p -> p.get(prop)).orElse(null);
                if (property != null) {
                    if ("entity".equals(property.get("type"))) {
                        stringProperties.append(ofNullable(property.get("range")).orElse("String"))
                                .append(" ")
                                .append(sanitizedProp(prop));
                        classAttributes.append("final ")
                                .append(ofNullable(property.get("range")).orElse("String"))
                                .append(" ")
                                .append(sanitizedProp(prop)).append(";");
                    } else {
                        // should have a type but CallForTenders.title has no type
                        String type = (String) ofNullable(property.get("type")).orElse("");
                        stringProperties.append(ofNullable(nativeTypeMapping.get(type)).orElse("String"))
                                .append(" ")
                                .append(sanitizedProp(prop));
                        classAttributes.append("final ")
                                .append(ofNullable(nativeTypeMapping.get(type)).orElse("String"))
                                .append(" ")
                                .append(sanitizedProp(prop)).append(";");
                    }
                } else {
                    // it seems that there are some fields that are not listed but in the required list
                    // we should do a PR to fix that but for now we are putting a String property
                    stringProperties.append(format("String %s", prop));
                }
                if (!prop.equals(modelAttributes.get(modelAttributes.size() - 1))) {
                    stringProperties.append(", ");
                }
            }

            if (parents.containsKey(modelName) || inheritanceString.contains("extends")) {
                return format("""
                        package org.icij.ftm;
                                        
                        public class %s %s{
                            %s
                            public %s (%s) {
                                %s
                            }
                        }
                        """, modelName, inheritanceString, classAttributes, modelName, stringProperties, classAttributesAssignation);
            } else {
                return format("""
                        package org.icij.ftm;
                                        
                        public record %s(%s) %s{};
                        """, modelName, stringProperties, inheritanceString);
            }
        } else {
            return format("""
                    package org.icij.ftm;
                                    
                    public interface %s {};
                    """, modelName);
        }
    }

    private static Map<String, Object> getProperty(String prop, Map<String, Object> model, Map<String, Map<String, Object>> parents) {
        Map<String, Object> property = (Map<String, Object>) ((Map<String, Object>) ofNullable(model.get("properties")).orElse(new HashMap<>())).get(prop);
        if (property == null) {
            Optional<String> parent = getParent(getExtends(model), parents);
            return parent.map(s -> getProperty(prop, parents.get(s), parents)).orElse(null);
        } else {
            return property;
        }
    }

    private static String getConstructor(Map<String, Object> model, Map<String, Map<String, Object>> parents) {
        List<String> parentAttributes = getParentsAttributes(model, parents);
        List<String> required = getRequired(model);
        if (!parentAttributes.isEmpty()) {
            return format("super(%s);\n", String.join(",", parentAttributes)) + required.stream().filter(a -> !parentAttributes.contains(a)).map(a -> format("this.%s = %s;", a, a)).collect(Collectors.joining("\n"));
        } else {
            return required.stream().map(a -> format("this.%s = %s;", a, a)).collect(Collectors.joining("\n"));
        }
    }

    private static List<String> getRequired(Map<String, Object> model) {
        return (List<String>) ofNullable(model.get("required")).orElse(new ArrayList<>());
    }

    private static List<String> getParentsAttributes(Map<String, Object> model, Map<String, Map<String, Object>> parents) {
        Optional<String> parent = getParent(getExtends(model), parents);
        if (parent.isPresent()) {
            return (List<String>) parents.getOrDefault(parent.get(), new HashMap<>()).get("required");
        } else {
            return new ArrayList<>();
        }
    }

    private static List<String> getExtends(Map<String, Object> model) {
        return (List<String>) model.getOrDefault("extends", new ArrayList<>());
    }

    private static Optional<String> getParent(List<String> extendz, Map<String, Map<String, Object>> parents) {
        return extendz.stream().filter(p -> parents.getOrDefault(p, new HashMap<>()).get("required") != null).findFirst();
    }

    private static String getInheritanceString(List<String> extendz, Map<String, Map<String, Object>> parents) {
        List<String> extendsList = extendz.stream().filter(p -> parents.getOrDefault(p, new HashMap<>()).get("required") != null).collect(Collectors.toList());
        List<String> implementsList = extendz.stream().filter(p -> parents.getOrDefault(p, new HashMap<>()).get("required") == null).collect(Collectors.toList());
        String extendsString = extendsList.isEmpty() ? "" : format("extends %s ", String.join(", ", extendsList));
        String implementsString = implementsList.isEmpty() ? "" : format("implements %s ", String.join(", ", implementsList));
        return extendz.isEmpty() ? "" : extendsString + implementsString;
    }

    static Map<String, Object> getYamlContent(File yamlFile) throws FileNotFoundException {
        return (Map<String, Object>) yaml.loadFromInputStream(new FileInputStream(yamlFile));
    }

    private String sanitizedProp(String prop) {
        return ofNullable(jvmReservedWords.get(prop)).orElse(prop);
    }
}
