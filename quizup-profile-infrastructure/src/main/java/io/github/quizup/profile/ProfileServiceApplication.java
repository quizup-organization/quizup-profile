package io.github.quizup.profile;

import io.github.quizup.profile.infrastructure.properties.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Profile Service Application
 */
@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class ProfileServiceApplication {

    static void main(String[] args) {
        SpringApplication.run(ProfileServiceApplication.class, args);
    }
}
