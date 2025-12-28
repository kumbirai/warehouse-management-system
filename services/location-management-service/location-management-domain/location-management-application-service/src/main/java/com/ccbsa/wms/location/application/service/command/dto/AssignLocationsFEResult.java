package com.ccbsa.wms.location.application.service.command.dto;

import java.util.Collections;
import java.util.Map;

import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;

/**
 * Result DTO: AssignLocationsFEResult
 * <p>
 * Result object returned after FEFO location assignment.
 */
@Getter
@Builder
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Lombok builder stores map directly. Defensive copy made in constructor and getter returns immutable view.")
public final class AssignLocationsFEResult {
    private final Map<String, LocationId> assignments; // Map of StockItemId (String) to LocationId

    public AssignLocationsFEResult(Map<String, LocationId> assignments) {
        if (assignments == null) {
            throw new IllegalArgumentException("Assignments map is required");
        }
        // Defensive copy to prevent external modification
        this.assignments = Map.copyOf(assignments);
    }

    /**
     * Returns an unmodifiable view of the assignments map.
     *
     * @return Unmodifiable map of assignments
     */
    public Map<String, LocationId> getAssignments() {
        return Collections.unmodifiableMap(assignments);
    }
}

