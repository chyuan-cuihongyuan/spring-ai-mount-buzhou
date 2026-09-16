package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.LastWriteWinsRegister.Timestamped;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2006 / T3114：LWW 寄存器合同——单调时间戳采纳、迟到旧写拒并
 * 计数、ts 平局 writerId 字典序仲裁、完全幂等、merge 同语义、畸形
 * fail-fast。
 */
class LastWriteWinsRegisterTest {

    @Test
    void monotonicallyNewerTimestampsShouldWin() {
        LastWriteWinsRegister<String> register = new LastWriteWinsRegister<>();
        assertThat(register.put("a", 100, "w1")).isTrue();
        assertThat(register.put("b", 200, "w1")).isTrue();
        assertThat(register.put("c", 300, "w2")).isTrue();
        assertThat(register.current().value()).isEqualTo("c");
        assertThat(register.current().timestamp()).isEqualTo(300);
        assertThat(register.supersededCount()).isZero();
        assertThat(register.conflictCount()).isZero();
    }

    @Test
    void lateOlderWriteShouldBeRejectedAndCounted() {
        LastWriteWinsRegister<String> register = new LastWriteWinsRegister<>();
        register.put("new", 200, "w1");
        assertThat(register.put("stale", 100, "w2")).isFalse(); // 迟到旧写
        assertThat(register.current().value()).isEqualTo("new"); // 现值不动
        assertThat(register.supersededCount()).isEqualTo(1L);
    }

    @Test
    void timestampTieShouldBreakByWriterIdLexicographic() {
        LastWriteWinsRegister<String> register = new LastWriteWinsRegister<>();
        register.put("from-a", 500, "writer-a");
        // 同 ts、writer 字典序更大 → 胜（确定性仲裁）+ 冲突计数
        assertThat(register.put("from-b", 500, "writer-b")).isTrue();
        assertThat(register.current().value()).isEqualTo("from-b");
        assertThat(register.conflictCount()).isEqualTo(1L);
        // 字典序更小 → 落败（计入 superseded）
        assertThat(register.put("from-a2", 500, "writer-a")).isFalse();
        assertThat(register.conflictCount()).isEqualTo(1L);
        assertThat(register.supersededCount()).isEqualTo(1L);
    }

    @Test
    void identicalWriteShouldBeIdempotentWithoutConflict() {
        LastWriteWinsRegister<String> register = new LastWriteWinsRegister<>();
        register.put("same", 100, "w1");
        assertThat(register.put("same", 100, "w1")).isTrue(); // 幂等
        assertThat(register.conflictCount()).isZero();
        assertThat(register.supersededCount()).isZero();
    }

    @Test
    void sameWriterSameTimestampDifferentValueShouldCountConflictAndKeepFirst() {
        LastWriteWinsRegister<String> register = new LastWriteWinsRegister<>();
        register.put("first", 100, "w1");
        assertThat(register.put("second", 100, "w1")).isFalse(); // 不可判定→first-wins
        assertThat(register.current().value()).isEqualTo("first");
        assertThat(register.conflictCount()).isEqualTo(1L);
    }

    @Test
    void mergeShouldFollowSameSemantics() {
        LastWriteWinsRegister<Integer> a = new LastWriteWinsRegister<>();
        a.put(1, 100, "node-a");
        LastWriteWinsRegister<Integer> b = new LastWriteWinsRegister<>();
        b.put(2, 150, "node-b");
        assertThat(a.merge(b.current())).isTrue();
        assertThat(a.current().value()).isEqualTo(2);
        // 合并回旧值：拒
        assertThat(b.merge(new Timestamped<>(1, 100, "node-a"))).isFalse();
        assertThat(b.current().value()).isEqualTo(2); // LWW 收敛：两端终值一致
    }

    @Test
    void emptyRegisterCurrentShouldBeNull() {
        LastWriteWinsRegister<String> register = new LastWriteWinsRegister<>();
        assertThat(register.current()).isNull();
    }

    @Test
    void malformedInputsShouldFailFast() {
        LastWriteWinsRegister<String> register = new LastWriteWinsRegister<>();
        assertThatThrownBy(() -> register.put(null, 1, "w1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> register.put("v", 1, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> register.put("v", -1, "w1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> register.merge(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
