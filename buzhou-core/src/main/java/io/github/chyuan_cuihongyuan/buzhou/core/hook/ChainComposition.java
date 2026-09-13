package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import java.util.List;
import java.util.Set;

/**
 * impl-755 / spec 1002：hook 链解析快照（Kong plugin priority 借鉴——显式优先级
 * 加稳定决胜序的配置可见性）。
 *
 * @param resolvedHookNames  解析后派发序（order 升序、同序按名字典序；已滤除 disabled）
 * @param ghostDisabledNames disabled 配置中未命中任何 hook 的名字（拼错静默蒸发的显形）
 */
public record ChainComposition(List<String> resolvedHookNames, Set<String> ghostDisabledNames) {

    public ChainComposition {
        resolvedHookNames = List.copyOf(resolvedHookNames);
        ghostDisabledNames = Set.copyOf(ghostDisabledNames);
    }
}
