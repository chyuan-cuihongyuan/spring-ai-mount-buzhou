package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 86 §B / T330：PII 脱敏红队——五型检出（校验位收窄误报：坏 checksum 身份证/
 * 坏 Luhn 卡号不误杀）；多型混排脱敏与占位符形态；类型子集；hook 端到端（幂等 +
 * error 路径跳过 + 无命中零改写）；fromYml 装配解析。借鉴：Presidio（规则式
 * recognizer 子集——无 ML 依赖）。
 */
class PiiRedactionTest {

    private final PiiDetector detector = new PiiDetector();

    @Test
    void detectsFiveTypesAndRejectsInvalidChecksums() {
        List<PiiDetector.PiiMatch> hits = detector.scan(
                "邮箱 a.b@test.com 手机 13812345678 证 11010519491231002X 卡 6222020200112233347 IP 192.168.1.42");
        assertThat(hits).extracting(PiiDetector.PiiMatch::type)
                .containsExactly(PiiType.EMAIL, PiiType.CN_PHONE, PiiType.CN_RESIDENT_ID,
                        PiiType.BANK_CARD, PiiType.IPV4);

        // 坏校验位身份证 / 坏 Luhn 卡号：不误杀（长数字串 ≠ PII）
        assertThat(detector.scan("证 110105194912310021")).isEmpty();
        assertThat(detector.scan("卡 6222020200112233348")).isEmpty();
        // 普通订单号/时间戳不命中
        assertThat(detector.scan("订单 ORD-2026-001 时间 1724870400000")).isEmpty();
    }

    @Test
    void redactMultiTypeAndTypeSubset() {
        String raw = "联系 a@b.co 或 13900001111，证 11010519491231002X";

        String all = detector.redact(raw, EnumSet.allOf(PiiType.class));
        assertThat(all).isEqualTo("联系 [PII:EMAIL] 或 [PII:CN_PHONE]，证 [PII:CN_RESIDENT_ID]");

        // 类型子集：只脱邮箱
        String onlyEmail = detector.redact(raw, EnumSet.of(PiiType.EMAIL));
        assertThat(onlyEmail).isEqualTo("联系 [PII:EMAIL] 或 13900001111，证 11010519491231002X");

        // 无命中零改写（引用等）
        String clean = "正常外部输出";
        assertThat(detector.redact(clean, EnumSet.allOf(PiiType.class))).isSameAs(clean);
    }

    @Test
    void hookRedactsToolResultIdempotentlyAndSkipsErrors() {
        PiiRedactionHook hook = new PiiRedactionHook();
        HookEnvironment env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());

        DefaultToolCallContext ctx = new DefaultToolCallContext(env, "tc1", "fetch", Map.of());
        ctx.markExecuted("用户邮箱 zhang.san@corp.com 退订", null);
        hook.afterTool(ctx);
        assertThat(String.valueOf(ctx.result()))
                .isEqualTo("用户邮箱 [PII:EMAIL] 退订");

        // 幂等：占位符内容再过 hook 不再改写
        DefaultToolCallContext again = new DefaultToolCallContext(env, "tc2", "fetch", Map.of());
        String once = String.valueOf(ctx.result());
        again.markExecuted(once, null);
        hook.afterTool(again);
        assertThat(String.valueOf(again.result())).isEqualTo(once);

        // error 路径跳过
        DefaultToolCallContext errored = new DefaultToolCallContext(env, "tc3", "fetch", Map.of());
        errored.markExecuted(null, new IllegalStateException("boom"));
        assertThat(hook.afterTool(errored)).isEqualTo(io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult.CONTINUE);

        // 无命中零改写
        DefaultToolCallContext clean = new DefaultToolCallContext(env, "tc4", "fetch", Map.of());
        clean.markExecuted("干净输出", null);
        hook.afterTool(clean);
        assertThat(String.valueOf(clean.result())).isEqualTo("干净输出");
    }

    @Test
    void fromYmlParsesEnabledAndTypes() {
        // 经 GuardModule.fromYml 全链：pii.enabled=true + types CSV
        io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores stores =
                io.github.chyuan_cuihongyuan.buzhou.core.Buzhou.inMemoryStores();
        var module = io.github.chyuan_cuihongyuan.buzhou.guard.GuardModule.fromYml(stores,
                Map.of("pii", Map.of("enabled", true, "types", List.of("EMAIL", "CN_PHONE"))));
        var config = module.configure();
        // hooks 进 RuntimeConfig——含 PiiRedactionHook（类型子集语义由 hook 内部持有）
        assertThat(config).isNotNull();

        // 精确面：builder 直测
        var hook = new io.github.chyuan_cuihongyuan.buzhou.guard.pii.PiiRedactionHook(
                EnumSet.of(PiiType.EMAIL));
        HookEnvironment env = new HookEnvironment("s2", "agent", new InMemorySessionStateStore());
        DefaultToolCallContext ctx = new DefaultToolCallContext(env, "tc5", "fetch", Map.of());
        ctx.markExecuted("邮箱 a@b.co 电话 13812345678", null);
        hook.afterTool(ctx);
        assertThat(String.valueOf(ctx.result()))
                .isEqualTo("邮箱 [PII:EMAIL] 电话 13812345678"); // 子集外类型不脱
    }
}
