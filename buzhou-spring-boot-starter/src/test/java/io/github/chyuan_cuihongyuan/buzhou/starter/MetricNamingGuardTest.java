package io.github.chyuan_cuihongyuan.buzhou.starter;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 指标命名规范守卫（spec 715 / T981，prometheus/client_java naming conventions
 * 守卫测试借鉴——边界守卫同款源码扫描范式，零新依赖）：双正则提取指标名候选
 * （直接调用点 + METRIC、COUNTER、TIMER 前缀常量赋值——调用点上下文天然排除
 * 配置键/日志文案），断言全部符合 {@code ^buzhou(\.[a-z][a-z0-9-]*)+$}（点
 * 分隔、段小写、段内数字与连字符合法）。
 */
class MetricNamingGuardTest {

    private static final Path REPO_ROOT = Path.of("..").toAbsolutePath().normalize();

    /** 命名规则：buzhou + 点分隔小写段（段内数字/连字符合法）。 */
    static final Pattern NAME_RULE = Pattern.compile("^buzhou(\\.[a-z][a-z0-9-]*)+$");

    /** 提取正则 ①：直接调用点 .counter("… / .timer("…。 */
    private static final Pattern CALL_SITE = Pattern.compile(
            "\\.(counter|timer)\\(\\s*\"(buzhou\\.[^\"]+)\"");
    /** 提取正则 ②：指标名常量赋值（String 后跟 METRIC、COUNTER 或 TIMER 前缀标识符）。 */
    private static final Pattern CONSTANT = Pattern.compile(
            "String\\s+(?:METRIC|COUNTER|TIMER)[A-Z_]*\\s*=\\s*\"(buzhou\\.[^\"]+)\"");

    @Test
    void nameRuleJudgement() {
        assertThat(NAME_RULE.matcher("buzhou.tool.calls").matches()).isTrue();
        assertThat(NAME_RULE.matcher("buzhou.archive.pdb-rejected").matches()).isTrue();
        assertThat(NAME_RULE.matcher("buzhou.bulkhead.scaling.scale-up").matches()).isTrue();
        assertThat(NAME_RULE.matcher("buzhou.compaction").matches()).isTrue();

        assertThat(NAME_RULE.matcher("buzhou.Tool.Calls").matches()).isFalse();     // 大写段
        assertThat(NAME_RULE.matcher("buzhou.tool.calls ").matches()).isFalse();    // 尾空格
        assertThat(NAME_RULE.matcher("buzhou.tool..calls").matches()).isFalse();    // 双点
        assertThat(NAME_RULE.matcher("buzhou.tool_Calls").matches()).isFalse();     // 下划线
        assertThat(NAME_RULE.matcher("buzhou.").matches()).isFalse();               // 空段
        assertThat(NAME_RULE.matcher("buzhou.9lives").matches()).isFalse();         // 段首数字
    }

    @Test
    void extractorPicksCallSitesAndConstantsSkipsNoise() {
        String snippet = """
                // 指标
                metrics.counter("buzhou.tool.calls", "outcome", "ok");
                metrics.timer("buzhou.turn.duration", Duration.ZERO);
                static final String METRIC_ROTATED = "buzhou.jsonl.rotated";
                static final String COUNTER_FALLBACK = "buzhou.jsonl.rotate-failed";
                // 干扰项：配置键与文案——不是指标名
                log.warn("buzhou.alert.rules[].name 非法（rule={})");
                String key = "buzhou.bulkhead.scaling.max-multiplier";
                env.getProperty("buzhou.leak.level", "SIMPLE");
                """;
        List<String> names = extractNames(snippet);
        Map<String, List<String>> found = Map.of("synthetic", names);

        assertThat(names).containsExactlyInAnyOrder(
                "buzhou.tool.calls", "buzhou.turn.duration",
                "buzhou.jsonl.rotated", "buzhou.jsonl.rotate-failed");
        assertThat(judge(found)).isEmpty(); // 合法名全过
    }

    @Test
    void extractorFlagsIllegalNames() {
        String snippet = """
                metrics.counter("buzhou.Bad.Name");
                """;
        Map<String, String> violations = judge(
                Map.of("synthetic", List.of(snippet)));
        assertThat(violations).containsKey("synthetic");
        assertThat(violations.get("synthetic")).contains("buzhou.Bad.Name");
    }

    @Test
    void allRepoMetricNamesConform() throws IOException {
        assumeTrue(Files.isDirectory(REPO_ROOT.resolve("buzhou-core/src/main/java")),
                "非仓库布局（找不到 buzhou-core/src/main/java）——扫描跳过");

        Map<String, List<String>> perModule = new LinkedHashMap<>();
        try (Stream<Path> mods = Files.list(REPO_ROOT)) {
            mods.filter(p -> p.getFileName().toString().startsWith("buzhou-"))
                    .forEach(mod -> scanModule(mod, perModule));
        }
        assertThat(perModule).isNotEmpty(); // 扫描面非空（自证）

        Map<String, String> violations = judge(perModule);
        assertThat(violations)
                .as("指标命名违规——命名规则 ^buzhou(\\.[a-z][a-z0-9-]*)+$ "
                        + "（点分隔、段小写；prometheus/micrometer 惯例）")
                .isEmpty();
    }

    /** 判定：返回 模块 → 违规名（含出处文件）清单；全合规 = 空表。 */
    private static Map<String, String> judge(Map<String, List<String>> perModule) {
        Map<String, String> out = new LinkedHashMap<>();
        perModule.forEach((module, names) -> names.forEach(name -> {
            if (!NAME_RULE.matcher(name).matches()) {
                out.put(module, name);
            }
        }));
        return out;
    }

    /** 单源码片段提取的全部指标名（调用点 + 常量，去重保序）。 */
    private static List<String> extractNames(String content) {
        List<String> out = new ArrayList<>();
        Matcher call = CALL_SITE.matcher(content);
        while (call.find()) {
            out.add(call.group(2));
        }
        Matcher constant = CONSTANT.matcher(content);
        while (constant.find()) {
            out.add(constant.group(1));
        }
        return out.stream().distinct().toList();
    }

    private static void scanModule(Path module, Map<String, List<String>> out) {
        String name = module.getFileName().toString();
        Path src = module.resolve("src/main/java");
        if (!Files.isDirectory(src)) {
            return;
        }
        List<String> names = out.computeIfAbsent(name, k -> new ArrayList<>());
        try (Stream<Path> files = Files.walk(src)) {
            files.filter(p -> p.toString().endsWith(".java")).forEach(file -> {
                try {
                    names.addAll(extractNames(Files.readString(file)));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
