package com.cinemaai.identity.mapper;

import com.cinemaai.identity.dto.response.audit.AuditLogResponse;
import com.cinemaai.identity.entity.AuditLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    @Mapping(target = "actorUserId", source = "actor.id")
    @Mapping(target = "actorEmail", source = "actor.email")
    @Mapping(target = "action", expression = "java(log.getAction() != null ? log.getAction().name() : null)")
    AuditLogResponse toResponse(AuditLog log);
}
