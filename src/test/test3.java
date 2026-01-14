package test;

import algorithms.pivotselection.FarthestFirstTraversalSelection;
import algorithms.pivotselection.PivotSelectionMethod;
import db.MetricData;
import db.VectorData;
import dataload.VectorReader;
import distance_function.LDistance;
import distance_function.MetricDistance;
import index.CompletePartitionTree;
import index.PivotTable;

import java.util.ArrayList;
import java.util.List;

public class test3 {

    private static final String DATASET_PATH = "src\\dataset\\uniformvector-20dim-1m.txt";
    private static final int DIMENSION = 20;
    private static final int DATA_COUNT = 10000;

    private static final int MAX_LEAF_SIZE = 800;
    private static final int LEAF_PIVOTS = 3;
    private static final int INTERNAL_PIVOTS = 3;
    private static final int NUM_REGIONS = 3;

    /** 统计结果封装 */
    private static class CountResult {
        int internalPivotCount = 0;
        int leafPivotCount = 0;
        int leafDataCount = 0;

        int total() {
            return internalPivotCount + leafPivotCount + leafDataCount;
        }
    }

    public static void main(String[] args) throws Exception {

        System.out.println("=== CPT 构建完整性测试（test3）===");

        /* 1. 加载数据 */
        VectorReader reader = new VectorReader();
        List<VectorData> vectors = reader.load(DATASET_PATH, DIMENSION, DATA_COUNT);
        List<MetricData> dataset = new ArrayList<>(vectors);

        System.out.println("原始数据量: " + dataset.size());

        /* 2. 构建 CPT */
        MetricDistance distance = new LDistance(1.0);
        PivotSelectionMethod pivotSelector = new FarthestFirstTraversalSelection();

        Object root = CompletePartitionTree.CPBulkLoad(
                dataset,
                MAX_LEAF_SIZE,
                distance,
                pivotSelector,
                LEAF_PIVOTS,
                NUM_REGIONS,
                INTERNAL_PIVOTS
        );

        System.out.println("CPT 构建完成");

        /* 3. 统计索引中数据条目 */
        CountResult result = new CountResult();
        traverseCPT(root, result);

        /* 4. 输出统计结果 */
        System.out.println("========== 统计结果 ==========");
        System.out.println("内部节点 Pivot 数量: " + result.internalPivotCount);
        System.out.println("叶子节点 Pivot 数量: " + result.leafPivotCount);
        System.out.println("叶子节点数据数量:   " + result.leafDataCount);
        System.out.println("--------------------------------");
        System.out.println("索引中数据总量:     " + result.total());
        System.out.println("原始数据总量:       " + dataset.size());
        System.out.println("差值:               " + (result.total() - dataset.size()));

        if (result.total() == dataset.size()) {
            System.out.println("✅ CPT 构建正确：数据未丢失、未重复");
        } else {
            System.err.println("❌ CPT 构建异常：数据数量不一致！");
        }
    }

    /**
     * 递归遍历 CPT
     */
    private static void traverseCPT(Object node, CountResult result) {

        if (node == null) return;

        // 叶子节点（与 MVPT / CGHT 共用 PivotTable）
        if (node instanceof PivotTable) {
            PivotTable table = (PivotTable) node;

            result.leafPivotCount += table.getPivots().size();
            result.leafDataCount += table.getDataPoints().size();
            return;
        }

        /*
         * CPT 内部节点
         * ⚠️ 假设内部节点类名为 CPInternalNode
         * 如果你的实际类名不同，只需替换这里
         */
        if (node instanceof CompletePartitionTree.CPTInternalNode) {
            CompletePartitionTree.CPTInternalNode internal =
                    (CompletePartitionTree.CPTInternalNode) node;

            result.internalPivotCount += internal.pivots.size();

            for (Object child : internal.children) {
                traverseCPT(child, result);
            }
            return;
        }

        throw new IllegalStateException("未知 CPT 节点类型: " + node.getClass());
    }
}
