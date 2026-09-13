package io.github.chyuan_cuihongyuan.buzhou.resilience.capability;

import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 700 / T1000–T1001：能力门决策审计——deny 环形留痕（容量覆盖+dropped）、
 * admit 只计数、denyByModel 聚合、snapshot 不可变、null fail-fast、
 * advisor 接线（throw 前留痕、透传不计）。
 */
class CapabilityDecisionAuditTest {

    /** 链桩（gate 放行即触链——返回即弃响应，502 测试同型）。 */
    private static final class StubChain implements org.springframework.ai.chat.client.advisor.api.CallAdvisorChain {
        @Override
        public org.springframework.ai.chat.client.ChatClientResponse nextCall(
                org.springframework.ai.chat.client.ChatClientRequest request) {
            return new org.springframework.ai.chat.client.ChatClientResponse(
                    new org.springframework.ai.chat.model.ChatResponse(List.of(
                            new org.springframework.ai.chat.model.Generation(
                                    new org.springframework.ai.chat.messages.AssistantMessage("ok")))),
                    request.context());
        }

        @Override
        public List<org.springframework.ai.chat.client.advisor.api.CallAdvisor> getCallAdvisors() {
            return List.of();
        }

        @Override
        public org.springframework.ai.chat.client.advisor.api.CallAdvisorChain copy(
                org.springframework.ai.chat.client.advisor.api.CallAdvisor advisor) {
            throw new UnsupportedOperationException();
        }
    }

    @Test
    void ringEvictsOldestAndCountsDropped() {
        CapabilityDecisionAudit audit = new CapabilityDecisionAudit(2);
        audit.recordDeny("m1", "vision");
        audit.recordDeny("m2", "tools");
        audit.recordDeny("m3", "vision");
        CapabilityDecisionAudit.Report report = audit.snapshot();
        assertThat(report.capacity()).isEqualTo(2);
        assertThat(report.denied()).isEqualTo(3);
        assertThat(report.dropped()).isEqualTo(1);
        assertThat(report.recentDenies()).hasSize(2);
        assertThat(report.recentDenies().get(0).model()).isEqualTo("m3"); // 最新在前
        assertThat(report.recentDenies().get(1).model()).isEqualTo("m2");
        assertThat(report.denyByModel()).containsEntry("m1", 1L).containsEntry("m3", 1L);
    }

    @Test
    void admitCountsOnlyAndAggregatesByModel() {
        CapabilityDecisionAudit audit = new CapabilityDecisionAudit();
        audit.recordAdmit("primary");
        audit.recordAdmit("primary");
        audit.recordDeny("primary", "vision");
        audit.recordDeny("primary", "vision");
        audit.recordDeny("other", "tools");
        CapabilityDecisionAudit.Report report = audit.snapshot();
        assertThat(report.admitted()).isEqualTo(2);
        assertThat(report.denied()).isEqualTo(3);
        assertThat(report.recentDenies()).hasSize(3);
        assertThat(report.denyByModel()).containsEntry("primary", 2L).containsEntry("other", 1L);
    }

    @Test
    void nullFieldsFailFastAndSnapshotIsImmutable() {
        CapabilityDecisionAudit audit = new CapabilityDecisionAudit();
        assertThatThrownBy(() -> audit.recordDeny(null, "vision"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> audit.recordDeny("m", null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> audit.recordAdmit(null))
                .isInstanceOf(NullPointerException.class);
        audit.recordDeny("m1", "vision");
        CapabilityDecisionAudit.Report report = audit.snapshot();
        List<CapabilityDecisionAudit.Decision> mutable = new ArrayList<>(report.recentDenies());
        mutable.clear();
        assertThat(audit.snapshot().recentDenies()).hasSize(1); // 防御拷贝——外部改不动内部
        assertThatThrownBy(() -> new CapabilityDecisionAudit(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void advisorWiringRecordsDenyAdmitAndSkipsUnregistered() {
        CapabilityDecisionAudit audit = new CapabilityDecisionAudit();
        CapabilityGateAdvisor blocked = new CapabilityGateAdvisor(
                new ModelCapabilityRegistry(Map.of("primary",
                        new ModelCapabilities(false, true, 128_000))), "primary", audit);
        boolean threw = false;
        try {
            blocked.adviseCall(request(true, false), new StubChain());
        } catch (BuzhouException expected) {
            threw = true;
        }
        assertThat(threw).isTrue();
        assertThat(audit.snapshot().denied()).isEqualTo(1);
        assertThat(audit.snapshot().recentDenies().get(0).capability()).isEqualTo("vision");

        // 能力齐备 → admit 计数
        CapabilityGateAdvisor passing = new CapabilityGateAdvisor(
                new ModelCapabilityRegistry(Map.of("primary",
                        new ModelCapabilities(true, true, 128_000))), "primary", audit);
        passing.adviseCall(request(false, false), new StubChain());
        assertThat(audit.snapshot().admitted()).isEqualTo(1);

        // 未注册模型 = 门未裁决 → 不计 admit 不计 deny
        CapabilityGateAdvisor unregistered = new CapabilityGateAdvisor(
                new ModelCapabilityRegistry(Map.of("primary",
                        new ModelCapabilities(true, true, 128_000))), "other", audit);
        unregistered.adviseCall(request(true, false), new StubChain());
        assertThat(audit.snapshot().admitted()).isEqualTo(1);
        assertThat(audit.snapshot().denied()).isEqualTo(1);
    }

    /** media 用户消息（公开 UserMessage 构造不带 media——子类覆写 getMedia()，502 测试同型）。 */
    private static final class MediaUserMessage extends org.springframework.ai.chat.messages.UserMessage {
        private final List<org.springframework.ai.content.Media> media;

        MediaUserMessage(String text, List<org.springframework.ai.content.Media> media) {
            super(text);
            this.media = media;
        }

        @Override
        public List<org.springframework.ai.content.Media> getMedia() {
            return media;
        }
    }

    private static org.springframework.ai.chat.client.ChatClientRequest request(
            boolean withMedia, boolean withTools) {
        org.springframework.ai.chat.messages.UserMessage user = withMedia
                ? new MediaUserMessage("看这张图", List.of(new org.springframework.ai.content.Media(
                        org.springframework.util.MimeType.valueOf("image/png"),
                        java.net.URI.create("file:///tmp/a.png"))))
                : new org.springframework.ai.chat.messages.UserMessage("纯文本问题");
        org.springframework.ai.chat.prompt.ChatOptions options = withTools
                ? org.springframework.ai.model.tool.ToolCallingChatOptions.builder().toolCallbacks(
                        List.of(new org.springframework.ai.tool.ToolCallback() {
                            @Override
                            public org.springframework.ai.tool.definition.ToolDefinition getToolDefinition() {
                                return org.springframework.ai.tool.definition.ToolDefinition.builder()
                                        .name("query").description("查询")
                                        .inputSchema("{\"type\":\"object\"}").build();
                            }

                            @Override
                            public String call(String toolInput) {
                                return "{}";
                            }
                        })).build()
                : org.springframework.ai.chat.prompt.ChatOptions.builder().build();
        return new org.springframework.ai.chat.client.ChatClientRequest(
                new org.springframework.ai.chat.prompt.Prompt(List.of(user), options), Map.of());
    }
}
