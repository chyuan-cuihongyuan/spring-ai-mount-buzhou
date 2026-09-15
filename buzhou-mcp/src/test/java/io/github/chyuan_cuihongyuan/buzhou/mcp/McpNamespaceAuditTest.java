package io.github.chyuan_cuihongyuan.buzhou.mcp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1737 / T2676：McpNamespaceAudit 直测——冲突判定/占用集合/空态。
 */
class McpNamespaceAuditTest {

    @Test
    void collisionsAreDetected() {
        var audit = new McpNamespaceAudit();
        audit.register("fs", "read");
        audit.register("git", "read");
        audit.register("fs", "write");
        var census = audit.census();
        assertThat(census.tools()).isEqualTo(2);
        assertThat(census.collidingTools()).isEqualTo(1);
        assertThat(census.maxServersPerTool()).isEqualTo(2);
        assertThat(audit.serversOf("read")).containsExactlyInAnyOrder("fs", "git");
        assertThat(audit.serversOf("write")).containsExactly("fs");
    }

    @Test
    void duplicateRegistrationIsIdempotent() {
        var audit = new McpNamespaceAudit();
        audit.register("fs", "read");
        audit.register("fs", "read");
        assertThat(audit.census().maxServersPerTool()).isEqualTo(1);
    }

    @Test
    void emptyAndAnonymous() {
        var audit = new McpNamespaceAudit();
        assertThat(audit.census().tools()).isZero();
        assertThat(audit.serversOf("nope")).isEmpty();
        audit.register(null, "tool");
        assertThat(audit.serversOf("tool")).contains("_anonymous_");
    }
}
