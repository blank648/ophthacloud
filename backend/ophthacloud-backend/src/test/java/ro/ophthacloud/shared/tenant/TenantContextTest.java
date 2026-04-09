package ro.ophthacloud.shared.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

@DisplayName("TenantContext")
class TenantContextTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("get: should return null when context is empty")
    void get_shouldReturnNull_whenNotSet() {
        assertThat(TenantContext.get()).isNull();
    }

    @Test
    @DisplayName("set/get: should return the stored tenant ID")
    void set_get_shouldReturnStoredTenantId() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.set(tenantId);
        assertThat(TenantContext.get()).isEqualTo(tenantId);
    }

    @Test
    @DisplayName("require: should return the tenant ID when set")
    void require_shouldReturnTenantId_whenSet() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.set(tenantId);
        assertThat(TenantContext.require()).isEqualTo(tenantId);
    }

    @Test
    @DisplayName("require: should throw IllegalStateException when context is empty")
    void require_shouldThrow_whenNotSet() {
        assertThatThrownBy(TenantContext::require)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TenantContext is empty");
    }

    @Test
    @DisplayName("clear: should remove tenant ID from context")
    void clear_shouldRemoveTenantId() {
        TenantContext.set(UUID.randomUUID());
        TenantContext.clear();
        assertThat(TenantContext.get()).isNull();
    }

    @Test
    @DisplayName("isolation: should be isolated across threads")
    void threadIsolation_shouldHoldSeparateValues_perThread() throws InterruptedException {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();

        AtomicReference<UUID> threadBObserved = new AtomicReference<>();

        TenantContext.set(tenantA);

        Thread threadB = new Thread(() -> {
            TenantContext.set(tenantB);
            threadBObserved.set(TenantContext.get());
            TenantContext.clear();
        });

        threadB.start();
        threadB.join();

        // Main thread still sees tenantA — not overwritten by thread B
        assertThat(TenantContext.get()).isEqualTo(tenantA);
        assertThat(threadBObserved.get()).isEqualTo(tenantB);
    }
}
