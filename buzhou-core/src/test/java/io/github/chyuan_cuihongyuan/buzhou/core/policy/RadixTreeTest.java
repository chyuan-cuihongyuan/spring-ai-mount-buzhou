package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5034 / T6170：基数树合同——最长前缀胜出、边分裂
 * （部分命中）、终态与插入序无关、压缩节点数、缺配空回、
 * fail-fast。
 */
class RadixTreeTest {

    private static final String API_PREFIX = "/api";

    private static final String API_V1 = "/api/v1";

    private static final String API_V1_USERS = "/api/v1/users";

    @Test
    void longestPrefixShouldWin() {
        RadixTree<String> tree = new RadixTree<>();
        tree.insert(API_PREFIX, "root-route");
        tree.insert(API_V1, "v1-route");
        tree.insert(API_V1_USERS, "users-route");
        assertThat(tree.match("/api/v1/users/42"))
                .hasValue(new RadixTree.Match<>(API_V1_USERS, "users-route"));
        assertThat(tree.match("/api/v1/orders"))
                .hasValue(new RadixTree.Match<>(API_V1, "v1-route"));
        assertThat(tree.match("/api/v2"))
                .hasValue(new RadixTree.Match<>(API_PREFIX, "root-route"));
        assertThat(tree.match("/ap")).isEmpty();
        assertThat(tree.match("/other")).isEmpty();
        assertThat(tree.match(API_V1_USERS)).hasValue(new RadixTree.Match<>(API_V1_USERS, "users-route"));
    }

    @Test
    void partialEdgeHitShouldSplitAndKeepBothKeys() {
        RadixTree<String> tree = new RadixTree<>();
        tree.insert("romane", "v1");
        tree.insert("roman", "v2");
        tree.insert("rome", "v3");
        assertThat(tree.match("roman")).hasValue(new RadixTree.Match<>("roman", "v2"));
        assertThat(tree.match("romane")).hasValue(new RadixTree.Match<>("romane", "v1"));
        assertThat(tree.match("romanes")).hasValue(new RadixTree.Match<>("romane", "v1"));
        assertThat(tree.match("rome")).hasValue(new RadixTree.Match<>("rome", "v3"));
        assertThat(tree.match("roman empire")).hasValue(new RadixTree.Match<>("roman", "v2"));
        assertThat(tree.match("rombus")).isEmpty();
    }

    @Test
    void finalTreeShouldBeIndependentOfInsertOrder() {
        RadixTree<String> forward = new RadixTree<>();
        forward.insert("abc", "1");
        forward.insert("ab", "2");
        forward.insert("a", "3");
        RadixTree<String> backward = new RadixTree<>();
        backward.insert("a", "3");
        backward.insert("ab", "2");
        backward.insert("abc", "1");
        assertThat(forward.nodeCount()).isEqualTo(backward.nodeCount());
        assertThat(forward.nodeCount()).isEqualTo(4);
        for (String probe : new String[]{"a", "ab", "abc", "abcd", "b"}) {
            assertThat(forward.match(probe)).isEqualTo(backward.match(probe));
        }
    }

    @Test
    void compressedNodeCountShouldStayMinimal() {
        RadixTree<String> tree = new RadixTree<>();
        tree.insert("abcdef", "1");
        tree.insert("abcxyz", "2");
        assertThat(tree.nodeCount()).isEqualTo(4);
        assertThat(tree.keyCount()).isEqualTo(2);
        assertThat(tree.match("abcdef")).hasValue(new RadixTree.Match<>("abcdef", "1"));
        assertThat(tree.match("abcxyz")).hasValue(new RadixTree.Match<>("abcxyz", "2"));
        assertThat(tree.match("abc")).isEmpty();
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        RadixTree<String> tree = new RadixTree<>();
        assertThatThrownBy(() -> tree.insert(null, "v")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tree.insert("", "v")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tree.insert("k", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tree.match(null)).isInstanceOf(IllegalArgumentException.class);
        tree.insert("dup", "v1");
        assertThatThrownBy(() -> tree.insert("dup", "v2")).isInstanceOf(IllegalArgumentException.class);
    }
}
