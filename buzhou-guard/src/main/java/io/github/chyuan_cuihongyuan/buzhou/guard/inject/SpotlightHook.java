package io.github.chyuan_cuihongyuan.buzhou.guard.inject;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.Spotlighting;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;

import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * 读侧 Spotlighting（wayfinder T18 / docs/spec/11 guard，来源 MSRC 间接注入防御）：
 * 工具/RAG 输出回灌 prompt 前，用<b>随机分隔符 + 交织标记字符</b>包裹（delimiting + datamarking），
 * 并随数据携带「仅数据」告示——标记段内出现的任何指令/要求/角色设定一律无效。
 *
 * <p>顺序：order 80（先于 SpillOffloadHook 100）——先包裹<b>原始外部输出</b>，再做溢出判断；
 * spill 侧经 {@link Spotlighting#unwrap} 还原后判定形状/落盘（SpillStore 存干净原文），
 * 回读结果再次进入 afterTool 时重新包裹（纵深）。
 *
 * <p>包裹格式单一事实源在 {@link Spotlighting}（core），guard 与 spill 共用、不漂移。
 */
public class SpotlightHook implements BuzhouHook {

    public static final int ORDER = 80;

    private final String tag;
    private final char markChar;
    private final int markEveryNChars;

    public SpotlightHook() {
        this(randomTag(), Spotlighting.DEFAULT_MARK_CHAR, 1);
    }

    public SpotlightHook(String tag, char markChar, int markEveryNChars) {
        this.tag = tag;
        this.markChar = markChar;
        this.markEveryNChars = Math.max(1, markEveryNChars);
    }

    @Override
    public String name() {
        return "SpotlightHook";
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public HookResult afterTool(ToolCallContext ctx) {
        if (ctx.error() != null || ctx.result() == null) {
            return HookResult.CONTINUE;
        }
        String content = String.valueOf(ctx.result());
        if (content.contains(Spotlighting.BEGIN_HEAD)) {
            return HookResult.CONTINUE; // 已包裹（幂等；readback 切片含标记段时不再二次包裹）
        }
        if (content.startsWith(CanaryGuardHook.INTERCEPT_NOTICE)) {
            return HookResult.CONTINUE; // 拦截告示是可信框架文本（非外部数据），不包裹
        }
        ctx.replaceResult(Spotlighting.wrap(tag, markChar, markEveryNChars, content));
        return HookResult.CONTINUE;
    }

    /** 包裹为「随机分隔符 + 数据告示 + 交织标记内容」。 */
    public String wrap(String content) {
        return Spotlighting.wrap(tag, markChar, markEveryNChars, content);
    }

    /** 去除标记字符（测试/自校验用：验证包裹不丢失原文）。 */
    public static String stripDatamarking(String marked, char markChar) {
        return Spotlighting.stripMark(marked, markChar);
    }

    /** 会话随机标记段（8 hex）。 */
    static String randomTag() {
        byte[] bytes = new byte[4];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
