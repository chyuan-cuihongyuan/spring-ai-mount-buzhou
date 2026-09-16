package io.github.chyuan_cuihongyuan.buzhou.tools.http;

import java.util.HashSet;
import java.util.Set;

/**
 * 重定向预算（spec 2030 / T3161 / impl 1581）——curl max-redirs + 环
 * 检测思想：HTTP 跟随重定向的双重防线——跳数预算（超即停——恶意/失控
 * 服务端可无限 302 拖死客户端）与已访问环检测（A→B→A 服务端配置错
 * 误时预算会空耗——环检测在预算耗尽前识破）。单请求一件（非共享）。
 */
public final class RedirectBudget {

    /** 跟随裁决三态。 */
    public enum Decision {
        /** 预算内且未见环——跟随。 */
        FOLLOW,
        /** 跳数预算耗尽——停（返回当前响应）。 */
        BUDGET_EXHAUSTED,
        /** 目标 URL 已访问过——环识破（服务端配置错误信号）。 */
        LOOP_DETECTED
    }

    private final int maxRedirects;
    private final Set<String> visited = new HashSet<>();
    private int hops;

    /** 契约：maxRedirects ≥ 0（0 = 不跟随任何重定向；fail-fast）。 */
    public RedirectBudget(int maxRedirects) {
        if (maxRedirects < 0) {
            throw new IllegalArgumentException("maxRedirects 须 ≥ 0：" + maxRedirects);
        }
        this.maxRedirects = maxRedirects;
    }

    /** 首个请求 URL 锚定（环检测基点）。契约：url 非空。 */
    public void startFrom(String initialUrl) {
        if (initialUrl == null || initialUrl.isBlank()) {
            throw new IllegalArgumentException("initialUrl 不能为空");
        }
        visited.add(initialUrl);
    }

    /**
     * 裁决是否跟随到 nextUrl：预算耗尽 → BUDGET_EXHAUSTED；nextUrl
     * 已访问 → LOOP_DETECTED；否则 FOLLOW（记账 hop + 访问集）。
     */
    public Decision decide(String nextUrl) {
        if (nextUrl == null || nextUrl.isBlank()) {
            throw new IllegalArgumentException("nextUrl 不能为空");
        }
        if (hops >= maxRedirects) {
            return Decision.BUDGET_EXHAUSTED;
        }
        if (!visited.add(nextUrl)) {
            return Decision.LOOP_DETECTED; // 已在访问集——环
        }
        hops++;
        return Decision.FOLLOW;
    }

    /** 已跟随跳数。 */
    public int hops() {
        return hops;
    }

    /** 访问过的 URL 数（含起点）。 */
    public int visitedCount() {
        return visited.size();
    }
}
