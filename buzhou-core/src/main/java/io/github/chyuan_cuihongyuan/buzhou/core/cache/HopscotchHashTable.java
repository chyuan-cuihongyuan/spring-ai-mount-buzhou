package io.github.chyuan_cuihongyuan.buzhou.core.cache;

import java.util.ArrayList;
import java.util.List;

/**
 * 跳房子哈希表（spec 8012 / V8025 / impl 2314）——
 * Herlihy, Shavit & Tzafrir 2008 思想（U 系两度退雾区遗珠，
 * 本轮认领）：**每桶 H 位邻域位图承诺「元素必落在家桶 H 槽
 * 邻域内」**——探测距离钉死常数 H；插入超邻域时沿空位回跳
 * swap 链把空位向家桶单调搬近（每步严格递减——勘误：初版
 * 允许空位回退来回搬运死循环，且数组访问缺模数回绕——
 * 线性偏移单调化+全程模数钉住修正）；无可搬元素扩容 ×2。
 * put upsert 覆值不增位/get 缺席 null 诚实/remove 缺席
 * fail-fast/size 读数；装载上限 0.9；单线程顺序语义（论文
 * 并发面不在本件——明示）；null 键/值 fail-fast；同操作序
 * 同布局完全确定。
 *
 * <p>与 ExtendibleHashing（T 系）同族不同面：目录倍增 vs
 * 邻域不变量常数探测。
 */
public final class HopscotchHashTable {

    /** 默认邻域宽（int 位图上限 32——本表取 16）。 */
    private static final int DEFAULT_NEIGHBORHOOD = 16;

    /** 装载上限（超过即扩容——纪律常数明示）。 */
    private static final double LOAD_LIMIT = 0.9;

    private final int neighborhood;
    private String[] keys;
    private String[] values;
    private int[] hop;
    private int size;

    /** 默认邻域 16、初始桶 64。 */
    public HopscotchHashTable() {
        this(DEFAULT_NEIGHBORHOOD, 64);
    }

    /** 注入邻域宽与初始桶数（桶数幂次向上取整；测试可注入小 H 触发扩容）。 */
    public HopscotchHashTable(int neighborhood, int initialBuckets) {
        if (neighborhood < 2 || neighborhood > 32) {
            throw new IllegalArgumentException("邻域宽 ∈ [2,32]（实际 " + neighborhood + "）");
        }
        this.neighborhood = neighborhood;
        int buckets = Integer.highestOneBit(Math.max(8, initialBuckets - 1)) * 2;
        this.keys = new String[buckets];
        this.values = new String[buckets];
        this.hop = new int[buckets];
    }

    /** 放入（upsert 覆值不增位；返回旧值或 null）。 */
    public String put(String key, String value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("键值均非空引用");
        }
        int existing = slotOf(key);
        if (existing >= 0) {
            String old = values[existing];
            values[existing] = value;
            return old;
        }
        if ((size + 1) > keys.length * LOAD_LIMIT) {
            expand();
        }
        insert(key, value);
        return null;
    }

    /** 取值（缺席 null 诚实缺省）。 */
    public String get(String key) {
        if (key == null) {
            throw new IllegalArgumentException("键非空引用");
        }
        int slot = slotOf(key);
        return slot < 0 ? null : values[slot];
    }

    /** 删除（缺席 fail-fast）。 */
    public void remove(String key) {
        if (key == null) {
            throw new IllegalArgumentException("键非空引用");
        }
        int slot = slotOf(key);
        if (slot < 0) {
            throw new IllegalArgumentException("键缺席（" + key + "）");
        }
        int home = homeOf(key);
        int offset = offsetOf(slot, home);
        hop[home] &= ~(1 << offset);
        keys[slot] = null;
        values[slot] = null;
        size--;
    }

    /** 键数。 */
    public int size() {
        return size;
    }

    /** 邻域不变量审计：任意键命中槽 ∈ 家桶 H 邻域且位图位真实（测试/自检面）。 */
    public boolean neighborhoodInvariantHolds() {
        int len = keys.length;
        for (int slot = 0; slot < len; slot++) {
            if (keys[slot] == null) {
                continue;
            }
            int home = homeOf(keys[slot]);
            int offset = offsetOf(slot, home);
            if (offset >= neighborhood || (hop[home] & (1 << offset)) == 0) {
                return false;
            }
        }
        for (int home = 0; home < len; home++) {
            for (int bit = 0; bit < neighborhood; bit++) {
                if ((hop[home] & (1 << bit)) != 0 && keys[(home + bit) % len] == null) {
                    return false;
                }
            }
        }
        return true;
    }

    /** 键在家桶邻域内的槽（缺席 −1）。 */
    private int slotOf(String key) {
        int home = homeOf(key);
        int len = keys.length;
        for (int bit = 0; bit < neighborhood; bit++) {
            if ((hop[home] & (1 << bit)) != 0) {
                int idx = (home + bit) % len;
                if (key.equals(keys[idx])) {
                    return idx;
                }
            }
        }
        return -1;
    }

    /** 插入主循环：找空位→邻域内落位；否则 swap 链把空位向家桶单调搬近/扩容重来。 */
    private void insert(String key, String value) {
        int home = homeOf(key);
        int len = keys.length;
        int empty = firstEmptyFrom(home);
        while (true) {
            int offset = offsetOf(empty, home);
            if (offset < neighborhood) {
                keys[empty] = key;
                values[empty] = value;
                hop[home] |= (1 << offset);
                size++;
                return;
            }
            int mover = -1;
            int moverBit = -1;
            for (int back = neighborhood - 1; back >= 1; back--) {
                int candidate = ((empty - back) % len + len) % len;
                if (keys[candidate] == null || hop[candidate] == 0) {
                    continue;
                }
                for (int bit = neighborhood - 1; bit >= 0; bit--) {
                    if ((hop[candidate] & (1 << bit)) == 0) {
                        continue;
                    }
                    int y = (candidate + bit) % len;
                    if (offsetOf(y, home) < offset) {
                        mover = candidate;
                        moverBit = bit;
                        break;
                    }
                }
                if (mover >= 0) {
                    break;
                }
            }
            if (mover < 0) {
                expand();
                home = homeOf(key);
                len = keys.length;
                empty = firstEmptyFrom(home);
                continue;
            }
            int y = (mover + moverBit) % len;
            keys[empty] = keys[y];
            values[empty] = values[y];
            keys[y] = null;
            values[y] = null;
            hop[mover] &= ~(1 << moverBit);
            hop[mover] |= (1 << offsetOf(empty, mover));
            empty = y;
        }
    }

    /** slot 相对 home 的前向偏移 ∈ [0,len)。 */
    private int offsetOf(int slot, int home) {
        int offset = slot - home;
        return offset < 0 ? offset + keys.length : offset;
    }

    /** 自 home 起模数序首个空槽（装载上限保证存在）。 */
    private int firstEmptyFrom(int home) {
        int len = keys.length;
        for (int step = 0; step < len; step++) {
            int idx = (home + step) % len;
            if (keys[idx] == null) {
                return idx;
            }
        }
        throw new IllegalStateException("无空槽（装载上限失守——容量 " + len + "）");
    }

    private void expand() {
        List<String> oldKeys = new ArrayList<>();
        List<String> oldValues = new ArrayList<>();
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null) {
                oldKeys.add(keys[i]);
                oldValues.add(values[i]);
            }
        }
        keys = new String[keys.length * 2];
        values = new String[values.length * 2];
        hop = new int[hop.length * 2];
        int oldSize = size;
        size = 0;
        for (int i = 0; i < oldKeys.size(); i++) {
            insert(oldKeys.get(i), oldValues.get(i));
        }
        if (size != oldSize) {
            throw new IllegalStateException("扩容重插计数失守（" + oldSize + "→" + size + "）");
        }
    }

    private int homeOf(String key) {
        int spread = key.hashCode() * 0x9E3779B9;
        return (spread ^ (spread >>> 16)) & (keys.length - 1);
    }

    /** 桶数（测试/审计面）。 */
    int bucketCount() {
        return keys.length;
    }
}
