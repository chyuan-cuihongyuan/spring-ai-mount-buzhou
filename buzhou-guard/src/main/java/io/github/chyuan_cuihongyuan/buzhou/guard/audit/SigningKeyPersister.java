package io.github.chyuan_cuihongyuan.buzhou.guard.audit;

import java.security.KeyPair;

/**
 * 审计签名密钥轮换持久化钩子（spec 41 §A / T153 / impl-124）：{@link SigningKeyRing#rotate}
 * 在切换 active 引用<b>之前</b>调用本钩子落盘新钥——持久化失败则轮换整体失败、active 不变
 * （写而后切，杜绝「运行期已切、重启后不在环内→期间签名记录变不可验」的断链窗口）。
 *
 * @since 1.0.0
 */
public interface SigningKeyPersister {

    /** 将新钥持久化（如写入 PEM 文件）。任何失败以异常上抛，轮换中止。 */
    void persist(int version, KeyPair keyPair);
}
