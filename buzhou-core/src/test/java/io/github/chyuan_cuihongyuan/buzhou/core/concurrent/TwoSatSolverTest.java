package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 10031 / X10063：TwoSatSolver 合同验证——强制赋值/经典 UNSAT 手锚
 * +植入解随机式全满足圣像+空式平凡 SAT+确定性+fail-fast。
 * 字面量口径：1 起址有符号——+v 真、−v 假。
 */
class TwoSatSolverTest {

    /** 赋值是否满足全部子句（合同判定核心谓词）。 */
    private static boolean satisfies(boolean[] assignment, List<int[]> clauses) {
        for (int[] clause : clauses) {
            boolean a = assignment[Math.abs(clause[0]) - 1] == (clause[0] > 0);
            boolean b = assignment[Math.abs(clause[1]) - 1] == (clause[1] > 0);
            if (!a && !b) {
                return false;
            }
        }
        return true;
    }

    @Test
    void shouldForceLiteralValues_whenImplicationChainFormula() {
        // (x1) ∧ (¬x1∨x2)：x1 强制真，x2 随之强制真
        List<int[]> clauses = List.of(
                new int[]{1, 1},
                new int[]{-1, 2});
        Optional<boolean[]> solution = TwoSatSolver.solve(2, clauses);
        assertThat(solution).isPresent();
        assertThat(solution.get()[0]).isTrue();
        assertThat(solution.get()[1]).isTrue();
    }

    @Test
    void shouldReturnEmpty_whenClassicUnsatFormula() {
        // (x1∨x2)∧(¬x1∨x2)∧(x1∨¬x2)∧(¬x1∨¬x2)：x1↔x2↔¬x1 矛盾
        List<int[]> clauses = List.of(
                new int[]{1, 2}, new int[]{-1, 2},
                new int[]{1, -2}, new int[]{-1, -2});
        assertThat(TwoSatSolver.solve(2, clauses)).isEmpty();
    }

    @Test
    void shouldSatisfyAllClauses_whenPlantedSolutionRandomFormulas() {
        Random random = new Random(10031L);
        for (int trial = 0; trial < 50; trial++) {
            int n = 12;
            boolean[] planted = new boolean[n];
            for (int v = 0; v < n; v++) {
                planted[v] = random.nextBoolean();
            }
            List<int[]> clauses = new ArrayList<>();
            for (int c = 0; c < 30; c++) {
                int a = 1 + random.nextInt(n);
                int b = 1 + random.nextInt(n);
                boolean aSign = planted[a - 1] || random.nextBoolean();
                boolean bSign = planted[b - 1] || random.nextBoolean();
                if (!aSign && !bSign) {
                    if (random.nextBoolean()) {
                        aSign = true;
                    } else {
                        bSign = true;
                    }
                }
                clauses.add(new int[]{aSign ? a : -a, bSign ? b : -b});
            }
            Optional<boolean[]> solution = TwoSatSolver.solve(n, clauses);
            assertThat(solution).as("植入解式 %d 必可满足", trial).isPresent();
            assertThat(satisfies(solution.get(), clauses))
                    .as("植入解式 %d 返回赋值全满足", trial).isTrue();
        }
    }

    @Test
    void shouldReturnTrivialAssignment_whenEmptyFormula() {
        Optional<boolean[]> solution = TwoSatSolver.solve(3, List.of());
        assertThat(solution).isPresent();
        assertThat(solution.get()).hasSize(3);
    }

    @Test
    void shouldReproduceIdenticalAssignment_whenSameInputTwice() {
        List<int[]> clauses = List.of(
                new int[]{1, 2}, new int[]{2, 3}, new int[]{-1, -3});
        Optional<boolean[]> first = TwoSatSolver.solve(3, clauses);
        Optional<boolean[]> second = TwoSatSolver.solve(3, clauses);
        assertThat(first).isPresent();
        assertThat(second).isPresent();
        assertThat(second.get()).isEqualTo(first.get());
    }

    @Test
    void shouldFailFast_whenNullClausesOrOutOfRangeLiterals() {
        assertThatThrownBy(() -> TwoSatSolver.solve(2, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TwoSatSolver.solve(2, List.of(new int[]{0, 2})))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("越界");
        assertThatThrownBy(() -> TwoSatSolver.solve(2, List.of(new int[]{1, 3})))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("越界");
        assertThatThrownBy(() -> TwoSatSolver.solve(2, List.of(new int[]{1})))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("int[2]");
        assertThatThrownBy(() -> TwoSatSolver.solve(0, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
