package ro.ophthacloud.modules.notifications.internal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ophthacloud.modules.notifications.NotificationsFacade;
import ro.ophthacloud.modules.notifications.dto.NotificationLogDto;
import ro.ophthacloud.shared.api.ApiResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications/log")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification management")
public class NotificationLogController {

    private final NotificationsFacade facade;

    @GetMapping
    @PreAuthorize("hasPermission('notifications', 'MODULE', 'VIEW')")
    @Operation(summary = "List notification logs")
    public ResponseEntity<ApiResponse<Page<NotificationLogDto>>> listNotificationLog(
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) NotificationStatus status,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(new ApiResponse<>(facade.listNotificationLog(patientId, status, pageable)));
    }
}
