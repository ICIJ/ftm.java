package org.icij.ftm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * Generate java source from FtM model yaml file.
 * @see <a href="https://github.com/alephdata/followthemoney/blob/main/followthemoney/schema">here</a>.
 */
public class SourceGenerator {
    private final static Logger logger = LoggerFactory.getLogger(SourceGenerator.class);
    private final Properties properties;

    private static final Map<String, String> nativeTypeMapping = Map.of(
            "number", "int",
            "url", "URL"
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
        Map<String, Model> parents = (Map<String, Model>)
                ofNullable(this.properties.get("parents")).orElse(new HashMap<>());
        Model model = new Model(Utils.getYamlContent(path.toFile()), parents);

        String inheritanceString = getInheritanceString(model);

        if (model.isConcrete()) {
            List<String> parentsAttributes = model.parentsAttributes();
            List<String> modelAttributes = model.attributes().stream().filter(a -> !parentsAttributes.contains(a)).toList();

            String parentsStringProperties = new AttributeHandlerForSignature(model).generateFor(parentsAttributes);
            String stringProperties = new AttributeHandlerForSignature(model).generateFor(modelAttributes);
            String classAttributes = new AttributeHandlerForAttrs(model).generateFor(modelAttributes);
            String classAttributesAssignation = getConstructor(model);

            if (parents.containsKey(model.name()) || inheritanceString.contains("extends")) {
                return format("""
                        package org.icij.ftm;
                                
                        /**
                         * Automatically generated class for FtM model. Do not update this class.
                         * @see <a href="https://github.com/alephdata/followthemoney/blob/main/followthemoney/schema/%s.yaml">%s</a>.
                         */
                        public %sclass %s %s{
                            %s
                            public %s (%s) {
                                %s
                            }
                        }
                        """, model.name(), model.name(), getAbstract(model), model.name(), inheritanceString, classAttributes, model.name(), concatenate(parentsStringProperties, stringProperties), classAttributesAssignation);
            } else {
                return format("""
                        package org.icij.ftm;
                         
                        /**
                         * Automatically generated record for FtM model. Do not update this record.
                         * @see <a href="https://github.com/alephdata/followthemoney/blob/main/followthemoney/schema/%s.yaml">%s</a>.
                         */
                        public record %s(%s) %s{};
                        """, model.name(), model.name(), model.name(), stringProperties, inheritanceString);
            }
        } else {
            return format("""
                    package org.icij.ftm;
                    
                    /**
                     * Automatically generated interface for FtM model. Do not update this interface.
                     * @see <a href="https://github.com/alephdata/followthemoney/blob/main/followthemoney/schema/%s.yaml">%s</a>.
                    */
                    public interface %s %s{};
                    """, model.name(), model.name(), model.name(), inheritanceString);
        }
    }

    private String getAbstract(Model model) {
        return model.isAbstract() ? "abstract ": "";
    }

    private static String getConstructor(Model model) {
        List<String> parentAttributes = model.parentsAttributes();
        List<String> attributes = model.attributes();
        if (!parentAttributes.isEmpty()) {
            return format("super(%s);\n", String.join(", ", parentAttributes)) + attributes.stream().filter(a -> !parentAttributes.contains(a)).map(a -> format("this.%s = %s;", a, a)).collect(Collectors.joining("\n"));
        } else {
            return attributes.stream().map(a -> format("this.%s = %s;", a, a)).collect(Collectors.joining("\n"));
        }
    }

    private static String getInheritanceString(Model model) {
        Optional<String> javaExtend = model.concreteParent();
        List<String> extendz = model.getExtends();
        List<String> implementsList = extendz.stream().filter(p -> model.parents.get(p) == null || !model.parents.get(p).isConcrete()).collect(Collectors.toList());
        String extendsString = model.isConcrete() && javaExtend.isPresent() ? format("extends %s ", javaExtend.get()): "";
        String implementsString = implementsList.isEmpty() ? "" : model.isConcrete() ?
                format("implements %s ", String.join(", ", implementsList)):
                format("extends %s ", String.join(", ", implementsList));
        return extendz.isEmpty() ? "" : extendsString + implementsString;
    }

    private static String concatenate(String parentsStringProperties, String stringProperties) {
        return parentsStringProperties.isEmpty() ? stringProperties : parentsStringProperties +
                (stringProperties.isEmpty() ? "" : ", " + stringProperties);
    }

    private static String sanitizedProp(String prop) {
        return ofNullable(jvmReservedWords.get(prop)).orElse(prop);
    }

    static class AttributeHandlerForSignature {
        private final Model model;

        public AttributeHandlerForSignature(Model model) {
            this.model = model;
        }

        String generateFor(List<String> attributes) {
            StringBuilder stringProperties = new StringBuilder();
            for (String prop: attributes) {
                Map<String, Object> property = model.property(prop);
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
        public AttributeHandlerForAttrs(Model model) {
            super(model);
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
