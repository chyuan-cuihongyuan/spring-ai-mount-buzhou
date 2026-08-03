package io.github.chyuan_cuihongyuan.buzhou.core.session;

/**
 * 会话装配定制器：机制模块（buzhou-observability 等）实现后挂进
 * {@link RuntimeConfig#assemblyCustomizers()}，由 {@code HarnessAssembler.assemble} 在装配
 * advisor/工具的固定步骤之间回调，注入自身 advisor 与工具包装。
 *
 * <p>与 {@link SessionResourceCustomizer}（资源注册）区别：本接口作用于 ChatClient 装配形态
 * （advisor 链 + 工具回调），先于 ChatClient 构建发生。
 */
@FunctionalInterface
public interface SessionAssemblyCustomizer {

    void customize(SessionAssemblyContext ctx);
}
