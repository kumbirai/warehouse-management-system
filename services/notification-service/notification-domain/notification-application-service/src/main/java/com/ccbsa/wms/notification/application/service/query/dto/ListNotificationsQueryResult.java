package com.ccbsa.wms.notification.application.service.query.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result DTO: ListNotificationsQueryResult
 * <p>
 * Result of listing notifications.
 */
public final class ListNotificationsQueryResult {
    private final List<GetNotificationQueryResult> items;
    private final long totalCount;

    private ListNotificationsQueryResult(Builder builder) {
        this.items = builder.items;
        this.totalCount = builder.totalCount;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<GetNotificationQueryResult> getItems() {
        // Return unmodifiable list to prevent external modification
        return items != null ? Collections.unmodifiableList(items) : Collections.emptyList();
    }

    public long getTotalCount() {
        return totalCount;
    }

    public static class Builder {
        private List<GetNotificationQueryResult> items;
        private long totalCount;

        public Builder items(List<GetNotificationQueryResult> items) {
            // Defensive copy to prevent external modification
            this.items = items != null ? new ArrayList<>(items) : null;
            return this;
        }

        public Builder totalCount(long totalCount) {
            this.totalCount = totalCount;
            return this;
        }

        public ListNotificationsQueryResult build() {
            return new ListNotificationsQueryResult(this);
        }
    }
}

