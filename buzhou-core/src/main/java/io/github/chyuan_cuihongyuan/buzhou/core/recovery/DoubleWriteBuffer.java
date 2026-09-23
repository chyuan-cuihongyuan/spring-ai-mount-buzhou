package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DoubleWrite 双写缓冲（spec 5012 / T6125 / impl 2163）——
 * InnoDB doublewrite buffer 思想：页写入**先入共享暂存缓冲**
 * （同页覆盖——最新版胜），缓冲满自动整体落盘（清空 +
 * flushCount++）守恒；{@code recoverable()} 给出崩溃恢复视图
 * （暂存中各页最新版）——就地更新写到一半崩溃（页撕裂半新
 * 半旧）时以此为恢复源。页级写崩溃一致性的病解。
 *
 * <p>与 HintedHandoff 同族不同面：投递暂代 vs 崩溃恢复暂存。
 */
public final class DoubleWriteBuffer {

    /**
     * 暂存页。
     *
     * @param pageId 页号
     * @param data 页数据（副本——外界改动不回灌）
     */
    public record Page(long pageId, byte[] data) {
    }

    private final int pageSlots;
    private final Map<Long, byte[]> staged = new LinkedHashMap<>();
    private long flushCount;

    /** 定构（pageSlots ≤0 fail-fast）。 */
    public DoubleWriteBuffer(int pageSlots) {
        if (pageSlots <= 0) {
            throw new IllegalArgumentException("pageSlots>0：" + pageSlots);
        }
        this.pageSlots = pageSlots;
    }

    /** 暂存页（缓冲满先自动整体落盘再装入；空 data fail-fast）。 */
    public void stage(long pageId, byte[] data) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("data 非空：" + pageId);
        }
        if (!staged.containsKey(pageId) && staged.size() >= pageSlots) {
            flush();   // 自动整体落盘——容量守恒
        }
        staged.put(pageId, Arrays.copyOf(data, data.length));
    }

    /** 手动整体落盘（返回并清空暂存页）。 */
    public List<Page> flush() {
        List<Page> pages = new java.util.ArrayList<>(staged.size());
        for (Map.Entry<Long, byte[]> entry : staged.entrySet()) {
            pages.add(new Page(entry.getKey(), entry.getValue()));
        }
        staged.clear();
        if (!pages.isEmpty()) {
            flushCount++;
        }
        return List.copyOf(pages);
    }

    /** 崩溃恢复视图（暂存中各页最新版；页数据为防御性副本）。 */
    public List<Page> recoverable() {
        List<Page> pages = new java.util.ArrayList<>(staged.size());
        for (Map.Entry<Long, byte[]> entry : staged.entrySet()) {
            pages.add(new Page(entry.getKey(), Arrays.copyOf(entry.getValue(), entry.getValue().length)));
        }
        return List.copyOf(pages);
    }

    /** 暂存页数读数。 */
    public int pendingCount() {
        return staged.size();
    }

    /** 已发生整体落盘次数读数。 */
    public long flushCount() {
        return flushCount;
    }

    /** 容量读数。 */
    public int pageSlots() {
        return pageSlots;
    }
}
