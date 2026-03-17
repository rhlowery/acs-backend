package com.rhlowery.acs.service.impl;

import com.rhlowery.acs.domain.User;
import com.rhlowery.acs.domain.Group;
import com.rhlowery.acs.service.UserService;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class MockUserService implements UserService {
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, Group> groups = new ConcurrentHashMap<>();

    public MockUserService() {
        // Initialize mock groups
        addGroup(new Group("admins", "Administrator Group", "Users with full administrative privileges"));
        addGroup(new Group("data-governors", "Data Governance Group", "Users responsible for data quality and access approval"));
        addGroup(new Group("finance-leads", "Finance Leads Group", "Users managing financial data access"));
        addGroup(new Group("standard-users", "Standard User Group", "Default user group"));
        addGroup(new Group("governance-team", "Mandatory Governance Group", "The final signature required for all access"));

        // Initialize mock users
        addUser(new User("alice", "Alice Smith", "alice@example.com", "STANDARD_USER", new ArrayList<>(List.of("standard-users"))));
        addUser(new User("bob", "Bob Jones", "bob@example.com", "ADMIN", new ArrayList<>(List.of("admins", "data-governors", "finance-leads", "governance-team"))));
        addUser(new User("charlie", "Charlie Brown", "charlie@example.com", "STANDARD_USER", new ArrayList<>(List.of("standard-users"))));
    }

    private void addUser(User user) {
        users.put(user.id(), user);
    }

    private void addGroup(Group group) {
        groups.put(group.id(), group);
    }

    @Override
    public List<User> listUsers() {
        return new ArrayList<>(users.values());
    }

    @Override
    public List<Group> listGroups() {
        return new ArrayList<>(groups.values());
    }

    @Override
    public Optional<User> getUser(String userId) {
        return Optional.ofNullable(users.get(userId));
    }

    @Override
    public Optional<Group> getGroup(String groupId) {
        return Optional.ofNullable(groups.get(groupId));
    }

    @Override
    public User updateUserGroups(String userId, List<String> groupsToUpdate) {
        User user = users.get(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
        User updatedUser = new User(user.id(), user.name(), user.email(), user.role(), new ArrayList<>(groupsToUpdate));
        users.put(userId, updatedUser);
        return updatedUser;
    }
}
