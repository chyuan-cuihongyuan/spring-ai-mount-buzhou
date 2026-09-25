package io.github.chyuan_cuihongyuan.buzhou.core.fs;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6010 / T6222：PieceTable 合同——原稿只读+增量片表
 * 的编辑不复制缓冲。随机混合编辑 StringBuilder oracle；
 * 尾插/切分/跨片段删除钉住；addedLength 审计；fail-fast。
 */
class PieceTableTest {

    @Test
    void randomEditsMatchStringBuilderOracle() {
        Random rng = new Random(6010L);
        PieceTable table = new PieceTable("seed document text");
        StringBuilder oracle = new StringBuilder("seed document text");
        long insertedChars = 0;
        for (int i = 0; i < 500; i++) {
            if (rng.nextBoolean() || oracle.length() < 6) {
                int pos = rng.nextInt(oracle.length() + 1);
                String s = String.valueOf((char) ('a' + rng.nextInt(4)))
                        + (rng.nextBoolean() ? "z" : "");
                table.insert(pos, s);
                oracle.insert(pos, s);
                insertedChars += s.length();
            } else {
                int dpos = rng.nextInt(oracle.length());
                int maxLen = Math.min(4, oracle.length() - dpos);
                int len = 1 + rng.nextInt(maxLen);
                table.delete(dpos, len);
                oracle.delete(dpos, dpos + len);
            }
            if (i % 50 == 0) {
                assertThat(table.text()).as("step %d", i).isEqualTo(oracle.toString());
                assertThat(table.length()).isEqualTo(oracle.length());
            }
        }
        assertThat(table.text()).isEqualTo(oracle.toString());
        assertThat(table.addedLength()).as("增量缓冲=累计插入字符").isEqualTo((int) insertedChars);
    }

    @Test
    void appendKeepsSinglePiecePerInsert() {
        PieceTable table = new PieceTable("abc");
        assertThat(table.pieceCount()).isEqualTo(1);
        table.insert(3, "d");
        assertThat(table.pieceCount()).isEqualTo(2);
        assertThat(table.text()).isEqualTo("abcd");
        table.insert(4, "e");
        assertThat(table.pieceCount()).isEqualTo(3);
        assertThat(table.text()).isEqualTo("abcde");
    }

    @Test
    void middleInsertSplitsPiece() {
        PieceTable table = new PieceTable("hello world");
        table.insert(5, "X");
        assertThat(table.text()).isEqualTo("helloX world");
        assertThat(table.pieceCount()).isEqualTo(3);
        table.insert(6, "Y");
        assertThat(table.text()).isEqualTo("helloXY world");
    }

    @Test
    void deleteAcrossPieceBoundaries() {
        PieceTable table = new PieceTable("hello world");
        table.insert(5, "XY");
        assertThat(table.text()).isEqualTo("helloXY world");
        table.delete(3, 5);
        assertThat(table.text()).isEqualTo("helworld");
        table.delete(0, table.length());
        assertThat(table.text()).isEmpty();
        assertThat(table.pieceCount()).isZero();
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> new PieceTable(null)).isInstanceOf(IllegalArgumentException.class);
        PieceTable table = new PieceTable("abc");
        assertThatThrownBy(() -> table.insert(2, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> table.insert(4, "x")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> table.insert(-1, "x")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> table.delete(1, 3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> table.delete(0, -1)).isInstanceOf(IllegalArgumentException.class);
        table.delete(1, 0);
        assertThat(table.text()).isEqualTo("abc");
    }
}
