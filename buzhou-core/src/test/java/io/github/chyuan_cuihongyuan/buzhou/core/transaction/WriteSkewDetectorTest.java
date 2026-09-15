package io.github.chyuan_cuihongyuan.buzhou.core.transaction;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1861 / T2924：写偏斜——经典医生反例、写冲突放行、全对扫描。 */
class WriteSkewDetectorTest {

    /** 经典「值班医生」：双读班表、各写自己的请假行——读交写不交即风险。 */
    @Test
    void classicDoctorsOnCallIsRisk() {
        WriteSkewDetector.Transaction doctorA = new WriteSkewDetector.Transaction(
                "a", Set.of("oncall-count"), Set.of("doctor-a-row"));
        WriteSkewDetector.Transaction doctorB = new WriteSkewDetector.Transaction(
                "b", Set.of("oncall-count"), Set.of("doctor-b-row"));
        assertThat(WriteSkewDetector.skewRisk(doctorA, doctorB)).isTrue();
    }

    /** 写集相交（同键竞争）非偏斜——写冲突检测本来就能拦。 */
    @Test
    void writeConflictIsNotSkew() {
        WriteSkewDetector.Transaction a = new WriteSkewDetector.Transaction(
                "a", Set.of("x"), Set.of("shared-row"));
        WriteSkewDetector.Transaction b = new WriteSkewDetector.Transaction(
                "b", Set.of("x"), Set.of("shared-row"));
        assertThat(WriteSkewDetector.skewRisk(a, b)).isFalse();
    }

    /** 读集不相交或读写集残缺——无组合不变量可言，非风险。 */
    @Test
    void disjointReadsOrDegenerateSetsAreNotRisk() {
        WriteSkewDetector.Transaction disjointReads =
                new WriteSkewDetector.Transaction("a", Set.of("x"), Set.of("row-a"));
        WriteSkewDetector.Transaction otherScope =
                new WriteSkewDetector.Transaction("b", Set.of("y"), Set.of("row-b"));
        assertThat(WriteSkewDetector.skewRisk(disjointReads, otherScope)).isFalse();

        WriteSkewDetector.Transaction readOnly = new WriteSkewDetector.Transaction(
                "c", Set.of("x"), Set.of());
        WriteSkewDetector.Transaction writer = new WriteSkewDetector.Transaction(
                "d", Set.of("x"), Set.of("row-d"));
        assertThat(WriteSkewDetector.skewRisk(readOnly, writer)).isFalse();
    }

    /** 全对扫描：3 事务中 1 对风险；null/空零输出。 */
    @Test
    void scanFindsAllRiskPairs() {
        WriteSkewDetector.Transaction a = new WriteSkewDetector.Transaction(
                "a", Set.of("s"), Set.of("row-a"));
        WriteSkewDetector.Transaction b = new WriteSkewDetector.Transaction(
                "b", Set.of("s"), Set.of("row-b"));
        WriteSkewDetector.Transaction c = new WriteSkewDetector.Transaction(
                "c", Set.of("other"), Set.of("row-c"));
        List<WriteSkewDetector.SkewPair> risks =
                WriteSkewDetector.scan(List.of(a, b, c));
        assertThat(risks).hasSize(1);
        assertThat(risks.get(0)).isEqualTo(new WriteSkewDetector.SkewPair("a", "b"));
        assertThat(WriteSkewDetector.scan(null)).isEmpty();
    }

    /** 畸形入参 fail-fast：空白 id、null 读写集。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> new WriteSkewDetector.Transaction(
                " ", Set.of(), Set.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("非法事务事实");
        assertThatThrownBy(() -> new WriteSkewDetector.Transaction(
                "a", null, Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
