package io.github.chyuan_cuihongyuan.buzhou.examples.demo;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.BuzhouChatMemory;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.session.DefaultAgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.session.DefaultAgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.session.HarnessAssembler;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MessageStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ReadDegradeHolder;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ReadDegradePolicy;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.FakeChatModel;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptStep;
import io.github.chyuan_cuihongyuan.buzhou.spill.DiskSpillStore;
import io.github.chyuan_cuihongyuan.buzhou.spill.SpillCipher;
import io.github.chyuan_cuihongyuan.buzhou.spill.SpillEntry;
import io.github.chyuan_cuihongyuan.buzhou.spill.SpillQuota;
import io.github.chyuan_cuihongyuan.buzhou.spill.SpillUri;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * effort #9 新能力演示（T164 / impl-135）：加密开关行为、单飞并发快速失败、
 * 读降级空历史续聊——examples 接缝文档（用户可读场景）。
 */
class Effort9CapabilitiesDemoTest {

    @TempDir
    Path spillRoot;

    @AfterEach
    void resetPolicy() {
        ReadDegradeHolder.set(ReadDegradePolicy.OFF);
    }

    /** 1) 加密开关：同一份秘密内容——未配密钥明文落盘；配钥后密文落盘且读回透明。 */
    @Test
    void encryptionOnOffBehavior() throws Exception {
        String secret = "部署口令 sk-live-1234567890";
        SpillUri uriOn = new SpillUri("agent", "demo", "tc-on");
        SpillUri uriOff = new SpillUri("agent", "demo", "tc-off");

        // 开关关（缺省）：明文在盘
        new DiskSpillStore(spillRoot).store(SpillEntry.of(uriOff, secret), 128);
        Path plain = spillRoot.resolve("agent").resolve("demo").resolve("tc-off.spill");
        assertThat(Files.readString(plain)).contains(secret);

        // 开关开：磁盘只见密文（魔法行），读回透明
        byte[] key = new byte[32];
        ThreadLocalRandom.current().nextBytes(key);
        DiskSpillStore encrypted = new DiskSpillStore(spillRoot, SpillQuota.unbounded(),
                SpillCipher.fromBase64Key(Base64.getEncoder().encodeToString(key)));
        encrypted.store(SpillEntry.of(uriOn, secret), 128);
        Path cipher = spillRoot.resolve("agent").resolve("demo").resolve("tc-on.spill");
        assertThat(Files.readString(cipher)).startsWith(SpillCipher.MAGIC).doesNotContain(secret);
        assertThat(encrypted.load(uriOn)).contains(secret);
        // 旧明文文件升级后仍可读
        assertThat(encrypted.load(uriOff)).contains(secret);
    }

    /** 2) 单飞闸：同会话并发第二轮快速失败（结构化错误码），终结后可续。 */
    @Test
    void singleFlightConcurrentSecondCallFailsFast() throws Exception {
        CountDownLatch toolEntered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        FakeChatModel model = FakeChatModel.script(
                ScriptStep.toolCall("slow_lookup", "{}"),
                ScriptStep.text("首轮答复"));
        ToolCallback slow = new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("slow_lookup")
                        .description("slow lookup").inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                toolEntered.countDown();
                try {
                    release.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "lookup-done";
            }
        };
        BuzhouStores stores = Buzhou.inMemoryStores();
        DefaultAgentRuntime runtime = new DefaultAgentRuntime(model, stores,
                new HarnessAssembler(), RuntimeConfig.defaults(), null, null,
                Duration.ofSeconds(30), slow);
        AgentSession session = runtime.spawn("app", "agent", "demo-singleflight");

        CompletableFuture<String> first =
                CompletableFuture.supplyAsync(() -> session.chat("查一下"));
        assertThat(toolEntered.await(5, TimeUnit.SECONDS)).isTrue();

        // 误用立即暴露：结构化 TURN_IN_FLIGHT（不排队、不静默交错）
        assertThatThrownBy(() -> session.chat("插队提问"))
                .isInstanceOf(BuzhouException.class)
                .hasMessageContaining("单飞闸");

        release.countDown();
        assertThat(first.get(10, TimeUnit.SECONDS)).isEqualTo("首轮答复");
        assertThat(((DefaultAgentSession) session).inFlightTurns()).isZero();
        runtime.close();
    }

    /** 3) 读降级续聊：EMPTY 策略下历史读失败→空历史继续（新消息照常落盘）。 */
    @Test
    void readDegradeKeepsSessionUsable() {
        FlakyStore flaky = new FlakyStore();
        BuzhouChatMemory memory = new BuzhouChatMemory(flaky);

        // 缺省 OFF：读失败上抛（语义不变）
        assertThatThrownBy(() -> memory.get("demo-degrade"))
                .hasMessageContaining("DB 瞬断");

        // EMPTY：降级空历史（会话保活），写路径不受影响
        ReadDegradeHolder.set(ReadDegradePolicy.EMPTY);
        assertThat(memory.get("demo-degrade")).isEmpty();
        memory.add("demo-degrade", List.of(
                new org.springframework.ai.chat.messages.UserMessage("新问题"),
                new org.springframework.ai.chat.messages.AssistantMessage("新答复")));
        assertThat(flaky.appended).isEqualTo(2);
    }

    /** 「存储恢复前读全挂、写正常」的 flaky 替身。 */
    private static final class FlakyStore implements MessageStore {
        volatile int appended = 0;

        @Override
        public void append(String sessionId, List<BuzhouMessage> messages) {
            appended += messages.size();
        }

        @Override
        public List<BuzhouMessage> load(String sessionId) {
            throw new IllegalStateException("存储读失败（模拟 DB 瞬断）");
        }

        @Override
        public Optional<BuzhouMessage> findById(String id) {
            return Optional.empty();
        }
    }
}
