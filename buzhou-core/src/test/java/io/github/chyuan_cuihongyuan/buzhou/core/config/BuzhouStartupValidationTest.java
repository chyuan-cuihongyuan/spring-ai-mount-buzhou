package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-42 / spec 13 §T68 启动校验：store.type 拼错/模块缺席启动即失败（带指引）、
 * FailureAnalyzer 翻译为 description + action、越界配置被拒、
 * 默认值迁移不破坏既有契约。
 */
class BuzhouStartupValidationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouCoreAutoConfiguration.class));

    /** 摊平异常链全文（绑定失败的具体原因在链尾）。 */
    private static String chainText(Throwable failure) {
        StringBuilder out = new StringBuilder();
        Throwable current = failure;
        for (int depth = 0; current != null && depth < 12; depth++) {
            out.append(current.getMessage()).append(" <- ");
            current = current.getCause();
        }
        return out.toString();
    }

    @Test
    void misspelledStoreTypeFailsFastWithGuidance() {
        runner.withPropertyValues("buzhou.store.type=jbdc").run(ctx -> {
            assertThat(ctx).hasFailed();
            assertThat(ctx.getStartupFailure()).isInstanceOf(BeanCreationException.class);
            String chain = chainText(ctx.getStartupFailure());
            assertThat(chain).contains("jbdc").contains("不是有效存储形态")
                    .contains("memory").contains("jdbc").contains("redis");
        });
    }

    @Test
    void storeTypeWithoutImplementationFailsWithDependencyHint() {
        runner.withPropertyValues("buzhou.store.type=jdbc").run(ctx -> {
            assertThat(ctx).hasFailed();
            String chain = chainText(ctx.getStartupFailure());
            assertThat(chain).contains("对应 store 实现未装配")
                    .contains("buzhou-store-jdbc");
        });
    }

    @Test
    void validStoreTypeStillBoots() {
        runner.withPropertyValues("buzhou.store.type=memory").run(ctx ->
                assertThat(ctx).hasNotFailed());
    }

    @Test
    void failureAnalyzerTranslatesToDescriptionAndAction() {
        BuzhouConfigurationException exception = new BuzhouConfigurationException(
                "buzhou.store.type=\"oops\" 不是有效存储形态",
                "修正为 memory / jdbc / redis 之一");
        BeanCreationException wrapped = new BeanCreationException("buzhouStoresGuard", "fail",
                exception);
        FailureAnalysis analysis = new BuzhouStoreFailureAnalyzer().analyze(wrapped);
        assertThat(analysis).isNotNull();
        assertThat(analysis.getDescription()).contains("不是有效存储形态");
        assertThat(analysis.getAction()).contains("memory");
        // 无关失败返回 null（走默认诊断）
        assertThat(new BuzhouStoreFailureAnalyzer()
                .analyze(new IllegalStateException("unrelated"))).isNull();
    }

    @Test
    void negativeEventDispatchCapacityRejectedAtBinding() {
        runner.withPropertyValues("buzhou.core.event-dispatch.capacity=-8").run(ctx -> {
            assertThat(ctx).hasFailed();
            String chain = chainText(ctx.getStartupFailure());
            assertThat(chain).contains("capacity").contains("-8");
        });
    }
}
