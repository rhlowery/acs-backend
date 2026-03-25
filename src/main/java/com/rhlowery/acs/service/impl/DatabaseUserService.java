package com.rhlowery.acs.service.impl;

import com.rhlowery.acs.domain.User;
import com.rhlowery.acs.domain.Group;
import com.rhlowery.acs.infrastructure.entity.UserEntity;
import com.rhlowery.acs.infrastructure.entity.GroupEntity;
import com.rhlowery.acs.service.UserService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

@ApplicationScoped
@Transactional
public class DatabaseUserService implements UserService {
    private static final Logger LOG = Logger.getLogger(DatabaseUserService.class);

    @Override
    public List<User> listUsers() {
        return UserEntity.<UserEntity>listAll().stream()
            .map(this::mapToDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Group> listGroups() {
        return GroupEntity.<GroupEntity>listAll().stream()
            .map(this::mapToDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<User> getUser(String userId) {
        return UserEntity.<UserEntity>findByIdOptional(userId).map(this::mapToDomain);
    }

    @Override
    public Optional<Group> getGroup(String groupId) {
        return GroupEntity.<GroupEntity>findByIdOptional(groupId).map(this::mapToDomain);
    }

    @Override
    public User updateUserGroups(String userId, List<String> groupsToUpdate) {
        LOG.info("Updating groups for user: " + userId);
        UserEntity entity = UserEntity.findById(userId);
        if (entity == null) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
        entity.groups = groupsToUpdate;
        return mapToDomain(entity);
    }

    @Override
    public User updateUserPersona(String userId, String persona) {
        LOG.infof("Updating user %s persona to %s", userId, persona);
        UserEntity entity = UserEntity.findById(userId);
        if (entity == null) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
        entity.persona = persona;
        return mapToDomain(entity);
    }

    @Override
    @Transactional
    public Group updateGroupPersona(String groupId, String persona) {
        LOG.infof("Updating group %s persona to %s", groupId, persona);
        GroupEntity entity = GroupEntity.findById(groupId);
        if (entity == null) {
            throw new IllegalArgumentException("Group not found: " + groupId);
        }
        entity.persona = persona;
        GroupEntity.getEntityManager().flush();
        return mapToDomain(entity);
    }

    @Override
    public User saveUser(User user) {
        LOG.info("Saving user: " + user.id());
        UserEntity entity = UserEntity.findById(user.id());
        if (entity == null) {
            entity = new UserEntity(user.id(), user.name(), user.email(), user.role(), user.groups(), user.persona());
            entity.persist();
        } else {
            entity.name = user.name();
            entity.email = user.email();
            entity.role = user.role();
            entity.groups = user.groups();
            entity.persona = user.persona();
        }
        return mapToDomain(entity);
    }

    @Override
    public Group saveGroup(Group group) {
        LOG.info("Saving group: " + group.id());
        GroupEntity entity = GroupEntity.findById(group.id());
        if (entity == null) {
            entity = new GroupEntity(group.id(), group.name(), group.description(), group.persona());
            entity.persist();
        } else {
            entity.name = group.name();
            entity.description = group.description();
            entity.persona = group.persona();
        }
        return mapToDomain(entity);
    }

    @Override
    @Transactional
    public void clear() {
        UserEntity.deleteAll();
        GroupEntity.deleteAll();
        UserEntity.getEntityManager().flush();
    }

    private User mapToDomain(UserEntity entity) {
        return new User(entity.id, entity.name, entity.email, entity.role, 
            entity.groups != null ? new java.util.ArrayList<>(entity.groups) : java.util.List.of(), 
            entity.persona);
    }

    private Group mapToDomain(GroupEntity entity) {
        return new Group(entity.id, entity.name, entity.description, entity.persona);
    }
}
