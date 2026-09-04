package io.github.chyuan_cuihongyuan.buzhou.core.config;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.crypto.EncryptingMessageStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MessageStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 333 / impl-356：消息加密装配回归——master-key 声明即 BPP 换装
 * （仅 messageStore 槽 + 容器内往返）/ 未配零变化 / 坏钥启动红。
 */
class BuzhouMessageEncryptionAssemblyTest {

    private static String key(int seed) {
        byte[] bytes = new byte[32];
        java.util.Arrays.fill(bytes, (byte) seed);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouCoreAutoConfiguration.class));

    @Test
    void masterKeyDeclared_storesWrapped_roundtripInContainer() {
        BuzhouStores base = Buzhou.inMemoryStores();
        runner.withPropertyValues(
                "buzhou.security.message-encryption.master-key=" + key(1),
                "buzhou.security.message-encryption.previous-master-key=" + key(2))
                .withBean(BuzhouStores.class, () -> base)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    BuzhouStores stores = context.getBean(BuzhouStores.class);
                    assertThat(stores.messageStore()).isInstanceOf(EncryptingMessageStore.class);
                    // spec 336：单开关双槽——summary 同换装
                    assertThat(stores.summaryStore()).isInstanceOf(
                            io.github.chyuan_cuihongyuan.buzhou.core.crypto.EncryptingSummaryStore.class);
                    // 其余四槽原样（与未包装基线同一实例；state 槽 CAS 比值面不加密——诚实边界）
                    assertThat(stores.sessionStateStore()).isSameAs(base.sessionStateStore());
                    assertThat(stores.observabilityStore()).isSameAs(base.observabilityStore());
                    // 容器内行为往返
                    MessageStore messageStore = stores.messageStore();
                    io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage original =
                            new io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage(
                                    "m-1", "s-1", 1, 1,
                                    io.github.chyuan_cuihongyuan.buzhou.core.message.Role.USER,
                                    "容器内明文", List.of(), null, null, null, java.util.Map.of(),
                                    java.time.Instant.parse("2026-09-04T12:00:00Z"));
                    messageStore.append("s-1", List.of(original));
                    assertThat(messageStore.load("s-1")).containsExactly(original);
                });
    }

    @Test
    void noMasterKey_noWrapping() {
        runner.withBean(BuzhouStores.class, Buzhou::inMemoryStores)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(BuzhouStores.class).messageStore())
                            .isNotInstanceOf(EncryptingMessageStore.class);
                    assertThat(context.getBean(BuzhouStores.class).summaryStore())
                            .isNotInstanceOf(
                                    io.github.chyuan_cuihongyuan.buzhou.core.crypto.EncryptingSummaryStore.class);
                });
    }

    @Test
    void badKeyFailsAtStartup() {
        runner.withBean(BuzhouStores.class, Buzhou::inMemoryStores)
                .withPropertyValues("buzhou.security.message-encryption.master-key=%%%")
                .run(context -> assertThat(context).hasFailed());
    }
}
