package com.rhlowery.acs.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public enum NodeType {
    CATALOG,
    DATABASE,
    TABLE,
    NAMESPACE,
    VIEW,
    VOLUME,
    MODEL,
    COMPUTE
}
