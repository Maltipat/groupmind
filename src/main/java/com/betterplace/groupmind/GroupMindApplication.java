package com.betterplace.groupmind;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.betterplace.groupmind.config.GroupMindProperties;

@SpringBootApplication
@EnableConfigurationProperties(GroupMindProperties.class)
public class GroupMindApplication {
    public static void main(String[] args) {
        SpringApplication.run(GroupMindApplication.class, args);
    }
}
