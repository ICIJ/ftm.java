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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;

/**
 * Generate java source from FtM model yaml file.
 * @see <a href="https://github.com/alephdata/followthemoney/blob/main/followthemoney/schema">here</a>.
 */
public class SourceGenerator {
    private final static Logger logger = LoggerFactory.getLogger(SourceGenerator.class);
    private final Properties properties;
    private static final Load yaml = new Load(LoadSettings.builder().build());
    ;
    private static final Map<String, String> nativeTypeMapping = Map.of(
            "number", "int",
            "url", "Url"
    );
    private static final Map<String, String> jvmReservedWords = Map.of(
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

        List<String> required = getRequired(modelDesc);
        String inheritanceString = getInheritanceString(modelDesc, parents);

        if (!required.isEmpty() || inheritanceString.contains("extends")) {
            List<String> parentsAttributes = new ArrayList<>(getParentsAttributes(modelDesc, parents));
            List<String> modelAttributes = required.stream().filter(a -> !parentsAttributes.contains(a)).toList();

            String parentsStringProperties = new AttributeHandlerForSignature(modelDesc, parents).generateFor(parentsAttributes);
            String stringProperties = new AttributeHandlerForSignature(modelDesc, parents).generateFor(modelAttributes);
            String classAttributes = new AttributeHandlerForAttrs(modelDesc, parents).generateFor(modelAttributes);
            String classAttributesAssignation = getConstructor(modelDesc, parents);

            if (parents.containsKey(modelName) || inheritanceString.contains("extends")) {
                return format("""
                        package org.icij.ftm;
                                
                        /**
                         * Automatically generated class for FtM model. Do not update this class.
                         * @see <a href="https://github.com/alephdata/followthemoney/blob/main/followthemoney/schema/%s.yaml">%s</a>.
                         */
                        public class %s %s{
                            %s
                            public %s (%s) {
                                %s
                            }
                        }
                        """, modelName, modelName, modelName, inheritanceString, classAttributes, modelName, concatenate(parentsStringProperties, stringProperties), classAttributesAssignation);
            } else {
                return format("""
                        package org.icij.ftm;
                         
                        /**
                         * Automatically generated record for FtM model. Do not update this class.
                         * @see <a href="https://github.com/alephdata/followthemoney/blob/main/followthemoney/schema/%s.yaml">%s</a>.
                         */
                        public record %s(%s) %s{};
                        """, modelName, modelName, modelName, stringProperties, inheritanceString);
            }
        } else {
            return format("""
                    package org.icij.ftm;
                    
                    /**
                     * Automatically generated class for FtM model. Do not update this class.
                     * @see <a href="https://github.com/alephdata/followthemoney/blob/main/followthemoney/schema/%s.yaml">%s</a>.
                    */
                    public interface %s {};
                    """, modelName, modelName, modelName);
        }
    }

    private static Map<String, Object> getProperty(String prop, Map<String, Object> model, Map<String, Map<String, Object>> parents) {
        Map<String, Object> property = (Map<String, Object>) ((Map<String, Object>) ofNullable(model.get("properties")).orElse(new HashMap<>())).get(prop);
        if (property == null) {
            Optional<String> parent = getParent(model, parents);
            return parent.map(s -> getProperty(prop, parents.get(s), parents)).orElse(null);
        } else {
            return property;
        }
    }

    private static String getConstructor(Map<String, Object> model, Map<String, Map<String, Object>> parents) {
        List<String> parentAttributes = new ArrayList<>(getParentsAttributes(model, parents));
        List<String> required = getRequired(model);
        if (!parentAttributes.isEmpty()) {
            return format("super(%s);\n", String.join(", ", parentAttributes)) + required.stream().filter(a -> !parentAttributes.contains(a)).map(a -> format("this.%s = %s;", a, a)).collect(Collectors.joining("\n"));
        } else {
            return required.stream().map(a -> format("this.%s = %s;", a, a)).collect(Collectors.joining("\n"));
        }
    }

    private static LinkedHashSet<String> getParentsAttributes(Map<String, Object> model, Map<String, Map<String, Object>> parents) {
        Optional<String> parent = getParent(model, parents);
        if (parent.isPresent()) {
            LinkedHashSet<String> grandParentsAttributes = getParentsAttributes(parents.get(parent.get()), parents);
            List<String> parentAttributes = getRequired(parents.getOrDefault(parent.get(), new HashMap<>()));
            grandParentsAttributes.addAll(parentAttributes);
            return grandParentsAttributes;
        } else {
            return new LinkedHashSet<>();
        }
    }

    private static Optional<String> getParent(Map<String, Object> model, Map<String, Map<String, Object>> parents) {
        List<String> extendz = getExtends(model);
        List<String> concreteParents = extendz.stream().filter(p -> isConcrete(parents.getOrDefault(p, new HashMap<>()))).collect(Collectors.toList());
        if (concreteParents.size()>1) {
            logger.warn("got 2 concrete parents ({}) for {}, using the first", concreteParents, model.get("label"));
            return Optional.of(concreteParents.get(0));
        } else if (concreteParents.isEmpty()) {
            // this is fragile. It works because the multiple inheritance is ending with diamonds
            logger.debug("got no concrete parent for {}, searching in grand-parents", model.get("label"));
            Set<Optional<String>> concreteGrandParents = extendz.stream()
                    .map(parents::get)
                    .map(m -> getParent(m, parents))
                    .filter(Optional::isPresent).collect(Collectors.toSet());
            if (!concreteGrandParents.isEmpty()) {
                if (concreteGrandParents.size() > 1) {
                    logger.warn("got {} concrete grand-parents, returning first", concreteGrandParents);
                }
                return concreteGrandParents.iterator().next();
            }
            return Optional.empty();
        } else {
            return Optional.of(concreteParents.get(0));
        }
    }

    private static String getInheritanceString(Map<String, Object> model, Map<String, Map<String, Object>> parents) {
        Optional<String> javaExtend = getParent(model, parents);
        List<String> extendz = getExtends(model);
        List<String> implementsList = extendz.stream().filter(p -> !isConcrete(parents.getOrDefault(p, new HashMap<>()))).collect(Collectors.toList());
        String extendsString = javaExtend.isPresent() ? format("extends %s ", javaExtend.get()): "";
        String implementsString = implementsList.isEmpty() ? "" : format("implements %s ", String.join(", ", implementsList));
        return extendz.isEmpty() ? "" : extendsString + implementsString;
    }

    private static boolean isConcrete(Map<String, Object> model) {
        return !getRequired(model).isEmpty();
    }

    private static String concatenate(String parentsStringProperties, String stringProperties) {
        return parentsStringProperties.isEmpty() ? stringProperties : parentsStringProperties +
                (stringProperties.isEmpty() ? "" : ", " + stringProperties);
    }

    private static List<String> getRequired(Map<String, Object> model) {
        return (List<String>) model.getOrDefault("required", new ArrayList<>());
    }

    private static List<String> getExtends(Map<String, Object> model) {
        return (List<String>) model.getOrDefault("extends", new ArrayList<>());
    }

    static Map<String, Object> getYamlContent(File yamlFile) throws FileNotFoundException {
        return (Map<String, Object>) yaml.loadFromInputStream(new FileInputStream(yamlFile));
    }

    private static String sanitizedProp(String prop) {
        return ofNullable(jvmReservedWords.get(prop)).orElse(prop);
    }

    static class AttributeHandlerForSignature {
        private final Map<String, Object> modelDesc;
        private final Map<String, Map<String, Object>> parents;

        public AttributeHandlerForSignature(Map<String, Object> modelDesc, Map<String, Map<String, Object>> parents) {
            this.modelDesc = modelDesc;
            this.parents = parents;
        }

        String generateFor(List<String> attributes) {
            StringBuilder stringProperties = new StringBuilder();
            for (String prop: attributes) {
                Map<String, Object> property = getProperty(prop, modelDesc, parents);
                if (property != null) {
                    if ("entity".equals(property.get("type"))) {
                        addPropertyForEntity(stringProperties, prop, property);
                    } else {
                        addPropertyForNativeType(stringProperties, prop, property);
                    }
                    if (!prop.equals(attributes.get(attributes.size() - 1))) {
                        addSeparator(stringProperties);
                    }
                }
            }
            return stringProperties.toString();
        }

        protected void addSeparator(StringBuilder stringProperties) {
            stringProperties.append(", ");
        }

        protected void addPropertyForEntity(StringBuilder stringProperties, String prop, Map<String, Object> property) {
            stringProperties.append(ofNullable(property.get("range")).orElse("String"))
                    .append(" ")
                    .append(sanitizedProp(prop));
        }

        protected void addPropertyForNativeType(StringBuilder stringProperties, String prop, Map<String, Object> property) {
            // should have a type but CallForTenders.title has no type
            String type = (String) ofNullable(property.get("type")).orElse("");
            stringProperties.append(ofNullable(nativeTypeMapping.get(type)).orElse("String"))
                    .append(" ")
                    .append(sanitizedProp(prop));
        }
    }

    static class AttributeHandlerForAttrs extends AttributeHandlerForSignature {
        public AttributeHandlerForAttrs(Map<String, Object> modelDesc, Map<String, Map<String, Object>> parents) {
            super(modelDesc, parents);
        }

        @Override
        protected void addPropertyForEntity(StringBuilder stringProperties, String prop, Map<String, Object> property) {
            stringProperties.append("final ")
                    .append(ofNullable(property.get("range")).orElse("String"))
                    .append(" ")
                    .append(sanitizedProp(prop)).append(";");
        }

        @Override
        protected void addPropertyForNativeType(StringBuilder stringProperties, String prop, Map<String, Object> property) {
            String type = (String) ofNullable(property.get("type")).orElse("");
            stringProperties.append("final ")
                    .append(ofNullable(nativeTypeMapping.get(type)).orElse("String"))
                    .append(" ")
                    .append(sanitizedProp(prop)).append(";");
        }

        protected void addSeparator(StringBuilder stringProperties) {
            stringProperties.append("\n");
        }
    }
}
