package io.github.chyuan_cuihongyuan.buzhou.resilience.cache;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 前缀块命中读面（spec 1804 / T2809 / impl 1405）——vLLM block-level prefix
 * cache / SGLang radix tree 思想：连续请求共享的公共前缀（系统提示 + 滚动
 * 历史）按固定块切分后逐块复用，**块命中率**直接量化「KV/嵌入计算省掉了
 * 多少」。命中率持续走高 = 前缀稳定，值得预热与 radix 索引；命中率贴地 =
 * 每次请求都是新面孔，前缀投资无回报。
 *
 * <p>纯函数零状态（索引为调用局部，无跨调用残留）：输入为块大小与请求文本
 * 序列（口径由调用方声明——字符块/token 块皆可，读面只认「等长块」）。
 * 语义对齐 vLLM：**尾块不足整块不参与复用账**（partial block 不入缓存）；
 * 同一请求内部重复块只记首见（跨请求才算复用——radix 树内一次插入）。
 */
public final class PrefixBlockHitStats {

    /** vLLM 语义对齐的默认块大小（字符口径；调用方可另声明）。 */
    public static final int DEFAULT_BLOCK_SIZE = 64;

    private PrefixBlockHitStats() {
    }

    /**
     * @param requests     参与统计的请求数
     * @param totalBlocks  入账块总数（全块；尾块被丢弃的请求贡献 0）
     * @param reusedBlocks 跨请求复用块数（每内容首见记新、再见记复用）
     */
    public record BlockReport(int blockSize, int requests, long totalBlocks,
                              long reusedBlocks) {

        /** 块命中率（无入账块 -1 哨兵）。 */
        public double blockHitRatio() {
            return totalBlocks == 0 ? -1d : (double) reusedBlocks / totalBlocks;
        }

        /** 新块率 = 1 − 命中率（无入账块 -1 哨兵）。 */
        public double blockMissRatio() {
            return totalBlocks == 0 ? -1d : 1d - blockHitRatio();
        }
    }

    /**
     * 命中账目入口。契约：blockSize ≥ 1（fail-fast）；null 按空表；null 元素
     * 按空文本（无全块可入账）。
     */
    public static BlockReport analyze(int blockSize, List<String> prompts) {
        if (blockSize < 1) {
            throw new IllegalArgumentException("blockSize 不能小于 1：" + blockSize);
        }
        List<String> window = prompts == null ? List.of() : prompts;
        long total = 0;
        long reused = 0;
        Set<String> seenAcrossRequests = new HashSet<>();
        Set<String> seenInThisRequest = new HashSet<>();
        for (String prompt : window) {
            String text = prompt == null ? "" : prompt;
            seenInThisRequest.clear();
            for (int offset = 0; offset + blockSize <= text.length(); offset += blockSize) {
                String block = text.substring(offset, offset + blockSize);
                total++;
                // 同请求内重复块只记首见；跨请求再见才计复用（radix 一次插入）
                if (seenInThisRequest.add(block) && !seenAcrossRequests.add(block)) {
                    reused++;
                }
            }
        }
        return new BlockReport(blockSize, window.size(), total, reused);
    }
}
