package io.github.chyuan_cuihongyuan.buzhou.spill;

import java.time.Instant;

public record SpillEntry(SpillUri uri, String content, String contentType,
                         long sizeChars, Instant createdAt) {

    public static SpillEntry of(SpillUri uri, String content) {
        return new SpillEntry(uri, content, "text/plain",
                content == null ? 0 : content.length(), Instant.now());
    }
}
