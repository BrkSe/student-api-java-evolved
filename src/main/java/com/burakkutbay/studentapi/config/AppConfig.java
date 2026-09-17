package com.burakkutbay.studentapi.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.burakkutbay.studentapi.repository.StudentRepository;

/**
 * application.properties + sistem özellikleri (-Danahtar=değer) ile yapılandırma.
 */
public class AppConfig {

    private final Properties properties = new Properties();

    public static AppConfig load() throws IOException {
        AppConfig config = new AppConfig();
        InputStream in = null;
        try {
            in = AppConfig.class.getClassLoader().getResourceAsStream("application.properties");
            if (in != null) {
                config.properties.load(in);
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // yut
                }
            }
        }
        return config;
    }

    public String get(String key, String defaultValue) {
        String systemValue = System.getProperty(key);
        if (systemValue != null) {
            return systemValue;
        }
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    public int getInt(String key, int defaultValue) {
        String value = get(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.valueOf(value.trim()).intValue();
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public StudentRepository createRepository() throws Exception {
        String className = get("repository.class",
                "com.burakkutbay.studentapi.repository.InMemoryStudentRepository");
        Class clazz = Class.forName(className);
        StudentRepository repository = (StudentRepository) clazz.newInstance();
        Properties repositoryProperties = new Properties();
        repositoryProperties.setProperty("repository.file", get("repository.file", "data/students.csv"));
        repository.init(repositoryProperties);
        return repository;
    }
}
