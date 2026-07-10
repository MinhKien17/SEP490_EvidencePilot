package com.evidencepilot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;

import lombok.Getter;
import lombok.Setter;
import com.evidencepilot.model.enums.ProjectStatus;
import com.evidencepilot.model.enums.ProjectRole;
import com.evidencepilot.model.enums.UserRole;

@Entity
@Table(name = "projects")
@Getter
@Setter
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)")
    @JdbcTypeCode(java.sql.Types.BINARY)
    private UUID id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    private ProjectStatus status;

    private String targetStandard;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "project")
    private List<ProjectMember> projectMembers;

    @OneToMany(mappedBy = "project")
    private List<Document> projectDocuments;

    @OneToMany(mappedBy = "project")
    private List<ProjectMedia> projectMedia;

    @OneToMany(mappedBy = "project")
    private List<Claim> claims;

    public User getStudent() {
        if (projectMembers == null) {
            return null;
        }
        return projectMembers.stream()
                .map(ProjectMember::getUser)
                .filter(user -> user != null && user.getRole() == UserRole.STUDENT)
                .findFirst()
                .orElse(null);
    }

    public User getInstructor() {
        if (projectMembers == null) {
            return null;
        }
        return projectMembers.stream()
                .filter(member -> member.getRole() == ProjectRole.INSTRUCTOR)
                .map(ProjectMember::getUser)
                .filter(user -> user != null && user.getRole() == UserRole.INSTRUCTOR)
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Project project = (Project) o;
        return id.equals(project.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
