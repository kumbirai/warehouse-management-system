package com.ccbsa.wms.gateway.api;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.ccbsa.common.application.api.ApiResponse;
import com.ccbsa.wms.gateway.api.dto.AuthenticationResult;
import com.ccbsa.wms.gateway.api.dto.CreateProductResponse;
import com.ccbsa.wms.gateway.api.dto.HandlePartialOrderAcceptanceRequest;
import com.ccbsa.wms.gateway.api.dto.HandlePartialOrderAcceptanceResponse;
import com.ccbsa.wms.gateway.api.dto.ProcessFullOrderReturnRequest;
import com.ccbsa.wms.gateway.api.dto.ProcessFullOrderReturnResponse;
import com.ccbsa.wms.gateway.api.dto.OrderQueryResult;
import com.ccbsa.wms.gateway.api.dto.ReturnResponse;
import com.ccbsa.wms.gateway.api.fixture.ProductTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.ReturnsTestDataBuilder;
import com.ccbsa.wms.gateway.api.fixture.TestDataManager;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReturnsServiceTest extends BaseIntegrationTest {

    private static AuthenticationResult tenantAdminAuth;
    private static String testTenantId;
    private static String testProductId;
    private static String testProductCode;
    private static String testOrderNumber;
    private static String testLocationId;

    @BeforeAll
    public static void setupTestData() {
        // Login as TENANT_ADMIN
        // Note: This will be set up in first test
    }

    @BeforeEach
    public void setUpReturnsTest() {
        if (tenantAdminAuth == null) {
            tenantAdminAuth = loginAsTenantAdmin();
            testTenantId = tenantAdminAuth.getTenantId();

            // Try to reuse product from repository, otherwise create new
            CreateProductResponse product = TestDataManager.getOrCreateProduct(
                    tenantAdminAuth.getAccessToken(), testTenantId,
                    ProductTestDataBuilder::buildCreateProductRequest,
                    req -> {
                        EntityExchangeResult<ApiResponse<CreateProductResponse>> productExchangeResult =
                                authenticatedPost("/api/v1/products", tenantAdminAuth.getAccessToken(), testTenantId, req).exchange().expectStatus().isCreated()
                                        .expectBody(new ParameterizedTypeReference<ApiResponse<CreateProductResponse>>() {
                                        }).returnResult();

                        ApiResponse<CreateProductResponse> productApiResponse = productExchangeResult.getResponseBody();
                        assertThat(productApiResponse).isNotNull();
                        assertThat(productApiResponse.isSuccess()).isTrue();
                        CreateProductResponse p = productApiResponse.getData();
                        assertThat(p).isNotNull();
                        return p;
                    });
            testProductId = product.getProductId();
            testProductCode = product.getProductCode();

            // Try to reuse location from repository, otherwise create new
            com.ccbsa.wms.gateway.api.dto.CreateLocationResponse location = createLocation(tenantAdminAuth.getAccessToken(), testTenantId);
            assertThat(location).isNotNull();
            testLocationId = location.getLocationId();

            // Try to find an existing completed order, otherwise create a new one
            testOrderNumber = findOrCreateCompletedOrder(testProductCode, testTenantId, tenantAdminAuth.getAccessToken());
        }
    }

    /**
     * Find and reuse a completed order from an existing completed picking list.
     * Queries the API for completed picking lists and checks their orders.
     *
     * @param productCode the product code to match (optional, can be null to match any product)
     * @param tenantId    the tenant ID
     * @param accessToken the access token
     * @param preferredOrderNumber optional preferred order number to look for first
     * @return the order number of a completed order if found, null otherwise
     */
    private String findAndReuseCompletedOrderFromPickingList(String productCode, String tenantId, String accessToken, String preferredOrderNumber) {
        try {
            // First, check if we have a preferred order number and it exists in H2
            if (preferredOrderNumber != null && !preferredOrderNumber.isEmpty()) {
                Optional<OrderQueryResult> existingOrder = TestDataManager.getRepository().findOrderByNumber(preferredOrderNumber, tenantId);
                if (existingOrder.isPresent()) {
                    try {
                        EntityExchangeResult<ApiResponse<OrderQueryResult>> verifyResult =
                                authenticatedGet("/api/v1/picking/orders/" + preferredOrderNumber, accessToken, tenantId).exchange().expectStatus().isOk()
                                        .expectBody(new ParameterizedTypeReference<ApiResponse<OrderQueryResult>>() {
                                        }).returnResult();

                        ApiResponse<OrderQueryResult> verifyApiResponse = verifyResult.getResponseBody();
                        if (verifyApiResponse != null && verifyApiResponse.isSuccess() && verifyApiResponse.getData() != null) {
                            OrderQueryResult order = verifyApiResponse.getData();
                            if (isOrderCompleted(order)) {
                                log.debug("Found completed preferred order {} in H2 repository", preferredOrderNumber);
                                TestDataManager.saveOrder(order, tenantId);
                                return preferredOrderNumber;
                            }
                        }
                    } catch (Exception e) {
                        log.debug("Error verifying preferred order {}: {}", preferredOrderNumber, e.getMessage());
                    }
                }
            }

            // Query API for completed picking lists
            EntityExchangeResult<ApiResponse<com.ccbsa.wms.gateway.api.dto.ListPickingListsQueryResult>> listsResult =
                    authenticatedGet("/api/v1/picking/picking-lists?status=COMPLETED&page=0&size=50", accessToken, tenantId).exchange().expectStatus().isOk()
                            .expectBody(new ParameterizedTypeReference<ApiResponse<com.ccbsa.wms.gateway.api.dto.ListPickingListsQueryResult>>() {
                            }).returnResult();

            ApiResponse<com.ccbsa.wms.gateway.api.dto.ListPickingListsQueryResult> listsApiResponse = listsResult.getResponseBody();
            if (listsApiResponse != null && listsApiResponse.isSuccess() && listsApiResponse.getData() != null) {
                com.ccbsa.wms.gateway.api.dto.ListPickingListsQueryResult listsData = listsApiResponse.getData();
                if (listsData.getPickingLists() != null && !listsData.getPickingLists().isEmpty()) {
                    // Iterate through completed picking lists
                    for (com.ccbsa.wms.gateway.api.dto.ListPickingListsQueryResult.PickingListView pickingListView : listsData.getPickingLists()) {
                        try {
                            // Get full picking list details
                            EntityExchangeResult<ApiResponse<com.ccbsa.wms.gateway.api.dto.PickingListQueryResult>> listResult =
                                    authenticatedGet("/api/v1/picking/picking-lists/" + pickingListView.getId(), accessToken, tenantId).exchange().expectStatus().isOk()
                                            .expectBody(new ParameterizedTypeReference<ApiResponse<com.ccbsa.wms.gateway.api.dto.PickingListQueryResult>>() {
                                            }).returnResult();

                            ApiResponse<com.ccbsa.wms.gateway.api.dto.PickingListQueryResult> listApiResponse = listResult.getResponseBody();
                            if (listApiResponse != null && listApiResponse.isSuccess() && listApiResponse.getData() != null) {
                                com.ccbsa.wms.gateway.api.dto.PickingListQueryResult pickingList = listApiResponse.getData();
                                
                                // Check loads and orders in this picking list
                                if (pickingList.getLoads() != null) {
                                    for (com.ccbsa.wms.gateway.api.dto.PickingListQueryResult.LoadQueryResult load : pickingList.getLoads()) {
                                        if (load.getOrders() != null) {
                                            for (com.ccbsa.wms.gateway.api.dto.PickingListQueryResult.OrderQueryResult orderInList : load.getOrders()) {
                                                if (orderInList.getOrderNumber() != null) {
                                                    // Query the order to get full details including picked quantities
                                                    try {
                                                        EntityExchangeResult<ApiResponse<OrderQueryResult>> orderResult =
                                                                authenticatedGet("/api/v1/picking/orders/" + orderInList.getOrderNumber(), accessToken, tenantId).exchange().expectStatus().isOk()
                                                                        .expectBody(new ParameterizedTypeReference<ApiResponse<OrderQueryResult>>() {
                                                                        }).returnResult();

                                                        ApiResponse<OrderQueryResult> orderApiResponse = orderResult.getResponseBody();
                                                        if (orderApiResponse != null && orderApiResponse.isSuccess() && orderApiResponse.getData() != null) {
                                                            OrderQueryResult order = orderApiResponse.getData();
                                                            
                                                            // Check if order is completed and matches product code if specified
                                                            if (isOrderCompleted(order) && matchesProductCode(order, productCode)) {
                                                                log.debug("Found completed order {} from picking list {}, reusing it", 
                                                                        order.getOrderNumber(), pickingListView.getId());
                                                                // Save to repository for future reuse
                                                                TestDataManager.saveOrder(order, tenantId);
                                                                return order.getOrderNumber();
                                                            }
                                                        }
                                                    } catch (Exception e) {
                                                        log.debug("Error querying order {}: {}, continuing", orderInList.getOrderNumber(), e.getMessage());
                                                        // Continue checking other orders
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.debug("Error querying picking list {}: {}, continuing", pickingListView.getId(), e.getMessage());
                            // Continue checking other picking lists
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error finding completed orders from picking lists: {}", e.getMessage());
        }
        
        return null; // No suitable order found
    }

    /**
     * Check if an order is completed (all line items have pickedQuantity > 0).
     *
     * @param order the order to check
     * @return true if the order is completed, false otherwise
     */
    private boolean isOrderCompleted(OrderQueryResult order) {
        if (order.getLineItems() == null || order.getLineItems().isEmpty()) {
            return false;
        }
        return order.getLineItems().stream()
                .allMatch(item -> item.getPickedQuantity() != null && item.getPickedQuantity() > 0);
    }

    /**
     * Check if an order matches the specified product code.
     * If productCode is null or empty, matches any order.
     * Matches by converting productCode to productId first, since OrderQueryResult uses productId.
     *
     * @param order       the order to check
     * @param productCode the product code to match (can be null)
     * @return true if the order matches the product code or productCode is null/empty
     */
    private boolean matchesProductCode(OrderQueryResult order, String productCode) {
        if (productCode == null || productCode.isEmpty()) {
            return true; // Match any product if no specific product code provided
        }
        if (order.getLineItems() == null || order.getLineItems().isEmpty()) {
            return false;
        }
        
        // Get productId from productCode to match against order line items
        // Use testProductId if productCode matches testProductCode, otherwise look it up
        String productIdToMatch = null;
        if (productCode.equals(testProductCode)) {
            productIdToMatch = testProductId;
        } else {
            // Try to find product by code in repository
            Optional<CreateProductResponse> product = TestDataManager.getProductByCode(productCode, testTenantId);
            if (product.isPresent()) {
                productIdToMatch = product.get().getProductId();
            } else {
                // If product not found, can't match - return false
                return false;
            }
        }
        
        // Match by productId
        final String finalProductId = productIdToMatch;
        return order.getLineItems().stream()
                .anyMatch(item -> finalProductId != null && finalProductId.equals(item.getProductId()));
    }

    /**
     * Find an existing completed order or create a new one.
     * First checks all orders in H2 repository for any completed orders.
     * Then checks completed picking lists for completed orders.
     * If a completed order is found, reuses it. Otherwise, creates a new one with a random order number.
     *
     * @param productCode the product code for the order line items
     * @param tenantId    the tenant ID
     * @param accessToken the access token
     * @return the order number of a completed order (existing or newly created)
     */
    private String findOrCreateCompletedOrder(String productCode, String tenantId, String accessToken) {
        // First, check all existing orders in H2 repository for any completed orders
        List<OrderQueryResult> existingOrders = TestDataManager.findAllOrders(tenantId);
        
        for (OrderQueryResult existingOrder : existingOrders) {
            String orderNumber = existingOrder.getOrderNumber();
            // Verify the order is completed by querying the API
            try {
                EntityExchangeResult<ApiResponse<OrderQueryResult>> verifyResult =
                        authenticatedGet("/api/v1/picking/orders/" + orderNumber, accessToken, tenantId).exchange().expectStatus().isOk()
                                .expectBody(new ParameterizedTypeReference<ApiResponse<OrderQueryResult>>() {
                                }).returnResult();

                ApiResponse<OrderQueryResult> verifyApiResponse = verifyResult.getResponseBody();
                if (verifyApiResponse != null && verifyApiResponse.isSuccess() && verifyApiResponse.getData() != null) {
                    OrderQueryResult order = verifyApiResponse.getData();
                    // Check if order is completed and matches product code
                    if (isOrderCompleted(order) && matchesProductCode(order, productCode)) {
                        log.debug("Reusing existing completed order from H2: {}", orderNumber);
                        // Update repository with latest order data
                        TestDataManager.saveOrder(order, tenantId);
                        return orderNumber;
                    }
                }
            } catch (Exception e) {
                log.debug("Error verifying existing order {}: {}, continuing to check other orders", orderNumber, e.getMessage());
                // Continue checking other orders
            }
        }
        
        // No completed order found in H2, try to find one from completed picking lists
        String reusedOrderNumber = findAndReuseCompletedOrderFromPickingList(productCode, tenantId, accessToken, null);
        if (reusedOrderNumber != null) {
            log.debug("Reusing completed order from picking list: {}", reusedOrderNumber);
            return reusedOrderNumber;
        }
        
        // No completed order found anywhere, create a new one with a random order number
        String newOrderNumber = "ORD-" + faker.number().digits(8);
        log.debug("No completed order found, creating new one: {}", newOrderNumber);
        setupCompletedPickingOrder(newOrderNumber, productCode, tenantId, accessToken);
        return newOrderNumber;
    }

    /**
     * Helper method to set up a completed picking order.
     * First tries to reuse existing completed orders from H2 database or from completed picking lists.
     * Only creates new data if no suitable existing order is found.
     *
     * @param orderNumber the order number to use (if null or empty, will find any available completed order)
     * @param productCode the product code for the order line items
     * @param tenantId    the tenant ID
     * @param accessToken the access token
     */
    private void setupCompletedPickingOrder(String orderNumber, String productCode, String tenantId, String accessToken) {
        try {
            // First, check if the specific order number already exists and is completed
            if (orderNumber != null && !orderNumber.isEmpty()) {
                Optional<OrderQueryResult> existingOrder = TestDataManager.getRepository().findOrderByNumber(orderNumber, tenantId);
                if (existingOrder.isPresent()) {
                    // Verify the order is completed by querying the API
                    try {
                        EntityExchangeResult<ApiResponse<OrderQueryResult>> verifyResult =
                                authenticatedGet("/api/v1/picking/orders/" + orderNumber, accessToken, tenantId).exchange().expectStatus().isOk()
                                        .expectBody(new ParameterizedTypeReference<ApiResponse<OrderQueryResult>>() {
                                        }).returnResult();

                        ApiResponse<OrderQueryResult> verifyApiResponse = verifyResult.getResponseBody();
                        if (verifyApiResponse != null && verifyApiResponse.isSuccess() && verifyApiResponse.getData() != null) {
                            OrderQueryResult order = verifyApiResponse.getData();
                            // Check if order is completed (all line items have pickedQuantity > 0)
                            if (order.getLineItems() != null && !order.getLineItems().isEmpty()) {
                                boolean allPicked = order.getLineItems().stream()
                                        .allMatch(item -> item.getPickedQuantity() != null && item.getPickedQuantity() > 0);
                                if (allPicked) {
                                    log.debug("Order {} already exists and is completed, reusing it", orderNumber);
                                    // Update repository with latest order data
                                    TestDataManager.saveOrder(order, tenantId);
                                    return; // Skip creation, order is ready
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.debug("Error verifying existing order {}: {}, will check for other completed orders", orderNumber, e.getMessage());
                        // Continue to check for other completed orders
                    }
                }
            }

            // Try to find and reuse an existing completed order from a completed picking list
            String reusedOrderNumber = findAndReuseCompletedOrderFromPickingList(productCode, tenantId, accessToken, orderNumber);
            if (reusedOrderNumber != null) {
                log.debug("Reusing existing completed order {} from picking list", reusedOrderNumber);
                // Update the orderNumber parameter if it was null/empty
                if (orderNumber == null || orderNumber.isEmpty()) {
                    orderNumber = reusedOrderNumber;
                }
                return; // Order is ready, skip creation
            }

            // No suitable existing order found, create new one
            log.debug("Setting up new completed picking order: {}", orderNumber);
            
            // 1. Check H2 for existing stock items before creating consignment
            List<com.ccbsa.wms.gateway.api.dto.StockItemResponse> existingStockItems = TestDataManager.getStockItemsByProductId(testProductId, tenantId);
            int totalQuantity = existingStockItems.stream()
                    .filter(item -> item.getQuantity() != null && item.getQuantity() > 0)
                    .mapToInt(com.ccbsa.wms.gateway.api.dto.StockItemResponse::getQuantity)
                    .sum();

            if (totalQuantity < 100) {
                // Create stock consignment for picking (100 units)
                com.ccbsa.wms.gateway.api.dto.CreateConsignmentRequest consignmentRequest =
                        com.ccbsa.wms.gateway.api.fixture.ConsignmentTestDataBuilder.buildCreateConsignmentRequestV2(testLocationId, productCode, 100, null);
                EntityExchangeResult<ApiResponse<com.ccbsa.wms.gateway.api.dto.CreateConsignmentResponse>> consignmentResult =
                        authenticatedPost("/api/v1/stock-management/consignments", accessToken, tenantId, consignmentRequest)
                                .exchange()
                                .expectStatus().isCreated()
                                .expectBody(new ParameterizedTypeReference<ApiResponse<com.ccbsa.wms.gateway.api.dto.CreateConsignmentResponse>>() {
                                }).returnResult();

                ApiResponse<com.ccbsa.wms.gateway.api.dto.CreateConsignmentResponse> consignmentApiResponse = consignmentResult.getResponseBody();
                if (consignmentApiResponse != null && consignmentApiResponse.isSuccess() && consignmentApiResponse.getData() != null) {
                    com.ccbsa.wms.gateway.api.dto.CreateConsignmentResponse consignment = consignmentApiResponse.getData();
                    TestDataManager.saveConsignment(consignment, tenantId);
                }

                // Wait for stock items to be created
                waitForStockItems(testProductId, accessToken, tenantId, 10, 500);
                // Query and save stock items to H2
                queryAndSaveStockItems(testProductId, accessToken, tenantId);
            }

            // Wait for stock items to be assigned to locations (FEFO assignment happens asynchronously)
            waitForStockItemsAssignedToLocations(testProductId, accessToken, tenantId, 15, 500, 1);

            // 2. Create picking list with the specified order number
            com.ccbsa.wms.gateway.api.dto.CreatePickingListRequest pickingListRequest = com.ccbsa.wms.gateway.api.fixture.PickingListTestDataBuilder.buildCreatePickingListRequest();
            pickingListRequest.getLoads().get(0).getOrders().get(0).setOrderNumber(orderNumber);
            pickingListRequest.getLoads().get(0).getOrders().get(0).getLineItems().get(0).setProductCode(productCode);
            pickingListRequest.getLoads().get(0).getOrders().get(0).getLineItems().get(0).setQuantity(100);

            EntityExchangeResult<ApiResponse<com.ccbsa.wms.gateway.api.dto.CreatePickingListResponse>> pickingListResult =
                    authenticatedPost("/api/v1/picking/picking-lists", accessToken, tenantId, pickingListRequest).exchange().expectStatus().isCreated()
                            .expectBody(new ParameterizedTypeReference<ApiResponse<com.ccbsa.wms.gateway.api.dto.CreatePickingListResponse>>() {
                            }).returnResult();

            ApiResponse<com.ccbsa.wms.gateway.api.dto.CreatePickingListResponse> pickingListApiResponse = pickingListResult.getResponseBody();
            assertThat(pickingListApiResponse).isNotNull();
            assertThat(pickingListApiResponse.isSuccess()).isTrue();
            String pickingListId = pickingListApiResponse.getData().getPickingListId();

            // 3. Wait for picking list to be planned
            boolean isPlanned = waitForPickingListStatus(pickingListId, "PLANNED", accessToken, tenantId, 60, 500);
            if (!isPlanned) {
                throw new AssertionError("Picking list did not reach PLANNED status within timeout period. Picking list ID: " + pickingListId);
            }

            // 4. Get picking list to find load IDs and order IDs
            EntityExchangeResult<ApiResponse<com.ccbsa.wms.gateway.api.dto.PickingListQueryResult>> listResult =
                    authenticatedGet("/api/v1/picking/picking-lists/" + pickingListId, accessToken, tenantId).exchange().expectStatus().isOk()
                            .expectBody(new ParameterizedTypeReference<ApiResponse<com.ccbsa.wms.gateway.api.dto.PickingListQueryResult>>() {
                            }).returnResult();

            com.ccbsa.wms.gateway.api.dto.PickingListQueryResult pickingListData = listResult.getResponseBody().getData();
            java.util.Set<String> loadIds = new java.util.HashSet<>();
            java.util.Set<String> orderIds = new java.util.HashSet<>();
            if (pickingListData.getLoads() != null) {
                for (com.ccbsa.wms.gateway.api.dto.PickingListQueryResult.LoadQueryResult load : pickingListData.getLoads()) {
                    if (load.getLoadId() != null) {
                        loadIds.add(load.getLoadId());
                    }
                    if (load.getOrders() != null) {
                        for (com.ccbsa.wms.gateway.api.dto.PickingListQueryResult.OrderQueryResult order : load.getOrders()) {
                            if (order.getOrderId() != null) {
                                orderIds.add(order.getOrderId());
                            }
                        }
                    }
                }
            }

            // 5. Wait a bit for tasks to be created
            Thread.sleep(2000);

            // 6. Get and execute all picking tasks
            EntityExchangeResult<ApiResponse<java.util.Map<String, Object>>> tasksResult =
                    authenticatedGet("/api/v1/picking/tasks?status=PENDING&page=0&size=100", accessToken, tenantId).exchange().expectStatus().isOk()
                            .expectBody(new ParameterizedTypeReference<ApiResponse<java.util.Map<String, Object>>>() {
                            }).returnResult();

            ApiResponse<java.util.Map<String, Object>> tasksApiResponse = tasksResult.getResponseBody();
            @SuppressWarnings("unchecked") java.util.List<java.util.Map<String, Object>> allTasks =
                    (java.util.List<java.util.Map<String, Object>>) tasksApiResponse.getData().get("pickingTasks");

            // Execute all tasks for this picking list (filter by loadId or orderId)
            int executedTasks = 0;
            if (allTasks != null && !allTasks.isEmpty()) {
                for (java.util.Map<String, Object> task : allTasks) {
                    String taskLoadId = (String) task.get("loadId");
                    String taskOrderId = (String) task.get("orderId");
                    boolean shouldExecute = false;
                    
                    if (taskLoadId != null && loadIds.contains(taskLoadId)) {
                        shouldExecute = true;
                    } else if (taskOrderId != null && orderIds.contains(taskOrderId)) {
                        shouldExecute = true;
                    }
                    
                    if (shouldExecute) {
                        String taskId = (String) task.get("taskId");
                        Object quantityObj = task.get("quantity");
                        Integer quantity = quantityObj instanceof Integer ? (Integer) quantityObj : quantityObj instanceof Number ? ((Number) quantityObj).intValue() : null;

                        if (taskId != null && quantity != null) {
                            com.ccbsa.wms.gateway.api.dto.ExecutePickingTaskRequest executeRequest =
                                    com.ccbsa.wms.gateway.api.dto.ExecutePickingTaskRequest.builder().pickedQuantity(quantity).isPartialPicking(false).build();

                            authenticatedPost("/api/v1/picking/picking-tasks/" + taskId + "/execute", accessToken, tenantId, executeRequest).exchange()
                                    .expectStatus().isOk();
                            executedTasks++;
                        }
                    }
                }
            } else {
                throw new AssertionError(String.format("No picking tasks found for order %s. Expected tasks to be created after planning.", orderNumber));
            }

            // Verify at least one task was executed
            if (executedTasks == 0) {
                throw new AssertionError(String.format("No picking tasks were executed for order %s. Found %d tasks but none matched loadIds or orderIds.",
                        orderNumber, allTasks != null ? allTasks.size() : 0));
            }

            log.debug("Executed {} picking task(s) for order {}", executedTasks, orderNumber);

            // 7. Wait for tasks to be persisted after execution and transactions to commit
            Thread.sleep(5000); // Wait for all task executions to be persisted

            // Verify tasks are saved by checking order picking status
            log.debug("Verifying that picked quantities are visible in order query for order: {}", orderNumber);
            int verificationAttempts = 0;
            boolean tasksVisible = false;
            while (verificationAttempts < 20 && !tasksVisible) {
                try {
                    EntityExchangeResult<ApiResponse<OrderQueryResult>> verifyResult =
                            authenticatedGet("/api/v1/picking/orders/" + orderNumber, accessToken, tenantId).exchange().expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<ApiResponse<OrderQueryResult>>() {
                                    }).returnResult();

                    ApiResponse<OrderQueryResult> verifyApiResponse = verifyResult.getResponseBody();
                    if (verifyApiResponse != null && verifyApiResponse.isSuccess() && verifyApiResponse.getData() != null) {
                        OrderQueryResult verifyOrder = verifyApiResponse.getData();
                        if (verifyOrder.getLineItems() != null && !verifyOrder.getLineItems().isEmpty()) {
                            boolean allPicked = verifyOrder.getLineItems().stream()
                                    .allMatch(item -> item.getPickedQuantity() != null && item.getPickedQuantity() > 0);
                            if (allPicked) {
                                log.debug("Order picking verified - all line items have picked quantities > 0");
                                tasksVisible = true;
                                break;
                            } else {
                                log.debug("Order picking NOT YET complete - attempt {}: lineItems with 0 picked quantity found", verificationAttempts + 1);
                            }
                        } else {
                            log.debug("Order has no line items - attempt {}", verificationAttempts + 1);
                        }
                    }
                } catch (Exception e) {
                    log.debug("Error verifying order picking (attempt {}): {}", verificationAttempts + 1, e.getMessage());
                }
                verificationAttempts++;
                if (verificationAttempts < 20) {
                    Thread.sleep(1000);  // Wait 1 second between attempts
                }
            }

            if (!tasksVisible) {
                log.warn("Warning: Could not verify that picked quantities are visible after {} attempts, but continuing...", verificationAttempts);
            }

            // 8. Complete picking list
            authenticatedPostWithoutBody("/api/v1/picking/picking-lists/" + pickingListId + "/complete", accessToken, tenantId).exchange()
                    .expectStatus().isOk();

            // 9. Wait for picking list to be completed
            boolean isCompleted = waitForPickingListStatus(pickingListId, "COMPLETED", accessToken, tenantId, 20, 500);
            if (!isCompleted) {
                log.warn("Warning: Picking list did not reach COMPLETED status, but continuing...");
            }

            // 10. Wait for order picking to be completed (verify via order query)
            // Increased timeout and poll interval for better reliability
            boolean orderPickingCompleted = waitForOrderPickingCompleted(orderNumber, accessToken, tenantId, 30, 500);
            if (!orderPickingCompleted) {
                log.warn("Warning: Order picking did not complete, but continuing...");
            }

            // 11. Save the completed order to repository for reuse in future test runs
            try {
                EntityExchangeResult<ApiResponse<OrderQueryResult>> finalOrderResult =
                        authenticatedGet("/api/v1/picking/orders/" + orderNumber, accessToken, tenantId).exchange().expectStatus().isOk()
                                .expectBody(new ParameterizedTypeReference<ApiResponse<OrderQueryResult>>() {
                                }).returnResult();

                ApiResponse<OrderQueryResult> finalOrderApiResponse = finalOrderResult.getResponseBody();
                if (finalOrderApiResponse != null && finalOrderApiResponse.isSuccess() && finalOrderApiResponse.getData() != null) {
                    OrderQueryResult finalOrder = finalOrderApiResponse.getData();
                    TestDataManager.saveOrder(finalOrder, tenantId);
                    log.debug("Saved completed order {} to repository for reuse", orderNumber);
                }
            } catch (Exception e) {
                log.warn("Failed to save order {} to repository: {}", orderNumber, e.getMessage());
                // Don't fail the test if repository save fails
            }
        } catch (Exception e) {
            // If setup fails, throw an assertion error with details
            String errorMsg = String.format("Failed to set up completed picking order for order %s: %s", orderNumber, e.getMessage());
            log.error(errorMsg, e);
            throw new AssertionError(errorMsg, e);
        }
    }
    
    /**
     * Wait for order picking to be completed by checking the order query endpoint.
     * Polls the order endpoint until all line items have picked quantities > 0 or timeout occurs.
     *
     * @param orderNumber  the order number to check
     * @param accessToken  the JWT access token for authentication
     * @param tenantId     the tenant ID for X-Tenant-Id header
     * @param maxWaitSeconds maximum number of seconds to wait
     * @param pollIntervalMs polling interval in milliseconds
     * @return true if the order picking is completed, false if timeout was reached
     */
    private boolean waitForOrderPickingCompleted(String orderNumber, String accessToken, String tenantId, int maxWaitSeconds, int pollIntervalMs) {
        long endTime = System.currentTimeMillis() + (maxWaitSeconds * 1000L);
        int attemptCount = 0;

        while (System.currentTimeMillis() < endTime) {
            attemptCount++;
            try {
                EntityExchangeResult<ApiResponse<OrderQueryResult>> result =
                        authenticatedGet("/api/v1/picking/orders/" + orderNumber, accessToken, tenantId).exchange().expectStatus().isOk()
                                .expectBody(new ParameterizedTypeReference<ApiResponse<OrderQueryResult>>() {
                                }).returnResult();

                ApiResponse<OrderQueryResult> apiResponse = result.getResponseBody();
                if (apiResponse != null && apiResponse.isSuccess() && apiResponse.getData() != null) {
                    OrderQueryResult order = apiResponse.getData();
                    if (order.getLineItems() != null && !order.getLineItems().isEmpty()) {
                        // Check if all line items have picked quantity > 0
                        boolean allPicked = order.getLineItems().stream()
                                .allMatch(item -> item.getPickedQuantity() != null && item.getPickedQuantity() > 0);
                        if (allPicked) {
                            log.debug("Order picking completed after {} attempts", attemptCount);
                            // Additional wait to ensure database consistency
                            Thread.sleep(1000);
                            return true;
                        }
                    } else {
                        // If no line items, log for debugging
                        if (attemptCount == 1) {
                            log.debug("Order found but has no line items: {}", orderNumber);
                        }
                    }
                }

                if (attemptCount % 4 == 0) { // Log every 2 seconds (4 attempts * 500ms)
                    log.debug("Waiting for order picking to complete... attempt {}", attemptCount);
                }

                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } catch (Exception e) {
                // Continue polling on error, but log first few errors
                if (attemptCount <= 3) {
                    log.warn("Error polling for order picking completion (attempt {}): {}", attemptCount, e.getMessage());
                }
                try {
                    Thread.sleep(pollIntervalMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        log.warn("Timeout waiting for order picking to complete after {} attempts ({} seconds)", attemptCount, maxWaitSeconds);
        return false;
    }

    // ==================== PARTIAL ORDER ACCEPTANCE TESTS ====================

    @Test
    @Order(1)
    public void testHandlePartialOrderAcceptance_Success() {
        // Arrange
        HandlePartialOrderAcceptanceRequest request = ReturnsTestDataBuilder.buildHandlePartialOrderAcceptanceRequest(testOrderNumber, testProductId);

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost("/api/v1/returns/partial-acceptance", tenantAdminAuth.getAccessToken(), testTenantId, request).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<HandlePartialOrderAcceptanceResponse>> exchangeResult =
                response.expectStatus().isCreated().expectBody(new ParameterizedTypeReference<ApiResponse<HandlePartialOrderAcceptanceResponse>>() {
                }).returnResult();

        ApiResponse<HandlePartialOrderAcceptanceResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        HandlePartialOrderAcceptanceResponse result = apiResponse.getData();
        assertThat(result).isNotNull();
        assertThat(result.getReturnId()).isNotBlank();
        assertThat(result.getOrderNumber()).isEqualTo(testOrderNumber);
        assertThat(result.getStatus()).isEqualTo("INITIATED");
        assertThat(result.getReturnType()).isEqualTo("PARTIAL");
    }

    // ==================== FULL ORDER RETURN TESTS ====================

    @Test
    @Order(2)
    public void testProcessFullOrderReturn_Success() {
        // Arrange - Use a different order number for full return test
        String fullReturnOrderNumber = testOrderNumber + "-FULL";
        setupCompletedPickingOrder(fullReturnOrderNumber, testProductCode, testTenantId, tenantAdminAuth.getAccessToken());
        ProcessFullOrderReturnRequest request = ReturnsTestDataBuilder.buildProcessFullOrderReturnRequest(fullReturnOrderNumber, testProductId);

        // Act
        WebTestClient.ResponseSpec response = authenticatedPost("/api/v1/returns/full-return", tenantAdminAuth.getAccessToken(), testTenantId, request).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<ProcessFullOrderReturnResponse>> exchangeResult =
                response.expectStatus().isCreated().expectBody(new ParameterizedTypeReference<ApiResponse<ProcessFullOrderReturnResponse>>() {
                }).returnResult();

        ApiResponse<ProcessFullOrderReturnResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        ProcessFullOrderReturnResponse result = apiResponse.getData();
        assertThat(result).isNotNull();
        assertThat(result.getReturnId()).isNotBlank();
        assertThat(result.getOrderNumber()).isEqualTo(testOrderNumber + "-FULL");
        assertThat(result.getStatus()).isEqualTo("PROCESSED");
        assertThat(result.getReturnType()).isEqualTo("FULL");
    }

    // ==================== QUERY TESTS ====================

    @Test
    @Order(3)
    public void testGetReturnById_Success() {
        // Set up a completed picking order for this test
        String orderNumberForTest = testOrderNumber + "-2";
        setupCompletedPickingOrder(orderNumberForTest, testProductCode, testTenantId, tenantAdminAuth.getAccessToken());

        // First create a return
        HandlePartialOrderAcceptanceRequest createRequest = ReturnsTestDataBuilder.buildHandlePartialOrderAcceptanceRequest(orderNumberForTest, testProductId);
        EntityExchangeResult<ApiResponse<HandlePartialOrderAcceptanceResponse>> createResult =
                authenticatedPost("/api/v1/returns/partial-acceptance", tenantAdminAuth.getAccessToken(), testTenantId, createRequest).exchange().expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<ApiResponse<HandlePartialOrderAcceptanceResponse>>() {
                        }).returnResult();

        HandlePartialOrderAcceptanceResponse createdReturn = createResult.getResponseBody().getData();
        String returnId = createdReturn.getReturnId();

        // Act - Get return by ID
        WebTestClient.ResponseSpec response = authenticatedGet("/api/v1/returns/" + returnId, tenantAdminAuth.getAccessToken(), testTenantId).exchange();

        // Assert
        EntityExchangeResult<ApiResponse<ReturnResponse>> exchangeResult = response.expectStatus().isOk().expectBody(new ParameterizedTypeReference<ApiResponse<ReturnResponse>>() {
        }).returnResult();

        ApiResponse<ReturnResponse> apiResponse = exchangeResult.getResponseBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        ReturnResponse returnData = apiResponse.getData();
        assertThat(returnData).isNotNull();
        assertThat(returnData.getReturnId()).isEqualTo(returnId);
    }

    @Test
    @Order(4)
    public void testListReturnsByStatus_Success() {
        // Act
        WebTestClient.ResponseSpec response = authenticatedGet("/api/v1/returns?status=INITIATED&page=0&size=10", tenantAdminAuth.getAccessToken(), testTenantId).exchange();

        // Assert
        response.expectStatus().isOk();
    }
}

