package io.github.chyuan_cuihongyuan.buzhou.examples.boot;

import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-55：starter + application.yml 全上下文冒烟——Boot 真实刷新（含 SmartLifecycle 停机段、
 * FailureAnalyzer 装配、application.yml 绑定），两轮对话跑通即验收。
 */
class StarterBootDemoTest {

    @Test
    void starterBootContextRunsTwoTurns() {
        try (var context = new SpringApplicationBuilder(StarterBootDemo.class)
                .properties("spring.main.web-application-type=none")
                .run()) {
            AgentRuntime runtime = context.getBean(AgentRuntime.class);
            try (AgentSession session = runtime.spawn("demo-app", "starter-demo", "boot-test")) {
                String first = session.chat("介绍一下你自己");
                String second = session.chat("再演示一轮");
                assertThat(first).contains("starter");
                assertThat(second).contains("第二轮");
            }
        } // context close：SmartLifecycle 优雅停机排空不抛即通过
    }
}
