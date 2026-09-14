package io.github.chyuan_cuihongyuan.buzhou.memory.budget;
/** spec 1529 / T2309：预算计算 SPI——分层预算的分配契约。 */

public interface BudgetCalculator {

    BudgetReport evaluate(BudgetInput input);
}
