package com.burakkutbay.studentapi.config;

import java.io.IOException;
import java.time.Duration;
import java.util.Properties;

import com.burakkutbay.studentapi.repository.StudentRepository;

/// `application.properties` + sistem özellikleri (`-Danahtar=değer`) ile yapılandırma.
public final class AppConfig {

    private final Properties properties = new Properties();

    public static AppConfig load() throws IOException {
        var config = new AppConfig();
        try (var in = AppConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                config.properties.load(in);
            }
        }
        return config;
    }

    public String get(String key, String defaultValue) {
        return System.getProperty(key, properties.getProperty(key, defaultValue));
    }

    public int getInt(String key, int defaultValue) {
        var value = System.getProperty(key, properties.getProperty(key));
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.strip());
        } catch (NumberFormatException _) {
            return defaultValue;
        }
    }

    public Duration getDuration(String key, Duration defaultValue) {
        return Duration.ofMillis(getInt(key, (int) defaultValue.toMillis()));
    }

    public StudentRepository createRepository() throws ReflectiveOperationException, IOException {
        var className = get("repository.class", "com.burakkutbay.studentapi.repository.InMemoryStudentRepository");
        var repository = Class.forName(className)
                .asSubclass(StudentRepository.class)
                .getDeclaredConstructor()
                .newInstance();
        var repositoryProperties = new Properties();
        repositoryProperties.setProperty("repository.file", get("repository.file", "data/students.csv"));
        repository.init(repositoryProperties);
        return repository;
    }
}
