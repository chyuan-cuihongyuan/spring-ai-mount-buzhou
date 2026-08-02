package io.github.chyuan_cuihongyuan.buzhou.memory.compact;

public record MicroCompactionPolicy(boolean neverCompress, int maxAgeTurns, int minSizeChars) {

    public static MicroCompactionPolicy defaults() {
        return new MicroCompactionPolicy(false, 3, 200);
    }
}
