package test;

import algorithms.pivotselection.FarthestFirstTraversalSelection;
import algorithms.pivotselection.PivotSelectionMethod;
import db.MetricData;
import db.VectorData;
import dataload.VectorReader;
import distance_function.LDistance;
import distance_function.MetricDistance;
import index.PivotTable;
import search.PTRangeSearch;
import search.SearchResult;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * PivotTable 基准查询测试（Ground Truth）
 * 使用 PTRangeSearch 封装的查询逻辑
 */
public class TestPivotTableBaseline {

    // ===== 与 Demo4 完全一致的参数 =====
    private static final String DATASET_PATH = "src\\dataset\\uniformvector-20dim-1m.txt";
    private static final int DIMENSION = 10;
    private static final int DATA_COUNT = 10000;
    private static final int QUERY_COUNT = 100;
    private static final double QUERY_RADIUS = 0.5;

    private static final int MAX_LEAF_SIZE = 50;
    private static final int LEAF_PIVOTS = 3;

    public static void main(String[] args) throws Exception {

        System.out.println("=== PivotTable 基准查询测试开始 ===");
        System.out.println("时间: " + new Date());

        /* 1. 加载数据 */
        VectorReader reader = new VectorReader();
        List<VectorData> vectors = reader.load(DATASET_PATH, DIMENSION, DATA_COUNT);
        List<MetricData> dataset = new ArrayList<>(vectors);

        System.out.println("加载数据量: " + dataset.size());

        /* 2. 构建 PivotTable（单一叶子节点） */
        MetricDistance distance = new LDistance(1.0);
        PivotSelectionMethod pivotSelector = new FarthestFirstTraversalSelection();

        long buildStart = System.currentTimeMillis();
        PivotTable table = new PivotTable(
                dataset,
                MAX_LEAF_SIZE,
                LEAF_PIVOTS,
                pivotSelector,
                distance
        );
        long buildEnd = System.currentTimeMillis();

        System.out.println("PivotTable 构建完成，耗时: " + (buildEnd - buildStart) + " ms");

        /* 3. 准备查询点 */
        List<MetricData> queryPoints = new ArrayList<>();
        int actualQueryCount = Math.min(QUERY_COUNT, dataset.size());
        for (int i = 0; i < actualQueryCount; i++) {
            queryPoints.add(dataset.get(i));
        }
        System.out.println("查询点数量: " + queryPoints.size());

        /* 4. 执行查询 */
        double totalQueryTime = 0;
        double totalResultCount = 0;
        double totalDistanceCount = 0;

        for (int i = 0; i < queryPoints.size(); i++) {
            MetricData query = queryPoints.get(i);

            distance.resetCount();
            long start = System.currentTimeMillis();

            // 使用 PTRangeSearch 查询 PivotTable
            SearchResult result = PTRangeSearch.rangeSearch(table, query, QUERY_RADIUS, distance);

            long end = System.currentTimeMillis();
            totalQueryTime += (end - start);
            totalResultCount += result.results.size();
            totalDistanceCount += result.distanceCount;

            if ((i + 1) % 10 == 0) {
                System.out.println("查询进度: " + (i + 1) + "/" + queryPoints.size()
                        + " | 结果数: " + result.results.size());
            }
        }

        /* 5. 输出统计结果 */
        System.out.println("========== PivotTable 基准结果 ==========");
        System.out.println("平均查询时间(ms): " + totalQueryTime / queryPoints.size());
        System.out.println("平均结果数量:     " + totalResultCount / queryPoints.size());
        System.out.println("平均距离计算次数: " + totalDistanceCount / queryPoints.size());
        System.out.println("========================================");
    }
}
