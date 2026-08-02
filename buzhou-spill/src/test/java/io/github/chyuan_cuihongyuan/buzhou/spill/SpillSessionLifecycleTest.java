package io.github.chyuan_cuihongyuan.buzhou.spill;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpillSessionLifecycleTest {

    @TempDir
    Path rootDir;

    @Test
    void closeCleansSessionSpills() {
        SpillModule spill = SpillModule.withDefaults(rootDir);
        RuntimeConfig config = spill.configure();
        BuzhouStores stores = Buzhou.inMemoryStores();
        ChatModel model = new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                return new ChatResponse(List.of(new Generation(new AssistantMessage("ok"))));
            }

            @Override
            public Flux<ChatResponse> stream(Prompt prompt) {
                return Flux.just(call(prompt));
            }
        };
        AgentRuntime runtime = Buzhou.runtime(model, stores, config);
        AgentSession session = runtime.spawn("app", "agent", "sess-spill");

        spill.store().store(SpillEntry.of(new SpillUri("agent", "sess-spill", "tc-1"),
                "data"), 2048);
        assertThat(spill.store().exists(new SpillUri("agent", "sess-spill", "tc-1"))).isTrue();

        session.close();

        assertThat(spill.store().exists(new SpillUri("agent", "sess-spill", "tc-1"))).isFalse();
    }
}
