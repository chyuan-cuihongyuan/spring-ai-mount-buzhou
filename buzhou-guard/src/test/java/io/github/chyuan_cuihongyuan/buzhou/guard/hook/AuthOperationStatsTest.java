package io.github.chyuan_cuihongyuan.buzhou.guard.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.guard.config.AuthTtl;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthOperationStatsTest {

    private GuardAuthApi api() {
        SessionStateStore stateStore = new InMemorySessionStateStore();
        return new GuardAuthApi(stateStore, AuthTtl.ONCE, null);
    }

    @Test
    void freshApiHasZeroCounts() {
        assertThat(api().stats()).isEqualTo(new GuardAuthApi.AuthOperationStats(0, 0, 0));
    }

    @Test
    void approveRejectRevokeEachCounted() {
        GuardAuthApi api = api();
        Map<String, Object> args = Map.of("table", "users");

        api.approve("s1", "delete_records", args);
        api.reject("s1", "delete_records", args, "reject", "too risky");
        api.revoke("s1", "delete_records", args);

        assertThat(api.stats()).isEqualTo(new GuardAuthApi.AuthOperationStats(1, 1, 1));
    }

    @Test
    void rejectDoesNotWriteAuthorization() {
        GuardAuthApi api = api();

        api.reject("s1", "delete_records", Map.of(), "reject", null);

        assertThat(api.isAuthorized("s1", "delete_records", Map.of())).isFalse();
        assertThat(api.stats().rejected()).isEqualTo(1);
    }

    @Test
    void convenientApproveOverloadCountedToo() {
        GuardAuthApi api = api();

        api.approve("s1", "delete_records", Map.of("table", "users"));
        api.approve("s1", "delete_records", Map.of("table", "users"), "approve", null, 3);

        assertThat(api.stats().approved()).isEqualTo(2);
    }
}
