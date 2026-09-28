package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import java.util.Deque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6033：ElevatorScan 合同——LOOK 扫掠服务序。
 * 上行/下行折返；磁头随服务移动+方向翻转；重复幂等；
 * fail-fast。
 */
class ElevatorScanTest {

    @Test
    void upwardScanServicesAscendingThenWrapsDown() {
        ElevatorScan elevator = new ElevatorScan(50, 199, ElevatorScan.Direction.UP);
        for (long track : new long[]{82, 170, 43, 140, 24, 16, 190}) {
            elevator.add(track);
        }
        Deque<Long> order = elevator.serveAll();
        assertThat(order).containsExactly(82L, 140L, 170L, 190L, 43L, 24L, 16L);
        assertThat(elevator.head()).isEqualTo(16);
        assertThat(elevator.direction()).isEqualTo(ElevatorScan.Direction.DOWN);
        assertThat(elevator.pendingCount()).isZero();
    }

    @Test
    void downwardScanServicesDescendingThenWrapsUp() {
        ElevatorScan elevator = new ElevatorScan(60, 199, ElevatorScan.Direction.DOWN);
        for (long track : new long[]{82, 170, 43, 140, 24}) {
            elevator.add(track);
        }
        Deque<Long> order = elevator.serveAll();
        assertThat(order).containsExactly(43L, 24L, 82L, 140L, 170L);
        assertThat(elevator.head()).isEqualTo(170);
        assertThat(elevator.direction()).isEqualTo(ElevatorScan.Direction.UP);
    }

    @Test
    void headItselfRequestedServedFirst() {
        ElevatorScan elevator = new ElevatorScan(50, 100, ElevatorScan.Direction.UP);
        elevator.add(50);
        elevator.add(60);
        assertThat(elevator.serveAll()).containsExactly(50L, 60L);
    }

    @Test
    void duplicateRequestsAreIdempotent() {
        ElevatorScan elevator = new ElevatorScan(10, 99, ElevatorScan.Direction.UP);
        elevator.add(20);
        elevator.add(20);
        assertThat(elevator.pendingCount()).isEqualTo(1);
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> new ElevatorScan(5, -1, ElevatorScan.Direction.UP))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ElevatorScan(200, 100, ElevatorScan.Direction.UP))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ElevatorScan(0, 100, null))
                .isInstanceOf(IllegalArgumentException.class);
        ElevatorScan elevator = new ElevatorScan(0, 100, ElevatorScan.Direction.UP);
        assertThatThrownBy(() -> elevator.add(101)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> elevator.add(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
