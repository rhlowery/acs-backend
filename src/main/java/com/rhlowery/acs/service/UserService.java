package com.rhlowery.acs.service;

import com.rhlowery.acs.domain.User;
import com.rhlowery.acs.domain.Group;
import java.util.List;
import java.util.Optional;

/**
 * Interface for user and group management.
 */
public interface UserService {
    /**
     * @return List of all users.
     */
    List<User> listUsers();

    /**
     * @return List of all groups.
     */
    List<Group> listGroups();

    /**
     * Retrieves a user by their unique identifier.
     * @param userId The unique identifier of the user.
     * @return Optional containing the user if found.
     */
    Optional<User> getUser(String userId);

     /**
     * Retrieves a group by its unique identifier.
     * @param groupId The unique identifier of the group.
     * @return Optional containing the group if found.
     */
    Optional<Group> getGroup(String groupId);

    /**
     * Updates group memberships for a user.
     * @param userId The unique identifier of the user.
     * @param groups List of new group memberships.
     * @return The updated user.
     */
    User updateUserGroups(String userId, List<String> groups);

    /**
     * Updates the system-wide persona for a user.
     * @param userId The unique identifier of the user.
     * @param persona The persona ID to assign.
     * @return The updated user.
     */
    User updateUserPersona(String userId, String persona);

    /**
     * Updates the system-wide persona for a group.
     * @param groupId The unique identifier of the group.
     * @param persona The persona ID to assign.
     * @return The updated group.
     */
    Group updateGroupPersona(String groupId, String persona);
    
    User saveUser(User user);
    Group saveGroup(Group group);
    
    void clear();
}

