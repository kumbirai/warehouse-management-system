package com.ccbsa.common.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Base class for all aggregate roots in the system. An aggregate root is the entry point to an aggregate and maintains consistency boundaries.
 *
 * @param <ID> The type of the aggregate identifier
 */
public abstract class AggregateRoot<ID> {
    private final List<DomainEvent<?>> domainEvents;
    private ID id;
    private int version;

    protected AggregateRoot() {
        this.domainEvents = new ArrayList<>();
        this.version = 0;
    }

    public ID getId() {
        return id;
    }

    /**
     * Initializes the aggregate identifier exactly once.
     *
     * @param id aggregate identifier
     */
    protected void setId(ID id) {
        ID sanitizedId = Objects.requireNonNull(id, "Aggregate identifier cannot be null");
        if (this.id != null && !this.id.equals(sanitizedId)) {
            throw new IllegalStateException("Aggregate identifier has already been initialized");
        }
        this.id = sanitizedId;
    }

    public int getVersion() {
        return version;
    }

    protected void setVersion(int version) {
        this.version = version;
    }

    /**
     * Adds a domain event to the aggregate's event list. Events are published after the aggregate is persisted.
     *
     * @param event The domain event to add
     */
    protected void addDomainEvent(DomainEvent<?> event) {
        if (event == null) {
            throw new IllegalArgumentException("Domain event cannot be null");
        }
        this.domainEvents.add(event);
    }

    /**
     * Returns an unmodifiable list of domain events.
     *
     * @return List of domain events
     */
    public List<DomainEvent<?>> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    /**
     * Clears all domain events after they have been published.
     */
    public void clearDomainEvents() {
        this.domainEvents.clear();
    }

    /**
     * Increments the version number for optimistic locking.
     */
    protected void incrementVersion() {
        this.version++;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AggregateRoot<?> that = (AggregateRoot<?>) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}

