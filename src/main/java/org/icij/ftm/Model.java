package org.icij.ftm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Stream.concat;

/**
 * Java encapsulation of Map of Map YAML models. It makes easier to manipulate models and centralize code generation rules.
 * <p>
 *   parents instance is the same reference for all Model objects event if it is not static because harder to test and initialize.
 * </p>
 * <p>
 *   yaml object is the original yaml map read from FtM models. We use this object for equal/hash methods.
 * </p>
 * <p>
 *     mixins instance is here to "help" the mapping of Java classes with multiple inheritance FtM models.
 * </p>
 */
public class Model {
    private final static Logger logger = LoggerFactory.getLogger(Model.class);

    public enum Mode {REQUIRED, FEATURED, FULL;}

    final Map<String, Model> parents;
    private final Mode mode;
    private final Map<String, Object> yaml;
    private static final Set<String> mixins = new LinkedHashSet<>(List.of("Asset", "Folder", "PlainText", "HyperText"));
    public Model(Map<String, Object> yamlContent) {
        this(yamlContent, new HashMap<>());
    }
    public Model(Map<String, Object> modelMap, Map<String, Model> parents) {
        this(modelMap, parents, Mode.REQUIRED);
    }

    public Model(Map<String, Object> modelMap, Map<String, Model> parents, Mode mode) {
        if (modelMap.size() > 1) {
            throw new IllegalStateException(format("model should contain one definition, found %s", modelMap.keySet()));
        }
        this.yaml = Collections.unmodifiableMap(modelMap);
        this.parents = Collections.unmodifiableMap(parents);
        this.mode = mode;
    }

    public String name() {
        return yaml.keySet().iterator().next();
    }

    /**
     * Recursive method to find a concrete parent (java class) for the current model.
     * @return the parent string model name with isConcrete = true from the inheritance tree.
     */
    public Optional<String> concreteParent() {
        List<String> extendz = getExtends();
        List<String> concreteParents = extendz.stream().filter(p -> parents.get(p) == null || parents.get(p).isConcrete()).collect(Collectors.toList());
        if (concreteParents.size()>1) {
            logger.warn("got 2 concrete parents ({}) for {}, using the first", concreteParents, label());
            return Optional.of(concreteParents.get(0));
        } else if (concreteParents.isEmpty()) {
            // this is fragile. It works because the multiple inheritance is ending with diamonds
            logger.debug("got no concrete parent for {}, searching in grand-parents", label());
            Set<Optional<String>> concreteGrandParents = extendz.stream()
                    .map(parents::get)
                    .map(Model::concreteParent)
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

    public Optional<Model> concreteParentModel() {
        return concreteParent().map(parents::get);
    }

    /**
     * get the attributes of the model.
     * <ul>
     * <li>It gets the required attributes if Mode.REQUIRED is provided to constructor (default)</li>
     * <li>It gets the required attributes concatenated to featured attributes if in Mode.FEATURED</li>
     * <li>It gets all the attributes from properties starting with required and featured attributes if in Mode.FULL</li>
     * </ul>
     * <p>
     * All duplicates are removed.
     * </p>
     * @return the list of attributes depending on Model's mode
     */
    public List<String> attributes() {
        switch (mode) {
            case REQUIRED -> {
                return required();
            }
            case FEATURED -> {
                return concat(required().stream(), featured().stream()).distinct().toList();
            }
            default -> {
                return concat(concat(required().stream(), featured().stream()), properties().keySet().stream()).distinct().toList();
            }
        }
    }

    public List<String> parentsAttributes() {
        return new LinkedList<>(parentsAttributes(this));
    }

    public Map<String, Object> property(String prop) {
        return property(prop, this);
    }

    public String type(String prop) {
        Map<String, Object> property = property(prop);
        if ("entity".equals(property.get("type"))) {
            return (String) property.get("range");
        } else {
            return (String) property.getOrDefault("type", "string");
        }
    }

    public Map<String, Object> description() {
        return (Map<String, Object>) yaml.get(name());
    }

    public List<String> required() {
        return (List<String>) description().getOrDefault("required", new ArrayList<>());
    }

    private List<String> featured() {
        return (List<String>) description().getOrDefault("featured", new ArrayList<>());
    }

    public List<String> getExtends() {
        return (List<String>) description().getOrDefault("extends", new ArrayList<>());
    }

    public Map<String, Object> properties() {
        return (Map<String, Object>) description().getOrDefault("properties", new HashMap<>());
    }

    public boolean isAbstract() {
        return (boolean) description().getOrDefault("abstract", false);
    }

    public String label() {
        return (String) description().get("label");
    }

    public List<String> getImplementsList() {
        return getExtends().stream().filter(p -> parents.get(p) == null || !parents.get(p).isConcrete()).collect(Collectors.toList());
    }

    /**
     * Warning that isConcrete is not !isAbstract. isConcrete is more in a Java sense
     * a structure (Class, Record) that can hold values whereas isAbstract is the exact
     * value from FtM models.
     * This could change in the future: do we need to harmonize this?
     * If so how could we avoid to store for example Thing.name in all daughters of Thing?
     *
     * @return true if the model can be a Class or Record
     */
    public boolean isConcrete() {
        return  !(mixins.contains(name()) || (required().isEmpty() && concreteParent().isEmpty()));
    }

    private Map<String, Object> property(String prop, Model model) {
        Map<String, Object> property = (Map<String, Object>) model.properties().get(prop);
        if (property == null) {
            List<String> extendz = model.getExtends();
            return extendz.stream().map(s -> property(prop, parents.get(s))).filter(Objects::nonNull).findFirst().orElse(null);
        } else {
            return property;
        }
    }

    private LinkedHashSet<String> parentsAttributes(Model model) {
        Optional<String> parentName = model.concreteParent();
        if (parentName.isPresent()) {
            LinkedHashSet<String> grandParentsAttributes = parentsAttributes(parents.get(parentName.get()));
            List<String> parentAttributes = parents.get(parentName.get()).attributes();
            grandParentsAttributes.addAll(parentAttributes);
            return grandParentsAttributes;
        } else {
            return new LinkedHashSet<>();
        }
    }

    @Override
    public String toString() {
        return name();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Model model = (Model) o;
        return Objects.equals(yaml, model.yaml);
    }

    @Override
    public int hashCode() {
        return Objects.hash(yaml);
    }
}
