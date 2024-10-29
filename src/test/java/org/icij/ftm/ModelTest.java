package org.icij.ftm;


import org.junit.Test;

import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class ModelTest  {

    @Test
    public void test_is_abstract() throws Exception {
        assertThat(new Model(loadYaml("Analyzable.yaml")).isAbstract()).isTrue();
        assertThat(new Model(loadYaml("Document.yaml")).isAbstract()).isFalse();
    }

    private static Map<String, Object> loadYaml(String name) throws FileNotFoundException {
        return Utils.getYamlContent(Paths.get(ClassLoader.getSystemResource(name).getPath()).toFile());
    }
}