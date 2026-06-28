package com.evidencepilot.mapper;

import com.evidencepilot.dto.response.PaperSectionResponse;
import com.evidencepilot.dto.response.ProjectMemberResponse;
import com.evidencepilot.dto.response.ProjectResponse;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.ProjectMember;
import com.evidencepilot.model.enums.ProjectRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    ProjectResponse toProjectResponse(Project entity);

    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "role", source = "role")
    ProjectMemberResponse toProjectMemberResponse(ProjectMember entity);

    @Mapping(target = "documentId", source = "document.id")
    @Mapping(target = "assignedUserId", source = "assignedUser.id")
    PaperSectionResponse toPaperSectionResponse(PaperSection entity);

    default String mapProjectRole(ProjectRole role) {
        return role != null ? role.name() : null;
    }
}
