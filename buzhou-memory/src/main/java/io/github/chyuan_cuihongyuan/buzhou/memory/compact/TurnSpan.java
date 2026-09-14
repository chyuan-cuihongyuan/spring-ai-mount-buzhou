package io.github.chyuan_cuihongyuan.buzhou.memory.compact;
/** spec 1529 / T2309：轮次区间载体——轮号与轮内序对的定位元组（微压缩的回收定位粒度）。 */

public record TurnSpan(int turnIndex, int startMessageOffset, int endMessageOffset, boolean completed) {
}
