package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 消息序列连续性审计（spec 711 / T1022，Kafka offset 审计思想）：对任意
 * store 的消息投影（turnSeq,seqInTurn 二元组序列）做单遍连续性判定——
 * store 级丢数据/重复写入在读取侧表现为「上下文缺一段」，本面把它变成
 * 结构化证据（GAP/DUPLICATE/OUT_OF_ORDER）。
 *
 * <p>纯函数不接 store SPI：宿主对 dump 或读出结果投影即可跑（341 StoreFsck
 * 不覆盖消息层——四类检查之外的对偶面）。期望形态：turn 升序、turn 内 seq
 * 从 0 连续、turn 序从首见连续递增。单遍 O(n)；重复检测用 long 编码 set。
 */
public final class TurnSequenceAudit {

    /** 序对标记（调用方从消息投影——解耦 store SPI）。 */
    public record Marker(int turnSeq, int seqInTurn) {
    }

    /** 单条发现（kind ∈ GAP / DUPLICATE / OUT_OF_ORDER）。 */
    public record Finding(String kind, int turnSeq, int seqInTurn) {
    }

    private TurnSequenceAudit() {
    }

    /** 审计（null fail-fast；空表 = 零发现）。 */
    public static List<Finding> audit(List<Marker> markers) {
        Objects.requireNonNull(markers, "markers");
        List<Finding> findings = new ArrayList<>();
        if (markers.isEmpty()) {
            return findings;
        }
        Set<Long> seen = new HashSet<>();
        int expectedTurn = -1;
        int expectedSeq = 0;
        long lastEncoded = Long.MIN_VALUE;
        for (Marker marker : markers) {
            Objects.requireNonNull(marker, "marker");
            long encoded = ((long) marker.turnSeq() << 32) | (marker.seqInTurn() & 0xFFFFFFFFL);
            if (!seen.add(encoded)) {
                findings.add(new Finding("DUPLICATE", marker.turnSeq(), marker.seqInTurn()));
                continue;
            }
            if (encoded < lastEncoded) {
                findings.add(new Finding("OUT_OF_ORDER", marker.turnSeq(), marker.seqInTurn()));
                continue;
            }
            lastEncoded = encoded;
            if (expectedTurn == -1) {
                // 首见：约定从 (0,0) 开始
                if (marker.turnSeq() != 0 || marker.seqInTurn() != 0) {
                    findings.add(new Finding("GAP", marker.turnSeq(), marker.seqInTurn()));
                }
            } else if (marker.turnSeq() == expectedTurn) {
                if (marker.seqInTurn() != expectedSeq) {
                    findings.add(new Finding("GAP", marker.turnSeq(), marker.seqInTurn()));
                }
            } else if (marker.turnSeq() > expectedTurn) {
                // 跨 turn：新 turn 必须恰好是 expectedTurn+1 且 seq 从 0 起
                if (marker.turnSeq() != expectedTurn + 1 || marker.seqInTurn() != 0) {
                    findings.add(new Finding("GAP", marker.turnSeq(), marker.seqInTurn()));
                }
            } else {
                // 小于 expectedTurn 的未见序对——缺号后补到（乱序缺号同报 GAP）
                findings.add(new Finding("GAP", marker.turnSeq(), marker.seqInTurn()));
            }
            // 期望推进：同 turn seq+1；异常发现后重同步到当前序对之后（后续照常判定）
            expectedTurn = marker.turnSeq();
            expectedSeq = marker.seqInTurn() + 1;
        }
        return List.copyOf(findings);
    }
}
