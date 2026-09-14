package io.github.chyuan_cuihongyuan.buzhou.memory.compact;
/** spec 1529 / T2309：微压缩策略——evict-ratio 等回收参数（yml 可配）。 */

public record MicroCompactionPolicy(boolean neverCompress, int maxAgeTurns, int minSizeChars) {

    public static MicroCompactionPolicy defaults() {
        return new MicroCompactionPolicy(false, 3, 200);
    }
}
