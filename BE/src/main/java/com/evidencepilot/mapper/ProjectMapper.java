package com.evidencepilot.mapper;

import com.evidencepilot.dto.response.PaperSectionResponse;
import com.evidencepilot.dto.response.ProjectMemberResponse;
import com.evidencepilot.dto.response.ProjectResponse;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.ProjectMember;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.ProjectRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    @Mapping(target = "currentUserRole", ignore = true)
    ProjectResponse toProjectResponse(Project entity);

    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "role", source = "role")
    @Mapping(target = "firstName", source = "user.firstName")
    @Mapping(target = "lastName", source = "user.lastName")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "userRole", source = "user.role")
    ProjectMemberResponse toProjectMemberResponse(ProjectMember entity);

    @Mapping(target = "documentId", source = "document.id")
    @Mapping(target = "assignedUserId", source = "assignedUser.id")
    @Mapping(target = "assignedUserName", source = "assignedUser")
    PaperSectionResponse toPaperSectionResponse(PaperSection entity);

    default String fullName(User user) {
        if (user == null) {
            return null;
        }
        String first = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String last = user.getLastName() == null ? "" : user.getLastName().trim();
        String name = (first + " " + last).trim();
        return name.isEmpty() ? null : name;
    }

    default String mapProjectRole(ProjectRole role) {
        return role != null ? role.name() : null;
    }
}
