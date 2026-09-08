package io.github.chyuan_cuihongyuan.buzhou.core.prompt;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 提示词注册表（spec 401 / T693，Langfuse prompt management 借鉴——版本 +
 * 标签双轴）：publish 得单调版本、标签恒指一个版本（晋级/回滚同一动作）、
 * 按版钉取复现历史。标签语义归宿主（registry 只管指针不解释标签名——
 * production/staging 是约定不是机制）。
 */
public interface PromptRegistry {

    /** 自动管理的默认标签（publish 恒重指最新版）。 */
    String LATEST = "latest";

    /** 发布新版本（per-name 单调递增；latest 自动重指）；note 可 null。 */
    PromptVersion publish(String name, String body, String note);

    /** 标签重指（晋级/回滚同一动作）；未知 name/version fail-fast。 */
    void label(String name, String label, int version);

    /** 按默认标签 latest 解析；无名/无版本空。 */
    Optional<PromptVersion> resolve(String name);

    /** 按标签解析；无名/无标签空。 */
    Optional<PromptVersion> resolve(String name, String label);

    /** 按版本号钉取（复现历史行为——不受标签移动影响）；无则空。 */
    Optional<PromptVersion> resolveVersion(String name, int version);

    /** 标签→版本指针表（快照）；无名空 map。 */
    Map<String, Integer> labels(String name);

    /** 版本全史（升序，不可变）；无名空列表。 */
    List<PromptVersion> versions(String name);

    /** 已注册名集。 */
    Set<String> names();
}
