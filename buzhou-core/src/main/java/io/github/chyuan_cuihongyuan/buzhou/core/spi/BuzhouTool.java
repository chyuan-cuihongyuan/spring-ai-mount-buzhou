package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 内置原子工具元数据声明（spec 06 注册模型）：name / idempotent / serialGroup。
 *
 * <p>装配方（ToolsModule / AutoConfiguration）读取注解填充 {@code RuntimeConfig}
 * 的 {@code idempotentToolNames} 与 {@code serialGroups}：
 * <ul>
 *   <li>{@code idempotent=true} — 悬空调用修复时允许合成中断结果（见 DanglingCallRepairer）；</li>
 *   <li>{@code serialGroup} — 同组工具调用串行执行（见 HarnessToolCallingManager 组锁）。</li>
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface BuzhouTool {

    /** 工具名（须与 ToolDefinition.name 一致）。 */
    String name();

    /** 是否幂等（只读/可重放）。 */
    boolean idempotent() default false;

    /** 串行组名；空 = 不串行（参与并行 fan-out）。 */
    String serialGroup() default "";

    /**
     * 是否破坏性（写侧副作用：写文件 / 执行命令 / 外呼变更类请求）——工具自描述
     * 风险维度（MCP tool annotations destructiveHint 思想）；ToolsModule 据此
     * 自动生成危险工具名单（装配侧注册进 GuardModule 的 HITL 清单），标注即
     * 入册、免改装配源码。默认 false（只读 / 幂等工具不标）。
     */
    boolean destructive() default false;
}
