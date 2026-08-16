package io.github.chyuan_cuihongyuan.buzhou.starter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.InputStream;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-52 / spec 14 §H：配置元数据生效防回退断言 + starter 反向用例。
 *
 * <p>元数据防回退：configuration-processor 生成的
 * {@code META-INF/spring-configuration-metadata.json} 必须随各模块 jar 发布（此前
 * additional-*.json 是死文件）。经 test classpath 的 jar 逐一断言关键键存在——
 * processor 被移除或 additional-json 再写坏时此测试红。
 */
class BuzhouStarterHardeningTest {

    /** 各发布 jar 的元数据必须包含本模块关键键（防 processor 回退）。 */
    @Test
    void configurationMetadataPublishedInModuleJars() throws Exception {
        String classpath = System.getProperty("surefire.test.class.path",
                System.getProperty("java.class.path"));
        assertKey(classpath, "buzhou-core", "buzhou.store.type");
        assertKey(classpath, "buzhou-resilience", "buzhou.resilience.max-attempts");
        assertKey(classpath, "buzhou-observability", "buzhou.observability.batch-size");
        assertKey(classpath, "buzhou-observe-otel", "buzhou.observe.otel.exporter-mode");
        assertKey(classpath, "buzhou-observe-dashboard", "buzhou.observe.dashboard.bind-address");
        assertKey(classpath, "buzhou-mcp", "buzhou.mcp.dangerous-tool-patterns");
        assertKey(classpath, "buzhou-spill", "buzhou.spill.enabled");
    }

    /** memory.enabled=false：memory 模块退场，runtime 仍可装配（降级路径不误伤）。 */
    @Test
    void memoryDisabledDegradesGracefully() {
        fullStackRunner("buzhou.memory.enabled=false")
                .run(context -> assertThat(context).hasNotFailed());
    }

    /** resilience.enabled=false：回退底座原生行为（无韧性 advisor）。 */
    @Test
    void resilienceDisabledFallsBackToVanilla() {
        fullStackRunner("buzhou.resilience.enabled=false")
                .run(context -> assertThat(context).hasNotFailed());
    }

    /** store.type=redis 但缺 Redis 连接：启动失败（fail-fast，而非深水区运行时炸）。 */
    @Test
    void redisStoreTypeWithoutRedisFailsFast() {
        fullStackRunner("buzhou.store.type=redis")
                .run(context -> assertThat(context).hasFailed());
    }

    private static ApplicationContextRunner fullStackRunner(String... props) {
        java.nio.file.Path spillDir;
        try {
            spillDir = java.nio.file.Files.createTempDirectory("buzhou-starter-hardening");
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        return new ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class,
                        io.github.chyuan_cuihongyuan.buzhou.memory.config.BuzhouMemoryAutoConfiguration.class,
                        io.github.chyuan_cuihongyuan.buzhou.spill.config.BuzhouSpillAutoConfiguration.class,
                        io.github.chyuan_cuihongyuan.buzhou.observability.config.BuzhouObservabilityAutoConfiguration.class,
                        io.github.chyuan_cuihongyuan.buzhou.skill.config.BuzhouSkillsAutoConfiguration.class,
                        io.github.chyuan_cuihongyuan.buzhou.mcp.config.BuzhouMcpAutoConfiguration.class,
                        io.github.chyuan_cuihongyuan.buzhou.guard.config.BuzhouGuardAutoConfiguration.class,
                        io.github.chyuan_cuihongyuan.buzhou.tools.config.BuzhouToolsAutoConfiguration.class,
                        io.github.chyuan_cuihongyuan.buzhou.otel.config.BuzhouOtelAutoConfiguration.class,
                        io.github.chyuan_cuihongyuan.buzhou.dashboard.config.BuzhouDashboardAutoConfiguration.class,
                        io.github.chyuan_cuihongyuan.buzhou.store.jdbc.config.BuzhouJdbcStoreAutoConfiguration.class,
                        io.github.chyuan_cuihongyuan.buzhou.store.redis.config.BuzhouRedisStoreAutoConfiguration.class,
                        io.github.chyuan_cuihongyuan.buzhou.resilience.config.BuzhouResilienceAutoConfiguration.class))
                .withBean(org.springframework.ai.chat.model.ChatModel.class,
                        io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel::new)
                .withPropertyValues(
                        java.util.Arrays.copyOf(props, props.length))
                .withPropertyValues(
                        "buzhou.spill.root-dir=" + spillDir,
                        "buzhou.spill.sandbox-root=" + spillDir);
    }

    private static void assertKey(String classpath, String jarNameFragment, String key)
            throws Exception {
        // T214：-am 联编（reactor）时依赖是 classes 目录而非 jar——退化为目录内 additional
        // 元数据断言（processor 生成面在 packaged verify 全流程另有覆盖；两条路都断关键键）。
        String jarPath = java.util.Arrays.stream(classpath.split(":"))
                .filter(p -> p.contains(jarNameFragment) && p.endsWith(".jar"))
                .findFirst()
                .orElse(null);
        if (jarPath != null) {
            try (JarFile jar = new JarFile(jarPath)) {
                ZipEntry entry = jar.getEntry("META-INF/spring-configuration-metadata.json");
                assertThat(entry).as(jarNameFragment + " 缺元数据文件（processor 回退？）").isNotNull();
                try (InputStream in = jar.getInputStream(entry)) {
                    String json = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    assertThat(json).as(jarNameFragment + " 元数据缺键 " + key).contains(key);
                }
            }
            return;
        }
        String classesDir = java.util.Arrays.stream(classpath.split(":"))
                .filter(p -> p.contains(jarNameFragment) && p.endsWith("/classes"))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        jarNameFragment + " jar/classes 未在 test classpath：" + key + " 断言无法执行"));
        // 与 jar 内同源：processor 在 compile 期生成到 target/classes/META-INF/
        java.nio.file.Path generated = java.nio.file.Path.of(classesDir,
                "META-INF", "spring-configuration-metadata.json");
        assertThat(generated).as(jarNameFragment + " 缺生成元数据（processor 回退？）").exists();
        String json = java.nio.file.Files.readString(generated);
        assertThat(json).as(jarNameFragment + " 元数据缺键 " + key).contains(key);
    }
}
