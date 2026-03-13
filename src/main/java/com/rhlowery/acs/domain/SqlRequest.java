package com.rhlowery.acs.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.Map;

@RegisterForReflection
public record SqlRequest(String statement, Map<String, Object> parameters) {}
