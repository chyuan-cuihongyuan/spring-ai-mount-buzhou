package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.time.Duration;

public record McpServerBinding(String name, String transport, String endpoint, Duration timeout) {
}
