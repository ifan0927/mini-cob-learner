package com.minicob.cob;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

/**
 * Splits the loans to close into fixed-size partitions. Each partition carries its own list of loan
 * ids; the grid size is ignored, as in Fineract's {@code LoanCOBPartitioner}.
 */
public class LoanCobPartitioner implements Partitioner {

    public static final String LOAN_IDS = "loanIds";

    private final CobLoanRepository repository;
    private final LocalDate cobDate;
    private final int partitionSize;

    public LoanCobPartitioner(CobLoanRepository repository, LocalDate cobDate, int partitionSize) {
        this.repository = repository;
        this.cobDate = cobDate;
        this.partitionSize = partitionSize;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        List<Long> ids = repository.findIdsToClose(cobDate);
        Map<String, ExecutionContext> partitions = new LinkedHashMap<>();
        for (int from = 0, n = 0; from < ids.size(); from += partitionSize, n++) {
            ExecutionContext context = new ExecutionContext();
            context.put(LOAN_IDS, new ArrayList<>(ids.subList(from, Math.min(from + partitionSize, ids.size()))));
            partitions.put("partition" + n, context);
        }
        return partitions;
    }

    @SuppressWarnings("unchecked")
    public static List<Long> loanIdsOf(ExecutionContext context) {
        Object ids = context.get(LOAN_IDS);
        return ids == null ? List.of() : (List<Long>) ids;
    }
}
