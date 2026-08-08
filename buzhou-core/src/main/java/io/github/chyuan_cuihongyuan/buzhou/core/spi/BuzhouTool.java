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
}
