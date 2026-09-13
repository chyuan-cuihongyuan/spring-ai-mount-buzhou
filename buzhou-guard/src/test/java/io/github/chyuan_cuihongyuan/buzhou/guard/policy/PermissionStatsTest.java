package io.github.chyuan_cuihongyuan.buzhou.guard.policy;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionStatsTest {

    private ToolPermissions permissions() {
        return new ToolPermissions(Map.of(
                "admin", java.util.List.of("*"),
                "writer", java.util.List.of("log*", "deploy"),
                "viewer", java.util.List.of("read_only")));
    }

    @Test
    void freshPermissionsHaveZeroCounts() {
        assertThat(permissions().stats())
                .isEqualTo(new ToolPermissions.PermissionStats(0, 0, 0, 0));
    }

    @Test
    void anyPatternCountsAllowed() {
        ToolPermissions permissions = permissions();

        assertThat(permissions.allows("admin", "anything")).isTrue();

        ToolPermissions.PermissionStats stats = permissions.stats();
        assertThat(stats.checks()).isEqualTo(1);
        assertThat(stats.allowed()).isEqualTo(1);
        assertThat(stats.deniedUndefinedRole()).isZero();
        assertThat(stats.deniedByRules()).isZero();
    }

    @Test
    void undefinedRoleCountedAsDeniedUndefined() {
        ToolPermissions permissions = permissions();

        assertThat(permissions.allows("typo_role", "read_only")).isFalse();

        ToolPermissions.PermissionStats stats = permissions.stats();
        assertThat(stats.checks()).isEqualTo(1);
        assertThat(stats.deniedUndefinedRole()).isEqualTo(1);
        assertThat(stats.allowed()).isZero();
    }

    @Test
    void definedRoleWithoutMatchCountedAsDeniedByRules() {
        ToolPermissions permissions = permissions();

        assertThat(permissions.allows("viewer", "write_file")).isFalse();

        ToolPermissions.PermissionStats stats = permissions.stats();
        assertThat(stats.deniedByRules()).isEqualTo(1);
        assertThat(stats.deniedUndefinedRole()).isZero();
    }

    @Test
    void prefixPatternCountsAllowed() {
        ToolPermissions permissions = permissions();

        assertThat(permissions.allows("writer", "log_cleanup")).isTrue();

        ToolPermissions.PermissionStats stats = permissions.stats();
        assertThat(stats.allowed()).isEqualTo(1);
        assertThat(stats.deniedByRules()).isZero();
    }

    @Test
    void conservationHoldsAcrossMixedDecisions() {
        ToolPermissions permissions = permissions();

        permissions.allows("admin", "x");
        permissions.allows("writer", "log_x");
        permissions.allows("writer", "deploy");
        permissions.allows("viewer", "write_file");
        permissions.allows("typo", "x");
        permissions.allows(null, "x");

        ToolPermissions.PermissionStats stats = permissions.stats();
        assertThat(stats.checks()).isEqualTo(6);
        assertThat(stats.allowed()).isEqualTo(3);
        assertThat(stats.deniedByRules()).isEqualTo(1);
        assertThat(stats.deniedUndefinedRole()).isEqualTo(2);
        assertThat(stats.allowed() + stats.deniedUndefinedRole() + stats.deniedByRules())
                .isEqualTo(stats.checks());
    }
}
