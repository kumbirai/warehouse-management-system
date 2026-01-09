package com.ccbsa.wms.stock.application.service.command.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;

/**
 * Command DTO: CheckExpirationDatesCommand
 * <p>
 * Command object for checking expiration dates of all stock items.
 */
@Getter
@Builder
@SuppressFBWarnings(value = "ISC_INSTANTIATE_STATIC_CLASS", justification = "Lombok @Builder generates builder class that requires instantiation")
public final class CheckExpirationDatesCommand {
    // Empty command - processes all stock items
}
