package org.icij.ftm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;

public class Model {
    private final Map<String, Object> yaml;
    private final Map<String, Model> parents;
    private static final Set<String> mixins = new LinkedHashSet<>(List.of("Asset"));

    public Model(Map<String, Object> modelMap, Map<String, Model> parents) {
        this.yaml = Collections.unmodifiableMap(modelMap);
        this.parents = parents;
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
        return !getRequired().isEmpty();
    }
}
