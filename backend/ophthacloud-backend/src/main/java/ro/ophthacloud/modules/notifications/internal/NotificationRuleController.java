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
import ro.ophthacloud.modules.notifications.dto.CreateNotificationRuleRequest;
import ro.ophthacloud.modules.notifications.dto.NotificationRuleDto;
import ro.ophthacloud.shared.api.ApiResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications/rules")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification management")
public class NotificationRuleController {

    private final NotificationsFacade facade;

    @GetMapping
    @PreAuthorize("hasPermission('notifications', 'MODULE', 'VIEW')")
    @Operation(summary = "List rules")
    public ResponseEntity<ApiResponse<Page<NotificationRuleDto>>> listRules(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(new ApiResponse<>(facade.getNotificationRules(pageable)));
    }

    @PostMapping
    @PreAuthorize("hasPermission('notifications', 'MODULE', 'CREATE')")
    @Operation(summary = "Create rule")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApiResponse<NotificationRuleDto>> createRule(
            @Valid @RequestBody CreateNotificationRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(facade.createRule(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission('notifications', 'MODULE', 'EDIT')")
    @Operation(summary = "Update rule")
    public ResponseEntity<ApiResponse<NotificationRuleDto>> updateRule(
            @PathVariable UUID id,
            @Valid @RequestBody CreateNotificationRuleRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(facade.updateRule(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission('notifications', 'MODULE', 'DELETE')")
    @Operation(summary = "Delete rule")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteRule(@PathVariable UUID id) {
        facade.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasPermission('notifications', 'MODULE', 'EDIT')")
    @Operation(summary = "Toggle rule status")
    public ResponseEntity<ApiResponse<NotificationRuleDto>> toggleRule(@PathVariable UUID id) {
        return ResponseEntity.ok(new ApiResponse<>(facade.toggleRule(id)));
    }
}
