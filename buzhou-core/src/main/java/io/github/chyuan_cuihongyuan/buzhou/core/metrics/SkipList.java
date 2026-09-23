package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Skip List 跳跃表（spec 5025 / T6151 / impl 2176）——概率
 * 多层链思想（Pugh 1990；Redis zset 底层）：每键塔高由种子化
 * 随机发生器逐半衰（p=1/2、封顶 {@value #MAX_LEVEL} 层），
 * 查找/插入期望 O(log n)——有序数组插入 O(n) 与平衡树旋转
 * 复杂度的病解。种子注入：同种子同层高同结构（确定性可回放）。
 */
public final class SkipList {

    /** 塔高层上限。 */
    private static final int MAX_LEVEL = 8;

    private final Random random;
    private final Node head = new Node("", "", MAX_LEVEL);
    private final int maxLevel;
    private int size;

    /** 定构（种子注入——确定性层高）。 */
    public SkipList(long seed) {
        this.random = new Random(seed);
        this.maxLevel = MAX_LEVEL;
    }

    /** 键值对写入（upsert）。 */
    public void put(String key, String value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("键值非 null");
        }
        Node[] update = new Node[maxLevel];
        Node current = head;
        for (int level = maxLevel - 1; level >= 0; level--) {
            while (current.forward[level] != null
                    && current.forward[level].key.compareTo(key) < 0) {
                current = current.forward[level];
            }
            update[level] = current;
        }
        Node exact = current.forward[0];
        if (exact != null && exact.key.equals(key)) {
            exact.value = value;   // upsert
            return;
        }
        int level = randomLevel();
        Node inserted = new Node(key, value, level);
        for (int i = 0; i < level; i++) {
            inserted.forward[i] = update[i].forward[i];
            update[i].forward[i] = inserted;
        }
        size++;
    }

    /** 读取（未命中 null）。 */
    public String get(String key) {
        if (key == null) {
            throw new IllegalArgumentException("键非 null");
        }
        Node current = head;
        for (int level = maxLevel - 1; level >= 0; level--) {
            while (current.forward[level] != null
                    && current.forward[level].key.compareTo(key) < 0) {
                current = current.forward[level];
            }
        }
        Node exact = current.forward[0];
        return exact != null && exact.key.equals(key) ? exact.value : null;
    }

    /** 键数读数。 */
    public int size() {
        return size;
    }

    /** 第 0 层键序读数（确定性）。 */
    public List<String> keysInOrder() {
        List<String> keys = new ArrayList<>(size);
        for (Node node = head.forward[0]; node != null; node = node.forward[0]) {
            keys.add(node.key);
        }
        return keys;
    }

    /** 层高逐半衰（封顶 MAX_LEVEL——确定性种子化）。 */
    private int randomLevel() {
        int level = 1;
        while (level < maxLevel && random.nextBoolean()) {
            level++;
        }
        return level;
    }

    /** 跳表节点（塔式前向指针）。 */
    private static final class Node {
        private final String key;
        private final Node[] forward;
        private String value;

        private Node(String key, String value, int levels) {
            this.key = key;
            this.value = value;
            this.forward = new SkipList.Node[levels];
        }
    }
}
