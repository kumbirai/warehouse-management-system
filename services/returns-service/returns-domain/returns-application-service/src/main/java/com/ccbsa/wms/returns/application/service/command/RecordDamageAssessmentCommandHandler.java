package com.ccbsa.wms.returns.application.service.command;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.returns.application.service.command.dto.RecordDamageAssessmentCommand;
import com.ccbsa.wms.returns.application.service.command.dto.RecordDamageAssessmentResult;
import com.ccbsa.wms.returns.application.service.port.messaging.ReturnsEventPublisher;
import com.ccbsa.wms.returns.application.service.port.repository.DamageAssessmentRepository;
import com.ccbsa.wms.returns.domain.core.entity.DamageAssessment;
import com.ccbsa.wms.returns.domain.core.entity.DamagedProductItem;
import com.ccbsa.wms.returns.domain.core.valueobject.DamageAssessmentId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command Handler: RecordDamageAssessmentCommandHandler
 * <p>
 * Handles recording of damage assessments for products damaged in transit.
 * <p>
 * Responsibilities:
 * - Create DamageAssessment aggregate
 * - Persist aggregate
 * - Publish DamageRecordedEvent after transaction commit
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecordDamageAssessmentCommandHandler {
    private final DamageAssessmentRepository damageAssessmentRepository;
    private final ReturnsEventPublisher eventPublisher;

    @Transactional
    public RecordDamageAssessmentResult handle(RecordDamageAssessmentCommand command, TenantId tenantId) {
        log.info("Recording damage assessment for order: {}, damage type: {}, severity: {}", command.getOrderNumber().getValue(), command.getDamageType(),
                command.getDamageSeverity());

        // 1. Validate command
        validateCommand(command);

        // 2. Create damaged product items
        List<DamagedProductItem> damagedProductItems = createDamagedProductItems(command.getDamagedProducts());

        // 3. Create DamageAssessment aggregate
        DamageAssessmentId damageAssessmentId = DamageAssessmentId.generate();
        DamageAssessment damageAssessment =
                DamageAssessment.recordDamage(damageAssessmentId, command.getOrderNumber(), tenantId, command.getDamageType(), command.getDamageSeverity(),
                        command.getDamageSource(), damagedProductItems, command.getInsuranceClaimInfo(), command.getDamageNotes());

        // 4. Get domain events BEFORE saving
        List<DomainEvent<?>> domainEvents = new ArrayList<>(damageAssessment.getDomainEvents());
        log.debug("Collected {} domain events from damage assessment creation", domainEvents.size());

        // 5. Persist aggregate
        damageAssessmentRepository.save(damageAssessment);
        damageAssessment.clearDomainEvents();

        // 6. Publish events after transaction commit
        if (!domainEvents.isEmpty()) {
            publishEventsAfterCommit(domainEvents);
        }

        // 7. Return result
        return RecordDamageAssessmentResult.builder().damageAssessmentId(damageAssessmentId).orderNumber(command.getOrderNumber()).damageType(command.getDamageType())
                .damageSeverity(command.getDamageSeverity()).damageSource(command.getDamageSource()).status(damageAssessment.getStatus())
                .recordedAt(damageAssessment.getRecordedAt()).build();
    }

    /**
     * Validates command before execution.
     *
     * @param command Command to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateCommand(RecordDamageAssessmentCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        if (command.getOrderNumber() == null) {
            throw new IllegalArgumentException("OrderNumber is required");
        }
        if (command.getDamageType() == null) {
            throw new IllegalArgumentException("DamageType is required");
        }
        if (command.getDamageSeverity() == null) {
            throw new IllegalArgumentException("DamageSeverity is required");
        }
        if (command.getDamageSource() == null) {
            throw new IllegalArgumentException("DamageSource is required");
        }
        if (command.getDamagedProducts() == null || command.getDamagedProducts().isEmpty()) {
            throw new IllegalArgumentException("At least one damaged product is required");
        }
    }

    /**
     * Creates damaged product items from command.
     *
     * @param damagedProductCommands List of damaged product commands
     * @return List of DamagedProductItem entities
     */
    private List<DamagedProductItem> createDamagedProductItems(List<RecordDamageAssessmentCommand.DamagedProductCommand> damagedProductCommands) {
        return damagedProductCommands.stream()
                .map(command -> DamagedProductItem.create(command.getItemId(), command.getProductId(), command.getDamagedQuantity(), command.getDamageType(),
                        command.getDamageSeverity(), command.getDamageSource(), command.getPhotoUrl(), command.getNotes())).collect(Collectors.toList());
    }

    /**
     * Publishes domain events after transaction commit to avoid race conditions.
     *
     * @param domainEvents Domain events to publish
     */
    private void publishEventsAfterCommit(List<DomainEvent<?>> domainEvents) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            log.debug("No active transaction - publishing events immediately");
            eventPublisher.publish(domainEvents);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    log.info("Transaction committed - publishing {} domain events", domainEvents.size());
                    eventPublisher.publish(domainEvents);
                } catch (Exception e) {
                    log.error("Failed to publish domain events after transaction commit", e);
                }
            }
        });
    }
}
