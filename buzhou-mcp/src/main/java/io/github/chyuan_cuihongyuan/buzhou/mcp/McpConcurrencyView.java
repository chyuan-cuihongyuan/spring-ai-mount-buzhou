package io.github.chyuan_cuihongyuan.buzhou.mcp;

/**
 * 每连接并发占用视图（spec 722 / T1044，610 并发闸的读数面）：
 * limit=-1 = 未设上限；available = 剩余许可；inFlight = 在途调用数。
 */
public record McpConcurrencyView(String server, int limit, int available, int inFlight) {

    public static final int UNSET = -1;
}
