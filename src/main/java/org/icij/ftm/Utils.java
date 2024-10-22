package org.icij.ftm;

import java.util.Map;
import java.util.Properties;

public class Utils {
    public static Properties propertiesFromMap(Map<String, Object> map) {
        Properties props = new Properties();
        props.putAll(map);
        return props;
    }
}
