package com.ccbsa.wms.picking.application.service.port.repository;

import java.util.List;
import java.util.Optional;

import com.ccbsa.wms.picking.domain.core.entity.PickingTask;
import com.ccbsa.wms.picking.domain.core.valueobject.LoadId;
import com.ccbsa.wms.picking.domain.core.valueobject.OrderId;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingTaskId;
import com.ccbsa.wms.picking.domain.core.valueobject.PickingTaskStatus;

/**
 * Repository Port: PickingTaskRepository
 * <p>
 * Defines the contract for PickingTask entity persistence. Implemented by data access adapters.
 */
public interface PickingTaskRepository {
    /**
     * Saves a PickingTask entity.
     *
     * @param pickingTask PickingTask entity to save
     */
    void save(PickingTask pickingTask);

    /**
     * Saves multiple PickingTask entities.
     *
     * @param pickingTasks List of PickingTask entities to save
     */
    void saveAll(List<PickingTask> pickingTasks);

    /**
     * Finds a PickingTask by ID.
     *
     * @param id Picking task identifier
     * @return Optional PickingTask if found
     */
    Optional<PickingTask> findById(PickingTaskId id);

    /**
     * Finds all picking tasks for a load.
     *
     * @param loadId Load identifier
     * @return List of PickingTask entities
     */
    List<PickingTask> findByLoadId(LoadId loadId);

    /**
     * Finds all picking tasks for an order.
     *
     * @param orderId Order identifier
     * @return List of PickingTask entities
     */
    List<PickingTask> findByOrderId(OrderId orderId);

    /**
     * Finds all picking tasks with optional status filter and pagination.
     *
     * @param status Optional status filter
     * @param page   Page number (0-based)
     * @param size   Page size
     * @return List of PickingTask entities
     */
    List<PickingTask> findAll(PickingTaskStatus status, int page, int size);

    /**
     * Counts all picking tasks with optional status filter.
     *
     * @param status Optional status filter
     * @return Total count
     */
    long countAll(PickingTaskStatus status);
}
