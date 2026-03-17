package com.rhlowery.acs.service;

import com.rhlowery.acs.domain.AccessRequest;
import java.util.List;
import java.util.Optional;

public interface AccessRequestService {
    List<AccessRequest> getAllRequests(String userId, List<String> groups, boolean isAdmin);
    void saveRequests(List<AccessRequest> requests, String userId, List<String> groups, boolean isAdmin);
    Optional<AccessRequest> getRequestById(String id);
    void clear();
}
