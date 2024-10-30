package org.icij.ftm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
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
    private static final Map<String, String> imports = Map.of(
            "URL",  "java.net.URL"
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
        Map<String, Model> parents = (Map<String, Model>) ofNullable(this.properties.get("parents")).orElse(new HashMap<>());
        Model.Mode attributeMode = Model.Mode.valueOf((String) this.properties.getOrDefault("attributeMode", "REQUIRED"));
        Model model = new Model(Utils.getYamlContent(path.toFile()), parents, attributeMode);

        String inheritanceString = getInheritanceString(model);

        if (model.isConcrete()) {
            List<String> parentsAttributes = model.parentsAttributes();
            List<String> modelAttributes = model.attributes().stream().filter(a -> !parentsAttributes.contains(a)).toList();

            String parentsStringProperties = new AttributeHandlerForSignature(model, this::javaType).generateFor(parentsAttributes);
            String stringProperties = new AttributeHandlerForSignature(model, this::javaType).generateFor(modelAttributes);
            String classAttributes = new AttributeHandlerForAttrs(model, this::javaType).generateFor(modelAttributes);
            String classAttributesAssignation = getConstructor(model);
            String imports = getImports(concatenate(parentsStringProperties, stringProperties));

            if (parents.containsKey(model.name()) || inheritanceString.contains("extends")) {
                return format("""
                        package org.icij.ftm;
                        
                        %s
                                
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
                        """, imports, model.name(), model.name(), getAbstract(model), model.name(), inheritanceString, classAttributes, model.name(), concatenate(parentsStringProperties, stringProperties), classAttributesAssignation);
            } else {
                return format("""
                        package org.icij.ftm;
                         
                         %s
                         
                        /**
                         * Automatically generated record for FtM model. Do not update this record.
                         * @see <a href="https://github.com/alephdata/followthemoney/blob/main/followthemoney/schema/%s.yaml">%s</a>.
                         */
                        public record %s(%s) %s{};
                        """, imports, model.name(), model.name(), model.name(), stringProperties, inheritanceString);
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

    private String getImports(String codeString) {
        return nativeTypeMapping.values().stream()
                .filter(codeString::contains)
                .filter(t -> ofNullable(imports.get(t)).isPresent())
                .map(t -> format("import %s;", imports.get(t)))
                .collect(Collectors.joining("\n"));
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

    public String generateMethods(Model model) {
        return model.attributes().stream().map(a -> format("%s %s();", javaType(model.type(a)), a)).collect(Collectors.joining("\n"));
    }

    String javaType(String ftmType) {
        return nativeTypeMapping.getOrDefault(ftmType,
                ((List<String>)properties.getOrDefault("models", new LinkedList<>())).contains(ftmType)? ftmType: "String");
    }

    static class AttributeHandlerForSignature {
        private final Model model;
        private final Function<String, String> typeMapping;

        public AttributeHandlerForSignature(Model model, Function<String, String> typeMapping) {
            this.model = model;
            this.typeMapping = typeMapping;
        }

        String generateFor(List<String> attributes) {
            StringBuilder stringProperties = new StringBuilder();
            for (String prop: attributes) {
                String type = typeMapping.apply(model.type(prop));
                if (type != null) {
                    addProperty(stringProperties, prop, type);
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

        protected void addProperty(StringBuilder stringProperties, String prop, String type) {
            stringProperties.append(type).append(" ").append(sanitizedProp(prop));
        }
    }

    static class AttributeHandlerForAttrs extends AttributeHandlerForSignature {
        public AttributeHandlerForAttrs(Model model, Function<String, String> typeMapping) {
            super(model, typeMapping);
        }

        @Override
        protected void addProperty(StringBuilder stringProperties, String prop, String type) {
            stringProperties.append("final ").append(type).append(" ").append(sanitizedProp(prop)).append(";");
        }

        protected void addSeparator(StringBuilder stringProperties) {
            stringProperties.append("\n");
        }
    }
}
