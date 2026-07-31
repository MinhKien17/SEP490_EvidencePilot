package com.evidencepilot.config.infrastructure;

import com.evidencepilot.model.SourceCategory;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.SourceCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SourceCategorySeeder implements CommandLineRunner {

    private static final List<Seed> DEFAULTS = List.of(
            new Seed("SE", "Software Engineering", "Software design, development, testing, and maintenance."),
            new Seed("AI_ML", "AI/ML", "Artificial intelligence and machine learning."),
            new Seed("DATA", "Data", "Data science, databases, analytics, and data engineering."),
            new Seed("SECURITY", "Security", "Cybersecurity, privacy, and secure systems."),
            new Seed("CLOUD_DEVOPS", "Cloud/DevOps", "Cloud platforms, delivery, operations, and reliability."),
            new Seed("NETWORKS_DISTRIBUTED", "Networks/Distributed Systems",
                    "Computer networks and distributed computing."),
            new Seed("IOT_EMBEDDED", "IoT/Embedded", "Internet of Things and embedded systems."),
            new Seed("HCI_UX", "HCI/UX", "Human-computer interaction and user experience."),
            new Seed("OTHER", "Other", "Fallback for Sources outside the active taxonomy.")
    );

    private final SourceCategoryRepository sourceCategoryRepository;
    private final DocumentRepository documentRepository;

    @Override
    @Transactional
    public void run(String... args) {
        for (Seed seed : DEFAULTS) {
            if (sourceCategoryRepository.existsByCode(seed.code())) continue;
            SourceCategory category = new SourceCategory();
            category.setCode(seed.code());
            category.setName(seed.name());
            category.setDescription(seed.description());
            category.setActive(true);
            category.setCreatedAt(LocalDateTime.now());
            sourceCategoryRepository.save(category);
        }
        SourceCategory other = sourceCategoryRepository.findByCode("OTHER")
                .orElseThrow();
        documentRepository.assignMissingSourceCategory(other);
    }

    private record Seed(String code, String name, String description) {}
}
