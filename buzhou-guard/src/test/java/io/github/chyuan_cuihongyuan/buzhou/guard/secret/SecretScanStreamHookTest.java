package io.github.chyuan_cuihongyuan.buzhou.guard.secret;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.StreamTextFilter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 536 / T825：流式回复秘密扫描——跨 chunk AWS 密钥合并占位符化、
 * flush 排空窗尾、无秘密恒等、yml secrets.stream-redaction 装配/缺席。
 * （500 StreamTextFilter SPI 第二消费者——组合性证明。）
 */
class SecretScanStreamHookTest {

    private static String feed(StreamTextFilter filter, String... chunks) {
        StringBuilder seen = new StringBuilder();
        for (String chunk : chunks) {
            seen.append(filter.filter(chunk));
        }
        seen.append(filter.flush());
        return seen.toString();
    }

    private static final String AWS_KEY = "AKIAIOSFODNN7EXAMPLE";

    @Test
    void secretSplitAcrossChunksIsRedacted() {
        StreamTextFilter filter = new SecretScanStreamHook().replyStreamFilter();
        String seen = feed(filter, "密钥 ", "AKIA", "IOSFODNN7", "EXAMPLE 请轮换");
        assertThat(seen).contains("[SECRET:");
        assertThat(seen).doesNotContain(AWS_KEY);
    }

    @Test
    void flushDrainsWindowTail() {
        StreamTextFilter filter = new SecretScanStreamHook().replyStreamFilter();
        String first = filter.filter("sk-" + "a".repeat(40));
        assertThat(first).isEmpty(); // 短回复整段滞窗
        assertThat(filter.flush()).contains("[SECRET:");
    }

    @Test
    void cleanTextPassesThroughIdentical() {
        StreamTextFilter filter = new SecretScanStreamHook().replyStreamFilter();
        String text = "这是一段不含任何密钥的正常回复，订单状态正常。";
        String seen = feed(filter, text.substring(0, 12), text.substring(12));
        assertThat(seen).isEqualTo(text);
    }

    @Test
    void ymlAssemblyParsesSecretsStreamRedaction() {
        var stores = io.github.chyuan_cuihongyuan.buzhou.core.Buzhou.inMemoryStores();
        var enabled = GuardModuleHelper.module(Map.of(
                "secrets", Map.of("enabled", true, "stream-redaction", true)));
        assertThat(enabled.configure().hooks())
                .anySatisfy(h -> assertThat(h)
                        .isInstanceOf(io.github.chyuan_cuihongyuan.buzhou.guard.secret.SecretScanStreamHook.class));
        var absent = GuardModuleHelper.module(Map.of(
                "secrets", Map.of("enabled", true)));
        assertThat(absent.configure().hooks())
                .noneMatch(h -> h instanceof io.github.chyuan_cuihongyuan.buzhou.guard.secret.SecretScanStreamHook);
    }

    /** GuardModule 构造助手（测试内联——避免 import 链）。 */
    private static final class GuardModuleHelper {
        static io.github.chyuan_cuihongyuan.buzhou.guard.GuardModule module(
                java.util.Map<String, Object> yml) {
            return io.github.chyuan_cuihongyuan.buzhou.guard.GuardModule.fromYml(
                    io.github.chyuan_cuihongyuan.buzhou.core.Buzhou.inMemoryStores(), yml);
        }
    }
}
