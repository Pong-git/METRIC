package test;

import algorithms.pivotselection.FarthestFirstTraversalSelection;
import algorithms.pivotselection.PivotSelectionMethod;
import db.MetricData;
import db.VectorData;
import dataload.VectorReader;
import distance_function.LDistance;
import distance_function.MetricDistance;
import index.CompleteGeneralHyperPlaneTree;
import index.PivotTable;

import java.util.ArrayList;
import java.util.List;

public class test2 {

    private static final String DATASET_PATH = "src\\dataset\\uniformvector-20dim-1m.txt";
    private static final int DIMENSION = 20;
    private static final int DATA_COUNT = 100000;

    private static final int MAX_LEAF_SIZE = 50;
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

        System.out.println("=== CGHT 构建完整性测试（test2）===");

        /* 1. 加载数据 */
        VectorReader reader = new VectorReader();
        List<VectorData> vectors = reader.load(DATASET_PATH, DIMENSION, DATA_COUNT);
        List<MetricData> dataset = new ArrayList<>(vectors);

        System.out.println("原始数据量: " + dataset.size());

        /* 2. 构建 CGHT */
        MetricDistance distance = new LDistance(1.0);
        PivotSelectionMethod pivotSelector = new FarthestFirstTraversalSelection();

        Object root = CompleteGeneralHyperPlaneTree.CGHBulkLoad(
                dataset,
                MAX_LEAF_SIZE,
                distance,
                pivotSelector,
                LEAF_PIVOTS,
                NUM_REGIONS,
                INTERNAL_PIVOTS
        );

        System.out.println("CGHT 构建完成");

        /* 3. 统计索引中数据条目 */
        CountResult result = new CountResult();
        traverseCGHT(root, result);

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
            System.out.println("✅ CGHT 构建正确：数据未丢失、未重复");
        } else {
            System.err.println("❌ CGHT 构建异常：数据数量不一致！");
        }
    }

    /**
     * 递归遍历 CGHT
     */
    private static void traverseCGHT(Object node, CountResult result) {

        if (node == null) return;

        // 叶子节点（与你的 MVPT 共用 PivotTable）
        if (node instanceof PivotTable) {
            PivotTable table = (PivotTable) node;

            result.leafPivotCount += table.getPivots().size();
            result.leafDataCount += table.getDataPoints().size();
            return;
        }

        /*
         * CGHT 的内部节点：
         * ⚠️ 这里假设 CGHT 内部节点类名是 CGHInternalNode
         * 如果你的实际类名不同，只需要改 instanceof 和字段访问即可
         */
        if (node instanceof CompleteGeneralHyperPlaneTree.CGHTInternalNode) {
            CompleteGeneralHyperPlaneTree.CGHTInternalNode internal =
                    (CompleteGeneralHyperPlaneTree.CGHTInternalNode) node;

            result.internalPivotCount += internal.pivots.size();

            for (Object child : internal.children) {
                traverseCGHT(child, result);
            }
            return;
        }

        throw new IllegalStateException("未知 CGHT 节点类型: " + node.getClass());
    }
}
