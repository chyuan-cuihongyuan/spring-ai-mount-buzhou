package io.github.chyuan_cuihongyuan.buzhou.dashboard;

import java.util.List;

/**
 * Skill 管理端口（spec 03 推演 #13，ticket 17）：dashboard 不直依 buzhou-skills
 * （09 模块工程档白名单的唯一二层边是 → buzhou-observability），Skill 管理页后端
 * 经本 SPI 由装配侧适配注入（适配器薄包 {@code SkillAdminApi}，归 starter/examples）。
 *
 * <p>未注入时 HTTP 层 Skill 端点回 501。
 */
public interface SkillAdminPort {

    /** 管理视图条目（对应 SkillSummary：DB 与内置合并，标注来源/状态/覆盖关系）。 */
    record SkillView(String name, String description, String source, String status,
                     boolean dbOverridesClasspath) {}

    List<SkillView> listSkills();

    /** 新建 DB Skill（初始 DRAFT）。 */
    SkillView create(String name, String description, String body,
                     List<String> allowedTools, String createdBy);

    /** 编辑 DB Skill（null 字段保留原值）。 */
    SkillView update(String name, String description, String body, List<String> allowedTools);

    /** 上架：DRAFT/DISABLED → PUBLISHED。 */
    SkillView publish(String name);

    /** 下架：PUBLISHED → DISABLED。 */
    SkillView disable(String name);

    boolean delete(String name);

    List<String> getBinding(String appId, String agentName);

    void setBinding(String appId, String agentName, List<String> skillNames);
}
