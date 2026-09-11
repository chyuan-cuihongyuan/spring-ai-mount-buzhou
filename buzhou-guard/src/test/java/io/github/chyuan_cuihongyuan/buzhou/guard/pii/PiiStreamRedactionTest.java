package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.StreamTextFilter;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.guard.GuardModule;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 500 / T752：流式回复 PII 脱敏红队——跨 chunk 断裂实体合并检出（窗口
 * 缓冲）、flush 全量排空、无 PII 逐 chunk 恒等、占位符不拆分、自定义规则叠加、
 * 窗口参数 fail-fast、fromYml 装配。借鉴：Presidio 流式匿名化 + 流式 WAF
 * 回看窗口。
 */
class PiiStreamRedactionTest {

    /** 逐 chunk 喂入并收口，返回订阅者所见拼接文本。 */
    private static String feed(StreamTextFilter filter, String... chunks) {
        StringBuilder seen = new StringBuilder();
        for (String chunk : chunks) {
            seen.append(filter.filter(chunk));
        }
        seen.append(filter.flush());
        return seen.toString();
    }

    @Test
    void redactsEntitySplitAcrossChunks() {
        StreamTextFilter filter = new PiiStreamRedactionHook().replyStreamFilter();
        String seen = feed(filter, "客服电话 138", "0013", "8000 请稍候，正在为您转接人工，", "谢谢");
        assertThat(seen).contains("[PII:CN_PHONE]");
        assertThat(seen).doesNotContain("13800138000");
        // 原文语义字面量保留（只改写命中段）
        assertThat(seen).startsWith("客服电话 ").endsWith("谢谢");
    }

    @Test
    void flushDrainsWindowTail() {
        StreamTextFilter filter = new PiiStreamRedactionHook().replyStreamFilter();
        // 短回复整段滞窗（emitLen = len−window+1 = 0）——只有 flush 才放行
        String first = filter.filter("尾号 13800138000 请查收");
        assertThat(first).isEmpty();
        String flushed = filter.flush();
        assertThat(flushed).isEqualTo("尾号 [PII:CN_PHONE] 请查收");
    }

    @Test
    void noPiiPassesThroughIdenticalAndOrderPreserved() {
        StreamTextFilter filter = new PiiStreamRedactionHook().replyStreamFilter();
        String text = "这是一段不含任何敏感信息的回复文本，订单状态正常，请放心。";
        String seen = feed(filter, text.substring(0, 10), text.substring(10, 20), text.substring(20));
        assertThat(seen).isEqualTo(text);
    }

    @Test
    void emailAndCustomRuleAcrossChunks() {
        CustomPiiRules rules = new CustomPiiRules(
                List.of(CustomPiiRules.Rule.of("ORDER_ID", "ORD-\\d{6}")));
        StreamTextFilter filter = new PiiStreamRedactionHook(null, rules, 128).replyStreamFilter();
        String seen = feed(filter, "邮箱 user", "@example", ".com；订单 ", "ORD-123456 已发货");
        assertThat(seen).contains("[PII:EMAIL]").contains("[PII:ORDER_ID]");
        assertThat(seen).doesNotContain("user@example.com").doesNotContain("ORD-123456");
    }

    @Test
    void placeholderNeverSplitAcrossEmissions() {
        // 小窗口逼出窗中 emit：占位符生成后 emit 边界落在占位符内部时回退到其起点
        StreamTextFilter filter = new PiiStreamRedactionHook(null, null, 12).replyStreamFilter();
        StringBuilder seen = new StringBuilder();
        seen.append(filter.filter("13800138000 "));
        String middle = filter.filter("x");
        seen.append(middle);
        seen.append(filter.flush());
        assertThat(seen.toString()).contains("[PII:CN_PHONE]");
        // 每个已发出的中间片段都不在占位符内部截断（出现的前缀必是完整占位符或无）
        assertThat(middle).satisfiesAnyOf(
                s -> assertThat(s).isEmpty(),
                s -> assertThat(s).doesNotContain("[PII").doesNotContain("PHONE]"));
    }

    @Test
    void windowMustBePositive() {
        assertThatThrownBy(() -> new PiiStreamRedactionHook(null, null, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void filterInstanceIsFreshPerTurn() {
        PiiStreamRedactionHook hook = new PiiStreamRedactionHook();
        assertThat(hook.replyStreamFilter()).isNotSameAs(hook.replyStreamFilter());
    }

    @Test
    void ymlAssemblyParsesReplyRedactionAndWindow() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        GuardModule enabled = GuardModule.fromYml(stores,
                Map.of("pii", Map.of("enabled", true, "reply-redaction", true)));
        assertThat(enabled.configure().hooks())
                .anySatisfy(h -> assertThat(h).isInstanceOf(PiiStreamRedactionHook.class));

        GuardModule absent = GuardModule.fromYml(stores,
                Map.of("pii", Map.of("enabled", true)));
        assertThat(absent.configure().hooks())
                .noneMatch(h -> h instanceof PiiStreamRedactionHook);

        GuardModule customWindow = GuardModule.fromYml(stores,
                Map.of("pii", Map.of("enabled", true, "reply-redaction", true, "reply-window", 24)));
        RuntimeConfig config = customWindow.configure();
        assertThat(config.hooks()).anySatisfy(h -> assertThat(h).isInstanceOf(PiiStreamRedactionHook.class));
    }
}
