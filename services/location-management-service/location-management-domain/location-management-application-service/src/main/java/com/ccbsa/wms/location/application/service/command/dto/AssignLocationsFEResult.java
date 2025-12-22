package com.ccbsa.wms.location.application.service.command.dto;

import java.util.Map;

import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

/**
 * Result DTO: AssignLocationsFEResult
 * <p>
 * Result object returned after FEFO location assignment.
 */
public final class AssignLocationsFEResult {
    private final Map<String, LocationId> assignments; // Map of StockItemId (String) to LocationId

    private AssignLocationsFEResult(Builder builder) {
        this.assignments = builder.assignments != null ? Map.copyOf(builder.assignments) : Map.of();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, LocationId> getAssignments() {
        return assignments;
    }

    public static class Builder {
        private Map<String, LocationId> assignments;

        public Builder assignments(Map<String, LocationId> assignments) {
            this.assignments = assignments != null ? new java.util.HashMap<>(assignments) : null;
            return this;
        }

        public AssignLocationsFEResult build() {
            if (assignments == null) {
                throw new IllegalArgumentException("Assignments map is required");
            }
            return new AssignLocationsFEResult(this);
        }
    }
}

