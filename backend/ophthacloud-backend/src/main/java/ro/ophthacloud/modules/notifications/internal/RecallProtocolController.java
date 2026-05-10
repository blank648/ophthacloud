package ro.ophthacloud.modules.notifications.internal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ophthacloud.modules.notifications.NotificationsFacade;
import ro.ophthacloud.modules.notifications.dto.CreateRecallProtocolRequest;
import ro.ophthacloud.modules.notifications.dto.RecallProtocolDto;
import ro.ophthacloud.shared.api.ApiResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications/recall-protocols")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification management")
public class RecallProtocolController {

    private final NotificationsFacade facade;

    @GetMapping
    @PreAuthorize("hasPermission('notifications', 'MODULE', 'VIEW')")
    @Operation(summary = "List recall protocols")
    public ResponseEntity<ApiResponse<Page<RecallProtocolDto>>> listRecallProtocols(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(new ApiResponse<>(facade.listRecallProtocols(pageable)));
    }

    @PostMapping
    @PreAuthorize("hasPermission('notifications', 'MODULE', 'CREATE')")
    @Operation(summary = "Create recall protocol")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApiResponse<RecallProtocolDto>> createRecallProtocol(
            @Valid @RequestBody CreateRecallProtocolRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(facade.createRecallProtocol(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission('notifications', 'MODULE', 'EDIT')")
    @Operation(summary = "Update recall protocol")
    public ResponseEntity<ApiResponse<RecallProtocolDto>> updateRecallProtocol(
            @PathVariable UUID id,
            @Valid @RequestBody CreateRecallProtocolRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(facade.updateRecallProtocol(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission('notifications', 'MODULE', 'DELETE')")
    @Operation(summary = "Delete recall protocol")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteRecallProtocol(@PathVariable UUID id) {
        facade.deleteRecallProtocol(id);
        return ResponseEntity.noContent().build();
    }
}
