package io.github.chyuan_cuihongyuan.buzhou.mcp.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MCP 每连接并发上限 yml 装配测试（spec 628 / T906–T907 / impl 481）：声明绑定进
 * BuzhouMcpProperties 并透传注册表；缺省 null 零变化；非法值 fail-fast。
 */
class McpConcurrencyAssemblyTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(BuzhouMcpAutoConfiguration.class);

    /** yml 声明：属性绑定 + 校验放行。 */
    @Test
    void ymlLimitBinds() {
        runner.withPropertyValues(
                "buzhou.mcp.enabled=true",
                "buzhou.mcp.per-connection-concurrency-limit=2")
                .run(context -> {
                    BuzhouMcpProperties props = context.getBean(BuzhouMcpProperties.class);
                    assertThat(props.perConnectionConcurrencyLimit()).isEqualTo(2);
                });
    }

    /** 缺省：null（不设——零行为变化）。 */
    @Test
    void defaultIsNull() {
        runner.run(context -> {
            BuzhouMcpProperties props = context.getBean(BuzhouMcpProperties.class);
            assertThat(props.perConnectionConcurrencyLimit()).isNull();
            assertThat(props.shutdownBudget()).isEqualTo(Duration.ofSeconds(35)); // 既有缺省不回退
        });
    }

    /** 非法值（0/负）：装配期 fail-fast。 */
    @Test
    void invalidRejected() {
        runner.withPropertyValues("buzhou.mcp.per-connection-concurrency-limit=0")
                .run(context -> assertThat(context).hasFailed());
        runner.withPropertyValues("buzhou.mcp.per-connection-concurrency-limit=-1")
                .run(context -> assertThat(context).hasFailed());
    }
}
