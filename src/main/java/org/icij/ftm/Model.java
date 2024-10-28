package org.icij.ftm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class Model {
    private final static Logger logger = LoggerFactory.getLogger(Model.class);
    final Map<String, Model> parents;
    private final Map<String, Object> yaml;
    private static final Set<String> mixins = new LinkedHashSet<>(List.of("Asset", "Folder", "PlainText", "HyperText"));

    public Model(Map<String, Object> modelMap, Map<String, Model> parents) {
        this.yaml = Collections.unmodifiableMap(modelMap);
        this.parents = Collections.unmodifiableMap(parents);;
        if (yaml.size() > 1) {
            throw new IllegalStateException(format("model should contain one definition, found %s", yaml.keySet()));
        }
    }

    public Model(Map<String, Object> yamlContent) {
        this(yamlContent, new HashMap<>());
    }

    public String name() {
        return yaml.keySet().iterator().next();
    }

    public Optional<String> getConcreteParent() {
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
                    .map(Model::getConcreteParent)
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

    public LinkedHashSet<String> getParentsAttributes() {
        return getParentsAttributes(this);
    }

    public Map<String, Object> getProperty(String prop) {
        return getProperty(prop, this);
    }

    public Map<String, Object> description() {
        return (Map<String, Object>) yaml.get(name());
    }

    public List<String> getRequired() {
        return (List<String>) description().getOrDefault("required", new ArrayList<>());
    }

    public List<String> getExtends() {
        return (List<String>) description().getOrDefault("extends", new ArrayList<>());
    }

    public Map<String, Object> properties() {
        return (Map<String, Object>) description().getOrDefault("properties", new HashMap<>());
    }

    public String label() {
        return (String) description().get("label");
    }

    public boolean isConcrete() {
        return  !(mixins.contains(name()) || (getRequired().isEmpty() && getConcreteParent().isEmpty()));
    }

    private Map<String, Object> getProperty(String prop, Model model) {
        Map<String, Object> property = (Map<String, Object>) model.properties().get(prop);
        if (property == null) {
            Optional<String> parent = model.getConcreteParent();
            return parent.map(s -> getProperty(prop, parents.get(s))).orElse(null);
        } else {
            return property;
        }
    }

    private LinkedHashSet<String> getParentsAttributes(Model model) {
        Optional<String> parentName = model.getConcreteParent();
        if (parentName.isPresent()) {
            LinkedHashSet<String> grandParentsAttributes = getParentsAttributes(parents.get(parentName.get()));
            List<String> parentAttributes = parents.get(parentName.get()).getRequired();
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
