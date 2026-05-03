package com.example.synchron.rules.decision;

import com.example.synchron.schemas.InternalSystemSchema;

public record Sync(InternalSystemSchema filteredRecord) implements Decision {}
