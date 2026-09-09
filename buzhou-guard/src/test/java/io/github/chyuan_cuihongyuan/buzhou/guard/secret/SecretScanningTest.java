package io.github.chyuan_cuihongyuan.buzhou.guard.secret;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultTurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.guard.GuardModule;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 400 §Testing / T691–T692：密钥扫描——检测器 7 型正/负样本 + 幂等 +
 * 引用等；hook 三缝 MASK（输入/出站参数/结果）；builder/yml 装配（默认关、
 * 类型子集）。
 */
class SecretScanningTest {

    private HookEnvironment env() {
        return new HookEnvironment("s1", "agent", new InMemorySessionStateStore());
    }

    @Test
    void shouldDetectAndRedactAllSevenTypes_whenPresent() {
        SecretScanner scanner = new SecretScanner();
        String mixed = "aws AKIAIOSFODNN7EXAMPLE gh ghp_"
                + "16CharactersTOKEN1234567890 google AIza"
                + "SyA1bC2dE3fG4hI5jK6lM7nO8pQ9rS0tU1v"
                + " slack xoxb-123456789012-abcdefghijkl"
                + " sk-proj-abcdefghijklmnopqrstuvwxy0123456789"
                + " eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.abcde-_fGhI"
                + " -----BEGIN RSA PRIVATE KEY-----";
        var hits = scanner.scan(mixed);
        assertThat(hits).extracting(SecretScanner.SecretMatch::type).doesNotHaveDuplicates();
        assertThat(hits).hasSize(7);
        String redacted = scanner.redact(mixed);
        assertThat(redacted).contains("[SECRET:AWS_ACCESS_KEY]")
                .contains("[SECRET:GITHUB_TOKEN]")
                .contains("[SECRET:GOOGLE_API_KEY]")
                .contains("[SECRET:SLACK_TOKEN]")
                .contains("[SECRET:OPENAI_STYLE_KEY]")
                .contains("[SECRET:JWT]")
                .contains("[SECRET:PRIVATE_KEY_BLOCK]");
        assertThat(redacted).doesNotContain("AKIA").doesNotContain("ghp_")
                .doesNotContain("AIza").doesNotContain("xoxb-");

        // 负样本：普通词不误伤（sk- 短词 / 普通句子 / 占位幂等）
        String clean = "ask-回问 skill-based 与 sk-short 词都不命中";
        assertThat(scanner.scan(clean)).isEmpty();
        assertThat(scanner.redact(clean)).isSameAs(clean);
        String once = scanner.redact(mixed);
        assertThat(scanner.redact(once)).isSameAs(once); // 幂等
    }

    @Test
    void shouldMaskThreeSeams_whenHookFires() {
        SecretScanHook hook = new SecretScanHook();

        DefaultTurnContext turn = new DefaultTurnContext(env(),
                "我的 key 是 AKIAIOSFODNN7EXAMPLE 别外传");
        hook.beforeTurn(turn);
        assertThat(turn.input()).isEqualTo("我的 key 是 [SECRET:AWS_ACCESS_KEY] 别外传");

        DefaultToolCallContext outbound = new DefaultToolCallContext(env(), "c1", "http_post",
                Map.of("url", "https://x.io", "body", "token=ghp_16CharactersTOKEN1234567890"));
        hook.beforeTool(outbound);
        assertThat(outbound.arguments().get("body"))
                .isEqualTo("token=[SECRET:GITHUB_TOKEN]");
        assertThat(outbound.arguments().get("url")).isEqualTo("https://x.io");

        DefaultToolCallContext inbound = new DefaultToolCallContext(env(), "c2", "read_file",
                Map.of("path", "a.env"));
        inbound.replaceResult("export KEY=\"-----BEGIN OPENSSH PRIVATE KEY-----\"");
        hook.afterTool(inbound);
        assertThat(String.valueOf(inbound.result()))
                .isEqualTo("export KEY=\"[SECRET:PRIVATE_KEY_BLOCK]\"");

        // 无命中零改写（引用等）
        DefaultTurnContext clean = new DefaultTurnContext(env(), "正常提问");
        hook.beforeTurn(clean);
        assertThat(clean.input()).isEqualTo("正常提问");
    }

    @Test
    void shouldAssembleFromBuilderAndYml_whenDeclared() {
        // builder 程序面：类型子集
        GuardModule byBuilder = GuardModule.builder(Buzhou.inMemoryStores())
                .secretScanning(EnumSet.of(SecretType.JWT)).build();
        SecretScanHook subset = byBuilder.configure().hooks().stream()
                .filter(h -> h instanceof SecretScanHook)
                .map(h -> (SecretScanHook) h)
                .findFirst().orElseThrow();
        DefaultTurnContext jwtTurn = new DefaultTurnContext(env(),
                "看这个 eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.abcde-_fGhI");
        subset.beforeTurn(jwtTurn);
        assertThat(jwtTurn.input()).contains("[SECRET:JWT]");
        DefaultTurnContext awsTurn = new DefaultTurnContext(env(),
                "aws AKIAIOSFODNN7EXAMPLE");
        subset.beforeTurn(awsTurn);
        assertThat(awsTurn.input()).contains("AKIA"); // 子集外不动

        // yml：enabled + types（CSV 形态）
        Map<String, Object> yml = new LinkedHashMap<>();
        yml.put("secrets", Map.of("enabled", true, "types", "aws_access_key, jwt"));
        GuardModule byYml = GuardModule.fromYml(Buzhou.inMemoryStores(), yml);
        SecretScanHook ymlHook = byYml.configure().hooks().stream()
                .filter(h -> h instanceof SecretScanHook)
                .map(h -> (SecretScanHook) h)
                .findFirst().orElseThrow();
        DefaultTurnContext ymlTurn = new DefaultTurnContext(env(),
                "aws AKIAIOSFODNN7EXAMPLE 与 slack xoxb-123456789012-abcdefghijkl");
        ymlHook.beforeTurn(ymlTurn);
        assertThat(ymlTurn.input()).contains("[SECRET:AWS_ACCESS_KEY]").contains("xoxb-");

        // 默认关：未声明 secrets 时不挂 hook
        GuardModule off = GuardModule.fromYml(Buzhou.inMemoryStores(), Map.of());
        assertThat(off.configure().hooks().stream()
                .noneMatch(h -> h instanceof SecretScanHook)).isTrue();
    }
}
