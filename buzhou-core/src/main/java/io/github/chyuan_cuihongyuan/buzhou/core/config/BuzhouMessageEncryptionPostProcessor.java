package io.github.chyuan_cuihongyuan.buzhou.core.config;

import io.github.chyuan_cuihongyuan.buzhou.core.crypto.EncryptingMessageStore;
import io.github.chyuan_cuihongyuan.buzhou.core.crypto.EnvelopeCipher;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MessageStore;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.env.Environment;

/**
 * 消息静态加密包装器（spec 333 / T658，Vault transit / KMS envelope 借鉴）：
 * 声明 {@code buzhou.security.message-encryption.master-key} 即启用——捕获
 * {@link BuzhouStores} bean 重建（仅换 messageStore 槽为
 * {@link EncryptingMessageStore}，其余五槽原样；宿主零改动）。
 *
 * <p>钥经 {@link Environment} 直读（BPP 早于属性 bean 就绪的诚实顺序）；
 * 非法钥 fail-fast（16/24/32 字节 AES 钥，带修法）。
 */
final class BuzhouMessageEncryptionPostProcessor implements BeanPostProcessor {

    private final EnvelopeCipher cipher;

    BuzhouMessageEncryptionPostProcessor(Environment environment) {
        this.cipher = new EnvelopeCipher(
                environment.getProperty("buzhou.security.message-encryption.master-key"),
                environment.getProperty("buzhou.security.message-encryption.previous-master-key"));
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof BuzhouStores stores
                && !(stores.messageStore() instanceof EncryptingMessageStore)) {
            MessageStore encrypted = new EncryptingMessageStore(stores.messageStore(), cipher);
            return new BuzhouStores(encrypted, stores.summaryStore(), stores.sessionStateStore(),
                    stores.sessionLeaseStore(), stores.observabilityStore(), stores.unitOfWork());
        }
        return bean;
    }
}
