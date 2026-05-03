package com.example.synchron.schemas;

import java.time.Instant;
import java.util.Objects;
import lombok.Builder;

@Builder
public record InternalSystemSchema(
    String internalId,
    String firstName,
    String lastName,
    String email,
    String phone,
    String company,
    Instant updatedAt,
    long sourceVersion,
    String schemaVersion) {
  public static final String SCHEMA_VERSION = "1.0.0";

  public InternalSystemSchema(
      String internalId,
      String firstName,
      String lastName,
      String email,
      String phone,
      String company,
      Instant updatedAt,
      long sourceVersion,
      String schemaVersion) {
    Objects.requireNonNull(internalId, "internalId");
    Objects.requireNonNull(firstName, "firstName");
    Objects.requireNonNull(lastName, "lastName");
    this.internalId = internalId;
    this.firstName = firstName;
    this.lastName = lastName;
    if (email != null) {
      this.email = email.trim().toLowerCase();
    } else {
      throw new IllegalArgumentException("email is required");
    }
    if (sourceVersion < 1) {
      throw new IllegalArgumentException("sourceVersion must be >= 1");
    }
    this.sourceVersion = sourceVersion;
    if (updatedAt == null) {
      this.updatedAt = Instant.now();
    } else {
      this.updatedAt = updatedAt;
    }
    this.schemaVersion = Objects.requireNonNullElse(schemaVersion, SCHEMA_VERSION);
    this.phone = phone;
    this.company = company;
  }
}
