package io.github.chyuan_cuihongyuan.buzhou.core.fact;

import java.util.List;
import java.util.Optional;

/**
 * 共享事实库（spec 410 / T711，mem0 共享记忆 + 隔离借鉴）：
 * <b>deny-by-default</b>——owner 恒读、显式 grant 才可读、revoke 幂等。
 * 键即所有权：非 owner 发布已存在键 fail-fast（抢键是配置错误不是竞争）。
 * 拒绝读计数（探测行为可见性）。
 */
public interface SharedFactStore {

    /** 发布/覆盖自己的键（非 owner 发布已存在键 IllegalArgumentException）。 */
    void publish(SharedFact fact);

    /** 授权 reader 读该键（键不存在 IllegalArgumentException；重复 grant 幂等）。 */
    void grant(String factKey, String reader);

    /** 收回授权（幂等；owner 恒读——revoke owner 无效）。 */
    void revoke(String factKey, String reader);

    /** 读：owner/被授权者可得；否则 empty（deny-by-default + 拒绝计数）。 */
    Optional<Object> read(String reader, String factKey);

    /** 该 reader 可读的全部事实（owner 键 + 被授权键；过期滤除）。 */
    List<SharedFact> readable(String reader);

    /** 被拒绝读次数（探测可见性）。 */
    long deniedReads();
}
