package ir.ac.kntu.domain.report;

/**
 * Immutable financial metrics summary encapsulating cash inflows, outflows, and fees.
 */
public class FinancialSummary {
    private final double totalInflow;
    private final double totalOutflow;
    private final double totalFees;
    private final int recordCount;

    public FinancialSummary(double totalInflow, double totalOutflow, double totalFees, int recordCount) {
        this.totalInflow = totalInflow;
        this.totalOutflow = totalOutflow;
        this.totalFees = totalFees;
        this.recordCount = recordCount;
    }

    public double getTotalInflow() {
        return totalInflow;
    }

    public double getTotalOutflow() {
        return totalOutflow;
    }

    public double getTotalFees() {
        return totalFees;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public double getNetCashFlow() {
        return totalInflow - totalOutflow - totalFees;
    }
}