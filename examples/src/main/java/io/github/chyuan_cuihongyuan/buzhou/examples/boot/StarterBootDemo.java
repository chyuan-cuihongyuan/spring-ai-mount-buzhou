package io.github.chyuan_cuihongyuan.buzhou.examples.boot;

import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * impl-55 / spec 14 §K：基于 starter + application.yml 的可启动 Boot 示例。
 *
 * <p>零 API key：内置 {@link StubChatModel}（固定剧本）；引 {@code buzhou-spring-boot-starter}
 * 后全部机制自装配。跑法：
 * <pre>
 *   ./mvnw -pl examples -am package -DskipTests
 *   java -jar examples/target/*.jar   # 或 IDE 直跑 main
 * </pre>
 * 输出多轮对话 + 关闭即优雅停机（SmartLifecycle 排空）。
 */
@SpringBootApplication
public class StarterBootDemo {

    /** 零依赖替身模型：两轮固定回复（无工具循环，纯演示装配链路与配置生效）。 */
    static class StubChatModel implements ChatModel {
        private int calls;

        @Override
        public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
            return org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build();
        }

        @Override
        public org.springframework.ai.chat.model.ChatResponse call(
                org.springframework.ai.chat.prompt.Prompt prompt) {
            calls++;
            String text = calls == 1
                    ? "你好！我是由 Buzhou starter 装配的 agent（配置见 application.yml）。"
                    : "第二轮对话正常；现在关闭应用体会优雅停机排空。";
            return new org.springframework.ai.chat.model.ChatResponse(java.util.List.of(
                    new org.springframework.ai.chat.model.Generation(
                            new org.springframework.ai.chat.messages.AssistantMessage(text))));
        }
    }

    @Bean
    ChatModel chatModel() {
        return new StubChatModel();
    }

    public static void main(String[] args) {
        try (ConfigurableApplicationContext context = SpringApplication.run(StarterBootDemo.class, args)) {
            AgentRuntime runtime = context.getBean(AgentRuntime.class);
            try (AgentSession session = runtime.spawn("demo-app", "starter-demo", "starter-boot-demo")) {
                System.out.println("[turn1] " + session.chat("介绍一下你自己"));
                System.out.println("[turn2] " + session.chat("再演示一轮"));
            }
            // try-with-resources 关闭 context：SmartLifecycle 分 phase 优雅停机（拒绝新 Turn → 排空 → 关停）
        }
    }
}
