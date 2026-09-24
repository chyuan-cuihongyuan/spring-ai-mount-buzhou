package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shuffle Sharding 洗牌分片（spec 5038 / T6177 / impl 2189）——
 * AWS shuffle sharding 思想：每租户从 n 个分片里种子化随机
 * 挑 k 个（SplitMix64 种子异或租户指纹 → Fisher-Yates 洗牌
 * 取前 k），请求只路由到本租户的分片子集——一个坏分片对
 * 单租户的爆炸半径从 n/n 压到 k/n（两租户分片子集期望重叠
 * k²/n——指望隔离性而不指望互相不可见）。同租户同子集
 * （种子确定性），无共享状态路由。确定性无时间依赖。
 *
 * <p>与 BoundedLoadRing（spec 5003）同族不同面：子集隔离
 * vs 全局负载有界；与 JumpConsistentHash（spec 3020）
 * 不同面：租户多分片冗余 vs 单键单桶。
 */
public final class ShuffleSharding {

    /** SplitMix64 黄金常量（种子化洗牌发生器）。 */
    private static final long SPLITMIX_GAMMA = 0x9e3779b97f4a7c15L;

    /** SplitMix64 混合乘子一。 */
    private static final long SPLITMIX_MULT_A = 0xbf58476d1ce4e5b9L;

    /** SplitMix64 混合乘子二。 */
    private static final long SPLITMIX_MULT_B = 0x94d049bb133111ebL;

    /** FNV-1a 64 偏移基（租户指纹）。 */
    private static final long FNV_OFFSET_BASIS = -3750763034362895579L;

    /** FNV-1a 64 素数。 */
    private static final long FNV_PRIME = 1099511628211L;

    private final int shardCount;
    private final int shardsPerTenant;
    private final long seed;
    private final Map<String, List<Integer>> assignments = new ConcurrentHashMap<>();

    /** 定构（0<shardsPerTenant≤shardCount fail-fast）。 */
    public ShuffleSharding(int shardCount, int shardsPerTenant, long seed) {
        if (shardCount < 1) {
            throw new IllegalArgumentException("shardCount≥1：" + shardCount);
        }
        if (shardsPerTenant < 1 || shardsPerTenant > shardCount) {
            throw new IllegalArgumentException("shardsPerTenant 1.." + shardCount + "：" + shardsPerTenant);
        }
        this.shardCount = shardCount;
        this.shardsPerTenant = shardsPerTenant;
        this.seed = seed;
    }

    /** 租户分片子集（升序确定性；重复调用同结果）。 */
    public List<Integer> assign(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            throw new IllegalArgumentException("tenantId 非空");
        }
        return assignments.computeIfAbsent(tenantId, this::computeAssignment);
    }

    /** 租户请求是否路由到指定分片。 */
    public boolean routesTo(String tenantId, int shard) {
        if (shard < 0 || shard >= shardCount) {
            throw new IllegalArgumentException("分片越界 0.." + (shardCount - 1) + "：" + shard);
        }
        return assign(tenantId).contains(shard);
    }

    /** 两租户分片子集重叠数（爆炸半径共担面读数）。 */
    public int overlap(String tenantA, String tenantB) {
        List<Integer> a = assign(tenantA);
        List<Integer> b = assign(tenantB);
        return (int) a.stream().filter(b::contains).count();
    }

    /** 分片数读数。 */
    public int shardCount() {
        return shardCount;
    }

    /** 每租户分片数读数。 */
    public int shardsPerTenant() {
        return shardsPerTenant;
    }

    private List<Integer> computeAssignment(String tenantId) {
        long[] table = new long[shardCount];
        for (int i = 0; i < shardCount; i++) {
            table[i] = i;
        }
        long state = seed ^ fingerprint(tenantId);
        for (int i = shardCount - 1; i > 0; i--) {
            state += SPLITMIX_GAMMA;
            long z = state;
            z = (z ^ (z >>> 30)) * SPLITMIX_MULT_A;
            z = (z ^ (z >>> 27)) * SPLITMIX_MULT_B;
            long next = z ^ (z >>> 31);
            int j = (int) Long.remainderUnsigned(next, i + 1L);
            long tmp = table[i];
            table[i] = table[j];
            table[j] = tmp;
        }
        List<Integer> chosen = new ArrayList<>(shardsPerTenant);
        for (int i = 0; i < shardsPerTenant; i++) {
            chosen.add((int) table[i]);
        }
        List<Integer> result = chosen.stream().sorted().toList();
        return List.copyOf(result);
    }

    private static long fingerprint(String tenantId) {
        long hash = FNV_OFFSET_BASIS;
        for (int i = 0; i < tenantId.length(); i++) {
            hash = (hash ^ tenantId.charAt(i)) * FNV_PRIME;
        }
        return hash;
    }
}
