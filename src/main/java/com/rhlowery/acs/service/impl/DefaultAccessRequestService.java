package com.rhlowery.acs.service.impl;

import com.rhlowery.acs.domain.AccessRequest;
import com.rhlowery.acs.service.AccessRequestService;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A secondary implementation that could be used for integration tests or as a base for DB implementation.
 */

public class DefaultAccessRequestService implements AccessRequestService {
    @Override
    public List<AccessRequest> getAllRequests(String userId, List<String> groups, boolean isAdmin) {
        return Collections.emptyList();
    }

    @Override
    public void saveRequests(List<AccessRequest> requests, String userId, List<String> groups, boolean isAdmin) {
        // Do nothing or throw unsupported
    }

    @Override
    public Optional<AccessRequest> getRequestById(String id) {
        return Optional.empty();
    }

    @Override
    public void clear() {
    }
}
