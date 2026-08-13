package io.github.chyuan_cuihongyuan.buzhou.spill;

public record RangeReadRequest(Mode mode, Integer offset, Integer limit,
                               String jsonPath, String cursor, Window window, Integer tailLimit) {

    public enum Mode {
        BYTES,
        JSON,
        PAGE
    }

    /**
     * bytes 模式的窗口风味（impl-03 / T43，Codex「头尾各半掐中间」风味的无损版）：
     * 取头/尾窗口、中段以<b>显式省略标记</b>（省略量 + offset 区间 + 回读指引）替代——
     * 原始字节在 spill 存储完整保留、可无损回取（与 Codex 销毁式截断的本质差异）。
     */
    public enum Window {
        /** 无窗口（既有 offset/limit 区间语义）。 */
        NONE,
        /** 仅取头部，尾部以省略标记收尾。 */
        HEAD,
        /** 仅取尾部，头部以省略标记开头。 */
        TAIL,
        /** 头 + 尾各取一段，中段以省略标记连接（schema 在头、结论在尾的数据一次看全）。 */
        HEAD_TAIL
    }

    public static RangeReadRequest bytes(int offset, int limit) {
        return new RangeReadRequest(Mode.BYTES, offset, limit, null, null, Window.NONE, null);
    }

    /**
     * 窗口风味请求：{@code headBytes}/{@code tailBytes} 为头/尾窗口大小（对称默认由调用方保证）。
     */
    public static RangeReadRequest bytesWindow(Window window, int headBytes, int tailBytes) {
        return new RangeReadRequest(Mode.BYTES, null, Math.max(0, headBytes), null, null,
                window == null ? Window.NONE : window, Math.max(0, tailBytes));
    }

    public static RangeReadRequest json(String jsonPath) {
        return new RangeReadRequest(Mode.JSON, null, null, jsonPath, null, Window.NONE, null);
    }

    public static RangeReadRequest page(String cursor, int limit) {
        return new RangeReadRequest(Mode.PAGE, null, limit, null, cursor, Window.NONE, null);
    }
}
