package io.github.chyuan_cuihongyuan.buzhou.starter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.dashboard.config.BuzhouDashboardAutoConfiguration;
import io.github.chyuan_cuihongyuan.buzhou.guard.config.BuzhouGuardAutoConfiguration;
import io.github.chyuan_cuihongyuan.buzhou.mcp.config.BuzhouMcpAutoConfiguration;
import io.github.chyuan_cuihongyuan.buzhou.memory.config.BuzhouMemoryAutoConfiguration;
import io.github.chyuan_cuihongyuan.buzhou.observability.config.BuzhouObservabilityAutoConfiguration;
import io.github.chyuan_cuihongyuan.buzhou.otel.config.BuzhouOtelAutoConfiguration;
import io.github.chyuan_cuihongyuan.buzhou.dashboard.config.DashboardProperties;
import io.github.chyuan_cuihongyuan.buzhou.otel.config.OtelProperties;
import io.github.chyuan_cuihongyuan.buzhou.skill.config.BuzhouSkillsProperties;
import io.github.chyuan_cuihongyuan.buzhou.mcp.config.BuzhouMcpProperties;
import io.github.chyuan_cuihongyuan.buzhou.spill.config.SpillProperties;
import io.github.chyuan_cuihongyuan.buzhou.skill.config.BuzhouSkillsAutoConfiguration;
import io.github.chyuan_cuihongyuan.buzhou.spill.config.BuzhouSpillAutoConfiguration;
import io.github.chyuan_cuihongyuan.buzhou.tools.config.BuzhouToolsAutoConfiguration;
import io.github.chyuan_cuihongyuan.buzhou.store.jdbc.config.BuzhouJdbcStoreAutoConfiguration;
import io.github.chyuan_cuihongyuan.buzhou.store.redis.config.BuzhouRedisStoreAutoConfiguration;
import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreProperties;
import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouBackpressureProperties;
import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouRunawayProperties;
import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouToolsProperties;
import io.github.chyuan_cuihongyuan.buzhou.core.webhook.BuzhouWebhookProperties;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.BuzhouResilienceAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 配置绑定完整性矩阵（T214 / impl-178 / effort#13）：全模块 metadata 键 → 真实装配路径
 * 绑定断言。两类防线：
 * <ol>
 *   <li><b>强断言</b>（@ConfigurationProperties record 模块）：配置全键后反射 accessor 树
 *       断言各键路径值非 null / map 命中 / list 非空——T187 类「yml 键静默不生效」
 *       （缺 @ConstructorBinding）在此必失败；</li>
 *   <li><b>中断言</b>（Environment 直读模块：guard / memory / tools 模块键）：
 *       env.getProperty 等值断言（装配链可达性）。</li>
 * </ol>
 * 新增 metadata 键未纳入本矩阵 → {@code coverage} 断言失败（新键必须补样例）。
 */
class ConfigBindingsMatrixTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ---- 键 → 断言目标（前缀最长匹配；null = env 直读断言） ----

    private static final Map<String, Class<?>> PREFIX_TO_BEAN = Map.ofEntries(
            Map.entry("buzhou.resilience", ResilienceProperties.class),
            Map.entry("buzhou.observe.dashboard", DashboardProperties.class),
            Map.entry("buzhou.observe.otel", OtelProperties.class),
            Map.entry("buzhou.skills", BuzhouSkillsProperties.class),
            Map.entry("buzhou.mcp", BuzhouMcpProperties.class),
            Map.entry("buzhou.spill", SpillProperties.class),
            Map.entry("buzhou.webhook", BuzhouWebhookProperties.class),
            Map.entry("buzhou.runaway", BuzhouRunawayProperties.class),
            Map.entry("buzhou.backpressure", BuzhouBackpressureProperties.class),
            Map.entry("buzhou.tools", BuzhouToolsProperties.class),
            Map.entry("buzhou", BuzhouCoreProperties.class));

    /** env 直读键（guard / memory 模块——无 properties record，装配链走 Environment）。 */
    private static final List<String> ENV_READ_KEYS = List.of(
            "buzhou.guard.enabled", "buzhou.guard.audit.enabled", "buzhou.guard.audit.store",
            "buzhou.guard.audit.in-memory-capacity", "buzhou.guard.audit.signing.min-verify-version",
            "buzhou.guard.policy.enabled", "buzhou.guard.policy.source",
            "buzhou.guard.policy.refresh-interval",
            "buzhou.memory.enabled", "buzhou.memory.embedding-cache-capacity",
            "buzhou.tools.run-command.max-output-bytes",
            "buzhou.leak.level", "buzhou.leak.lease-age-threshold",
            "buzhou.mcp.grace-period", "buzhou.mcp.force-close-timeout", "buzhou.mcp.poll-interval",
            "buzhou.store.jdbc.dialect", "buzhou.store.redis.snapshot-ttl",
            "buzhou.skills.catalog-max-entries", "buzhou.skills.catalog-cache-ttl",
            "buzhou.spill.enabled");

    /** 复杂结构化键（List<KeyFile> 等）——样例值需文件/结构，跳过并显式登记（不静默）。 */
    private static final List<String> SKIPPED_KEYS = List.of(
            "buzhou.guard.audit.signing.keys", // List<KeyFile>：需 PEM 文件，结构化装配面
            "buzhou.guard.audit.signing.key-dir"); // 目录扫描副作用键（防真扫）

    /** 无默认值键的样例（其余按 defaultValue 或类型默认）。 */
    private static final Map<String, String> SAMPLE_OVERRIDES = Map.ofEntries(
            Map.entry("buzhou.resilience.rate-limit.requests-per-minute", "1000"),
            Map.entry("buzhou.resilience.rate-limit.tokens-per-minute", "100000"),
            Map.entry("buzhou.resilience.shadow.models", "shadowModel"),
            Map.entry("buzhou.spill.max-total-bytes", "1048576"),
            Map.entry("buzhou.spill.max-files-per-session", "100"),
            Map.entry("buzhou.observe.dashboard.auth-token", "sample-token"),
            Map.entry("buzhou.observe.otel.headers", "sample-header"),
            Map.entry("buzhou.resilience.fallback.weights", "sample"),
            // effort#15 / spec 55：语义缓存 enabled=true 全路径（矩阵上下文配 stub EmbeddingModel
            // ——见 MatrixStubEmbeddingModel；无 bean 时 fail-fast 由红队测试覆盖）
            Map.entry("buzhou.resilience.semantic-cache.enabled", "true"),
            Map.entry("buzhou.tools.result-limit-overrides", "sampleTool"),
            Map.entry("buzhou.runaway.per-turn.max-steps", "50"),
            Map.entry("buzhou.runaway.per-turn.max-tool-calls", "50"),
            Map.entry("buzhou.runaway.per-turn.wall-clock", "10m"),
            Map.entry("buzhou.runaway.per-session.max-steps", "500"),
            Map.entry("buzhou.runaway.per-session.max-tool-calls", "500"),
            Map.entry("buzhou.backpressure.max-concurrent-sessions", "100"),
            Map.entry("buzhou.backpressure.tool.max-concurrent-per-turn", "10"),
            Map.entry("buzhou.backpressure.tool.tool-timeout", "120s"),
            Map.entry("buzhou.observe.dashboard.enabled", "true"), // 默认关：显式开供 bean 断言（port=0 随机）
            Map.entry("buzhou.observe.otel.enabled", "true"), // 默认关：显式开供 bean 断言（endpoint 不真发）
            Map.entry("buzhou.spill.encryption-key",
                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="), // Base64(32B) 合法 AES-256 测试密钥
            Map.entry("buzhou.guard.audit.store", "memory"),
            Map.entry("buzhou.guard.policy.source", "classpath:buzhou-policy.json"));

    @Test
    void everyMetadataKeyBindsThroughRealAssemblyPath() throws Exception {
        List<String> keys = allMetadataKeys();
        assertThat(keys.size()).isGreaterThanOrEqualTo(85); // 覆盖量下限（新模块键只增不减）

        Map<String, String> properties = new LinkedHashMap<>();
        for (String key : keys) {
            if (SKIPPED_KEYS.contains(key)) {
                continue;
            }
            if (typeOf(key).contains("Map")) {
                continue; // Map 主键不配置（绑定经 <key> 子属性；子键在下方补）
            }
            properties.put(key, sampleValueFor(key, typeOf(key)));
        }
        // map 值键补 hash 子键（Map 键本身配置无效——Spring Boot map 绑定经 <key> 子属性）
        properties.put("buzhou.observe.otel.headers.k", "v");
        properties.put("buzhou.resilience.fallback.weights.m", "3");
        properties.put("buzhou.tools.result-limit-overrides.t", "100");

        java.nio.file.Path spillDir = Files.createTempDirectory("buzhou-matrix-spill");
        ScriptedChatModel model = new ScriptedChatModel();
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        BuzhouCoreAutoConfiguration.class,
                        BuzhouMemoryAutoConfiguration.class,
                        BuzhouSpillAutoConfiguration.class,
                        BuzhouObservabilityAutoConfiguration.class,
                        BuzhouSkillsAutoConfiguration.class,
                        BuzhouMcpAutoConfiguration.class,
                        BuzhouGuardAutoConfiguration.class,
                        BuzhouToolsAutoConfiguration.class,
                        BuzhouOtelAutoConfiguration.class,
                        BuzhouDashboardAutoConfiguration.class,
                        BuzhouJdbcStoreAutoConfiguration.class,
                        BuzhouRedisStoreAutoConfiguration.class,
                        BuzhouResilienceAutoConfiguration.class))
                .withBean(ChatModel.class, () -> model)
                .withBean("shadowModel", ScriptedChatModel.class, ScriptedChatModel::new)
                .withBean(org.springframework.ai.embedding.EmbeddingModel.class,
                        MatrixStubEmbeddingModel::new)
                .withPropertyValues("buzhou.spill.root-dir=" + spillDir)
                .withPropertyValues(properties.entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue()).toArray(String[]::new))
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    List<String> failures = new ArrayList<>();
                    for (String key : keys) {
                        if (SKIPPED_KEYS.contains(key)) {
                            continue;
                        }
                        Class<?> beanClass = ENV_READ_KEYS.contains(key)
                                ? null : resolveBeanClass(key);
                        if (beanClass == null) {
                            // 中断言：env 直读键配置可达
                            String expected = properties.get(key);
                            if (expected != null) {
                                String actual = ctx.getEnvironment().getProperty(key);
                                if (!expected.equals(actual)) {
                                    failures.add(key + " env 值不符：" + actual);
                                }
                            }
                            continue;
                        }
                        Object root = ctx.getBean(beanClass);
                        try {
                            assertBound(root, key, properties);
                        } catch (AssertionError e) {
                            failures.add(e.getMessage());
                        } catch (Exception e) {
                            failures.add(key + " 断言异常：" + e);
                        }
                    }
                    assertThat(failures).as("绑定矩阵失败清单（T187 类静默失效防线）").isEmpty();
                });
    }

    /** 覆盖完整性：登记键真实存在（防 SKIPPED/ENV 登记腐化）；键量下限。 */
    @Test
    void metadataKeyUniverseIsCovered() throws Exception {
        List<String> keys = allMetadataKeys();
        assertThat(keys.size()).isGreaterThanOrEqualTo(85);
        for (String key : SKIPPED_KEYS) {
            assertThat(keys).as("SKIPPED 登记 %s 必须真实存在", key).contains(key);
        }
        for (String key : ENV_READ_KEYS) {
            assertThat(keys).as("ENV 登记 %s 必须真实存在", key).contains(key);
        }
        // 每键必有归属：bean 映射或 env 登记（二选一），无主键即失败
        for (String key : keys) {
            assertThat(SKIPPED_KEYS.contains(key) || ENV_READ_KEYS.contains(key)
                    || resolveBeanClass(key) != null)
                    .as("键 %s 无归属（补 PREFIX_TO_BEAN 前缀或 ENV/SKIPPED 登记）", key)
                    .isTrue();
        }
    }

    // ---- 键断言树（record accessor 链；Map/List 特例） ----

    private void assertBound(Object root, String key, Map<String, String> props) {
        String prefix = longestPrefix(key);
        String[] segments = key.substring(prefix.length() + 1).split("\\.");
        Object current = root;
        for (int i = 0; i < segments.length; i++) {
            String camel = camel(segments[i]);
            Object next = accessor(current, camel);
            if (i == segments.length - 1) {
                assertThat(next).as("%s → %s 末段", key, camel).isNotNull();
                return;
            }
            // 中间段：Map 则下一段为键（原 kebab）
            if (next instanceof Map<?, ?> map) {
                Object value = map.get(segments[i + 1]);
                assertThat(value).as("%s map 键 %s", key, segments[i + 1]).isNotNull();
                return;
            }
            assertThat(next).as("%s 中间段 %s（T187 类静默 null 在此暴露）", key, camel).isNotNull();
            current = next;
        }
    }

    private static Object accessor(Object target, String name) {
        try {
            Method m = target.getClass().getMethod(name);
            return m.invoke(target);
        } catch (NoSuchMethodException e) {
            return null;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String camel(String kebab) {
        StringBuilder sb = new StringBuilder();
        for (String part : kebab.split("-")) {
            if (sb.isEmpty()) {
                sb.append(part);
            } else {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
        }
        return sb.toString();
    }

    private static String longestPrefix(String key) {
        String best = "buzhou";
        for (String p : PREFIX_TO_BEAN.keySet()) {
            if (key.startsWith(p + ".") && p.length() > best.length()) {
                best = p;
            }
        }
        return best;
    }

    private Class<?> resolveBeanClass(String key) {
        return PREFIX_TO_BEAN.get(longestPrefix(key));
    }

    // ---- metadata 解析 ----

    private static List<String> allMetadataKeys() throws Exception {
        List<String> keys = new ArrayList<>();
        var resources = ConfigBindingsMatrixTest.class.getClassLoader()
                .getResources("META-INF/additional-spring-configuration-metadata.json");
        while (resources.hasMoreElements()) {
            var url = resources.nextElement();
            if (!url.toString().contains("buzhou")) {
                continue; // 第三方 metadata（spring-boot 自带等）不入矩阵
            }
            JsonNode root = MAPPER.readTree(url.openStream());
            for (JsonNode p : root.path("properties")) {
                keys.add(p.path("name").asText());
            }
        }
        keys.sort(String::compareTo);
        return keys;
    }

    private static String defaultValueOf(String key) {
        try {
            var resources = ConfigBindingsMatrixTest.class.getClassLoader()
                    .getResources("META-INF/additional-spring-configuration-metadata.json");
            while (resources.hasMoreElements()) {
                JsonNode root = MAPPER.readTree(resources.nextElement().openStream());
                for (JsonNode p : root.path("properties")) {
                    if (key.equals(p.path("name").asText())) {
                        JsonNode def = p.path("defaultValue");
                        if (def.isMissingNode() || def.isNull()) {
                            return null;
                        }
                        if (def.isBoolean()) {
                            return String.valueOf(def.asBoolean());
                        }
                        if (def.isArray() && def.size() > 0) {
                            return def.get(0).asText(); // List 默认取首元素
                        }
                        return def.asText();
                    }
                }
            }
        } catch (Exception ignored) {
            // 首测试已覆盖异常路径
        }
        return null;
    }

    private static String typeOf(String key) {
        try {
            var resources = ConfigBindingsMatrixTest.class.getClassLoader()
                    .getResources("META-INF/additional-spring-configuration-metadata.json");
            while (resources.hasMoreElements()) {
                JsonNode root = MAPPER.readTree(resources.nextElement().openStream());
                for (JsonNode p : root.path("properties")) {
                    if (key.equals(p.path("name").asText())) {
                        return p.path("type").asText("");
                    }
                }
            }
        } catch (Exception ignored) {
            // coverage 断言兜底
        }
        return "";
    }

    private static String sampleValueFor(String key, String type) {
        String override = SAMPLE_OVERRIDES.get(key);
        if (override != null) {
            return override;
        }
        String def = defaultValueOf(key);
        if (def != null && !def.isBlank() && !def.equals("{}") && !def.equals("[]")) {
            return def; // metadata defaultValue 即绑定器可接受形态
        }
        if (type.contains("Duration")) {
            return "1s";
        }
        if (type.contains("Boolean")) {
            return "true";
        }
        if (type.contains("Long")) {
            return "7";
        }
        if (type.contains("Integer")) {
            return "7";
        }
        if (type.contains("Double")) {
            return "1.5";
        }
        if (type.contains("List")) {
            return "RATE_LIMIT";
        }
        if (type.contains("Map")) {
            return "k";
        }
        return "sample";
    }

    /** 矩阵 stub：semantic-cache.enabled=true 全路径装配用（真实嵌入判别力不在此测）。 */
    static final class MatrixStubEmbeddingModel implements org.springframework.ai.embedding.EmbeddingModel {
        @Override
        public org.springframework.ai.embedding.EmbeddingResponse call(
                org.springframework.ai.embedding.EmbeddingRequest request) {
            return new org.springframework.ai.embedding.EmbeddingResponse(java.util.List.of());
        }

        @Override
        public float[] embed(org.springframework.ai.document.Document document) {
            return new float[]{1f, 0f};
        }
    }

}
