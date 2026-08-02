package io.github.chyuan_cuihongyuan.buzhou.memory.compact;

public record TurnSpan(int turnIndex, int startMessageOffset, int endMessageOffset, boolean completed) {
}
