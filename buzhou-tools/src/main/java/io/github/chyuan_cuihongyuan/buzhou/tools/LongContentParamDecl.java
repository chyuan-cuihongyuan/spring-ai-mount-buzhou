package io.github.chyuan_cuihongyuan.buzhou.tools;

/**
 * 写侧长内容参数声明（spec 06：content/contentPath、body/bodyPath 互补对）。
 *
 * <p>buzhou-tools 不能依赖 buzhou-spill（feature 模块依赖白名单），故以本记录暴露声明，
 * 装配侧接线进 {@code SpillGuardModule.Builder.longContentParam(...)}：
 * <pre>{@code
 * tools.longContentParamDecls().forEach(d ->
 *     spillGuard.longContentParam(d.toolName(), d.contentParam(), d.pathParam()));
 * }</pre>
 */
public record LongContentParamDecl(String toolName, String contentParam, String pathParam) {
}
