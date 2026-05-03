package com.example.synchron.schemas;

import lombok.Builder;

/**
 * @param externalIdC this variable is defined just for simplifying this in-memory implementation
 */
@Builder
public record ExampleExternalSystemSchema(
    String id,
    String firstName,
    String lastName,
    String email,
    String phone,
    String accountName,
    String externalIdC)
    implements ExternalSystemSchema {}
