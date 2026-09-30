package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.Random;
import java.util.function.ToDoubleFunction;

/**
 * 模拟退火（spec 9044 / W9089 / impl 2397）——Kirkpatrick 1983
 * 思想（Metropolis 准则 + 几何降温——VLSI 布局/NP-hard 启发式
 * 同源）：**劣解按 exp(−Δ/T) 概率接受（高温大步跳出局部凹坑、
 * 低温逐步收敛），全程 best-ever 记忆**——纯贪心陷局部最优
 * （单向下山无逃逸）与随机重启（无温度结构）的病解。几何降温
 * T₀→T_end 跨迭代均摊；高斯扰动提议；种子化确定可回放；
 * best-ever 恒不劣于当前态；null/参数越域 fail-fast。
 *
 * <p>与 MetropolisHastings（experiment 域）同根不同面：分布
 * 采样 vs 最优化；与 SimulatedAnnealing 的 TspTwoOpt（spec
 * 9045）衔接：单点连续优化 vs 组合结构优化。
 */
public final class SimulatedAnnealing {

    private SimulatedAnnealing() {
    }

    /**
     * 最小化（energy 能量函数；start 起点；返回 best-ever 解）。
     *
     * @throws IllegalArgumentException null/步长/温度/次数越域
     */
    public static double[] minimize(ToDoubleFunction<double[]> energy, double[] start,
                                    double stepSize, double tempStart, double tempEnd,
                                    int iterations, Random random) {
        if (energy == null || start == null || start.length == 0 || random == null) {
            throw new IllegalArgumentException("能量函数/起点/随机源非空");
        }
        if (stepSize <= 0 || Double.isNaN(stepSize)) {
            throw new IllegalArgumentException("步长为正（实际 " + stepSize + "）");
        }
        if (!(tempStart > tempEnd) || tempEnd <= 0 || Double.isNaN(tempStart)) {
            throw new IllegalArgumentException("温度 0<T_end<T_start（实际 " + tempStart + "," + tempEnd + "）");
        }
        if (iterations < 1) {
            throw new IllegalArgumentException("迭代为正（实际 " + iterations + "）");
        }
        double[] current = start.clone();
        double currentEnergy = energy.applyAsDouble(current);
        double[] best = current.clone();
        double bestEnergy = currentEnergy;
        double cooling = Math.pow(tempEnd / tempStart, 1.0 / iterations);
        double temperature = tempStart;
        for (int i = 0; i < iterations; i++) {
            double[] candidate = current.clone();
            int dim = random.nextInt(candidate.length);
            candidate[dim] += stepSize * random.nextGaussian();
            double candidateEnergy = energy.applyAsDouble(candidate);
            double delta = candidateEnergy - currentEnergy;
            if (delta <= 0 || random.nextDouble() < Math.exp(-delta / temperature)) {
                current = candidate;
                currentEnergy = candidateEnergy;
                if (currentEnergy < bestEnergy) {
                    best = current.clone();
                    bestEnergy = currentEnergy;
                }
            }
            temperature *= cooling;
        }
        return best;
    }
}
