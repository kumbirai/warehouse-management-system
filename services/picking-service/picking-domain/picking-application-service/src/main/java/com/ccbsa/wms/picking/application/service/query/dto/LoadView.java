package com.ccbsa.wms.picking.application.service.query.dto;

import java.time.ZonedDateTime;

import com.ccbsa.common.domain.valueobject.LoadNumber;
import com.ccbsa.wms.picking.domain.core.valueobject.LoadId;
import com.ccbsa.wms.picking.domain.core.valueobject.LoadStatus;

import lombok.Builder;
import lombok.Getter;

/**
 * View DTO: LoadView
 * <p>
 * Read model view for Load queries.
 */
@Getter
@Builder
public final class LoadView {
    private final LoadId id;
    private final LoadNumber loadNumber;
    private final LoadStatus status;
    private final ZonedDateTime createdAt;
    private final ZonedDateTime plannedAt;
    private final int orderCount;
}
