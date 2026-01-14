package test;

import db.MetricData;
import db.VectorData;
import distance_function.LDistance;
import distance_function.MetricDistance;
import algorithms.pivotselection.FarthestFirstTraversalSelection;
import algorithms.pivotselection.PivotSelectionMethod;
import index.*;
import search.MVPRangeSearch;
import search.SearchResult;
import dataload.VectorReader;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * 批量测试MVPT、CGHT和CPT索引性能
 */
public class Demo4 {

    // 配置参数
    private static final String DATASET_PATH = "src\\dataset\\uniformvector-20dim-1m.txt";
    private static final int[] DIMENSIONS = {2, 5, 10, 20};
    private static final int DATA_COUNT = 10000;
    private static final int QUERY_COUNT = 100;
    private static final double QUERY_RADIUS = 0.5;
    private static final int MAX_LEAF_SIZE = 50;
    private static final int LEAF_PIVOTS = 3;
    private static final int INTERNAL_PIVOTS = 3;
    private static final int NUM_REGIONS = 3;
    private static final String OUTPUT_FILE = "index_performance_report.txt";
    
    // 随机数生成器（用于选择查询点）
    private static final Random RANDOM = new Random(42); // 固定种子确保可重复性

    /**
     * 测试结果封装类
     */
    private static class TestResult {
        String indexName;
        int dimension;
        double buildTime;           // 构建时间（毫秒）
        double avgQueryTime;        // 平均查询时间（毫秒）
        double avgResultCount;      // 平均结果数量
        double avgDistanceCount;    // 平均距离计算次数
        double totalQueryTime;      // 总查询时间（毫秒）
        
        public TestResult(String indexName, int dimension) {
            this.indexName = indexName;
            this.dimension = dimension;
        }
        
        @Override
        public String toString() {
            return String.format(
                "%s (dim=%d): Build=%.2fms, AvgQuery=%.2fms, AvgResults=%.1f, " +
                "AvgDistCount=%.1f, TotalQuery=%.2fms",
                indexName, dimension, buildTime, avgQueryTime, avgResultCount,
                avgDistanceCount, totalQueryTime
            );
        }
    }

    public static void main(String[] args) {
        System.out.println("=== 索引批量测试开始 ===");
        System.out.println("时间: " + new Date());
        System.out.println("数据集: " + DATASET_PATH);
        System.out.println("数据数量: " + DATA_COUNT);
        System.out.println("查询次数: " + QUERY_COUNT);
        System.out.println("查询半径: " + QUERY_RADIUS);
        
        // 创建结果输出文件
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE, false))) {
            writer.write("=== 索引批量测试报告 ===\n");
            writer.write("测试时间: " + new Date() + "\n");
            writer.write("数据集: " + DATASET_PATH + "\n");
            writer.write("数据数量: " + DATA_COUNT + "\n");
            writer.write("查询次数: " + QUERY_COUNT + "\n");
            writer.write("查询半径: " + QUERY_RADIUS + "\n");
            writer.write("最大叶子节点容量: " + MAX_LEAF_SIZE + "\n");
            writer.write("叶子节点支撑点数: " + LEAF_PIVOTS + "\n");
            writer.write("内部节点支撑点数: " + INTERNAL_PIVOTS + "\n");
            writer.write("扇区数: " + NUM_REGIONS + "\n");
            writer.write("距离函数: 曼哈顿距离 (L1)\n");
            writer.write("支撑点选择算法: FarthestFirstTraversal\n");
            writer.write("========================================\n\n");
            
            // 测试不同维度
            testAllDimensions(writer);
            
            System.out.println("\n=== 测试完成 ===");
            System.out.println("结果已保存到: " + OUTPUT_FILE);
            
        } catch (IOException e) {
            System.err.println("无法写入结果文件: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("测试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 测试所有维度
     */
    private static void testAllDimensions(BufferedWriter writer) throws IOException, Exception {
        writer.write("=== 多维度性能测试 ===\n\n");
        
        for (int dim : DIMENSIONS) {
            System.out.println("\n=== 测试维度 " + dim + " ===");
            writer.write("--- 维度 " + dim + " ---\n");
            
            try {
                // 1. 加载数据
                System.out.println("正在加载数据...");
                VectorReader vectorReader = new VectorReader();
                List<VectorData> vectors = vectorReader.load(DATASET_PATH, dim, DATA_COUNT);
                List<MetricData> dataset = new ArrayList<>(vectors);
                
                System.out.println("成功加载 " + dataset.size() + " 个 " + dim + " 维向量");
                writer.write("加载数据: " + dataset.size() + " 个 " + dim + " 维向量\n");
                
                // 2. 准备查询点（前1000个数据作为查询点）
                System.out.println("准备查询点...");
                List<MetricData> queryPoints = new ArrayList<>();
                int actualQueryCount = Math.min(QUERY_COUNT, dataset.size());
                for (int i = 0; i < actualQueryCount; i++) {
                    queryPoints.add(dataset.get(i));
                }
                System.out.println("准备 " + queryPoints.size() + " 个查询点");
                
                // 3. 创建距离函数和支撑点选择器
                MetricDistance distance = new LDistance(1.0); // 曼哈顿距离
                PivotSelectionMethod pivotSelector = new FarthestFirstTraversalSelection();
                
                // 4. 测试三种索引
                TestResult mvptResult = testMVPT(dataset, queryPoints, distance, pivotSelector, dim);
                TestResult cghtResult = testCGHT(dataset, queryPoints, distance, pivotSelector, dim);
                TestResult cptResult = testCPT(dataset, queryPoints, distance, pivotSelector, dim);
                
                // 5. 输出结果
                System.out.println("MVPT结果: " + mvptResult);
                System.out.println("CGHT结果: " + cghtResult);
                System.out.println("CPT结果: " + cptResult);
                
                // 写入文件
                writer.write("索引类型 | 构建时间(ms) | 平均查询时间(ms) | 平均结果数 | 平均距离计算次数 | 总查询时间(ms)\n");
                writer.write(String.format("MVPT    | %.2f | %.2f | %.1f | %.1f | %.2f\n",
                    mvptResult.buildTime, mvptResult.avgQueryTime, mvptResult.avgResultCount,
                    mvptResult.avgDistanceCount, mvptResult.totalQueryTime));
                writer.write(String.format("CGHT    | %.2f | %.2f | %.1f | %.1f | %.2f\n",
                    cghtResult.buildTime, cghtResult.avgQueryTime, cghtResult.avgResultCount,
                    cghtResult.avgDistanceCount, cghtResult.totalQueryTime));
                writer.write(String.format("CPT     | %.2f | %.2f | %.1f | %.1f | %.2f\n",
                    cptResult.buildTime, cptResult.avgQueryTime, cptResult.avgResultCount,
                    cptResult.avgDistanceCount, cptResult.totalQueryTime));
                
                
            } catch (Exception e) {
                System.err.println("维度 " + dim + " 测试失败: " + e.getMessage());
                e.printStackTrace();
                writer.write("维度 " + dim + " 测试失败: " + e.getMessage() + "\n\n");
            }
        }
    }
    
    /**
     * 测试MVPT索引
     */
    private static TestResult testMVPT(List<MetricData> dataset, List<MetricData> queryPoints,
                                      MetricDistance distance, PivotSelectionMethod pivotSelector,
                                      int dimension) throws Exception {
        
        System.out.println("开始测试MVPT...");
        TestResult result = new TestResult("MVPT", dimension);
        
        // 1. 构建索引
        long buildStart = System.currentTimeMillis();
        Object indexRoot = MultipleVantagePointTree.MVPBulkLoad(
            dataset, MAX_LEAF_SIZE, distance, pivotSelector,
            LEAF_PIVOTS, NUM_REGIONS, INTERNAL_PIVOTS
        );
        long buildEnd = System.currentTimeMillis();
        result.buildTime = buildEnd - buildStart;
        System.out.println("MVPT构建完成，耗时: " + result.buildTime + "ms");
        
        // 2. 执行查询
        double totalQueryTime = 0;
        double totalResultCount = 0;
        double totalDistanceCount = 0;
        int queryCount = queryPoints.size();
        
        for (int i = 0; i < queryCount; i++) {
            MetricData query = queryPoints.get(i);
            
            // 重置距离计数器
            distance.resetCount();
            
            long queryStart = System.currentTimeMillis();
            SearchResult searchResult = MVPRangeSearch.rangeSearch(indexRoot, query, QUERY_RADIUS, distance);
            long queryEnd = System.currentTimeMillis();
            
            totalQueryTime += (queryEnd - queryStart);
            totalResultCount += searchResult.results.size();
            totalDistanceCount += distance.getCount();
            
            // 进度显示
            if ((i + 1) % 100 == 0) {
                System.out.println("MVPT查询进度: " + (i + 1) + "/" + queryCount);
            }
        }
        
        // 3. 计算平均值
        result.totalQueryTime = totalQueryTime;
        result.avgQueryTime = totalQueryTime / queryCount;
        result.avgResultCount = totalResultCount / queryCount;
        result.avgDistanceCount = totalDistanceCount / queryCount;
        
        System.out.println("MVPT测试完成");
        return result;
    }
    
    /**
     * 测试CGHT索引
     */
    private static TestResult testCGHT(List<MetricData> dataset, List<MetricData> queryPoints,
                                      MetricDistance distance, PivotSelectionMethod pivotSelector,
                                      int dimension) throws Exception {
        
        System.out.println("开始测试CGHT...");
        TestResult result = new TestResult("CGHT", dimension);
        
        // 1. 构建索引
        long buildStart = System.currentTimeMillis();
        Object indexRoot = CompleteGeneralHyperPlaneTree.CGHBulkLoad(
            dataset, MAX_LEAF_SIZE, distance, pivotSelector,
            LEAF_PIVOTS, NUM_REGIONS, INTERNAL_PIVOTS
        );
        long buildEnd = System.currentTimeMillis();
        result.buildTime = buildEnd - buildStart;
        System.out.println("CGHT构建完成，耗时: " + result.buildTime + "ms");
        
        // 2. 执行查询（使用CPRangeSearch，因为结构相同）
        double totalQueryTime = 0;
        double totalResultCount = 0;
        double totalDistanceCount = 0;
        int queryCount = queryPoints.size();
        
        for (int i = 0; i < queryCount; i++) {
            MetricData query = queryPoints.get(i);
            
            // 重置距离计数器
            distance.resetCount();
            
            long queryStart = System.currentTimeMillis();
           
            SearchResult searchResult = search.CGHRangeSearch.rangeSearch(indexRoot, query, QUERY_RADIUS, distance);
            long queryEnd = System.currentTimeMillis();
            
            totalQueryTime += (queryEnd - queryStart);
            totalResultCount += searchResult.results.size();
            totalDistanceCount += distance.getCount();
            
            // 进度显示
            if ((i + 1) % 100 == 0) {
                System.out.println("CGHT查询进度: " + (i + 1) + "/" + queryCount);
            }
        }
        
        // 3. 计算平均值
        result.totalQueryTime = totalQueryTime;
        result.avgQueryTime = totalQueryTime / queryCount;
        result.avgResultCount = totalResultCount / queryCount;
        result.avgDistanceCount = totalDistanceCount / queryCount;
        
        System.out.println("CGHT测试完成");
        return result;
    }
    
    /**
     * 测试CPT索引
     */
    private static TestResult testCPT(List<MetricData> dataset, List<MetricData> queryPoints,
                                     MetricDistance distance, PivotSelectionMethod pivotSelector,
                                     int dimension) throws Exception {
        
        System.out.println("开始测试CPT...");
        TestResult result = new TestResult("CPT", dimension);
        
        // 1. 构建索引
        long buildStart = System.currentTimeMillis();
        Object indexRoot = CompletePartitionTree.CPBulkLoad(
            dataset, MAX_LEAF_SIZE, distance, pivotSelector,
            LEAF_PIVOTS, NUM_REGIONS, INTERNAL_PIVOTS
        );
        long buildEnd = System.currentTimeMillis();
        result.buildTime = buildEnd - buildStart;
        System.out.println("CPT构建完成，耗时: " + result.buildTime + "ms");
        
        // 2. 执行查询
        double totalQueryTime = 0;
        double totalResultCount = 0;
        double totalDistanceCount = 0;
        int queryCount = queryPoints.size();
        
        for (int i = 0; i < queryCount; i++) {
            MetricData query = queryPoints.get(i);
            
            // 重置距离计数器
            distance.resetCount();
            
            long queryStart = System.currentTimeMillis();
            SearchResult searchResult = search.CPRangeSearch.rangeSearch(indexRoot, query, QUERY_RADIUS, distance);
            long queryEnd = System.currentTimeMillis();
            
            totalQueryTime += (queryEnd - queryStart);
            totalResultCount += searchResult.results.size();
            totalDistanceCount += distance.getCount();
            
            // 进度显示
            if ((i + 1) % 100 == 0) {
                System.out.println("CPT查询进度: " + (i + 1) + "/" + queryCount);
            }
        }
        
        // 3. 计算平均值
        result.totalQueryTime = totalQueryTime;
        result.avgQueryTime = totalQueryTime / queryCount;
        result.avgResultCount = totalResultCount / queryCount;
        result.avgDistanceCount = totalDistanceCount / queryCount;
        
        System.out.println("CPT测试完成");
        return result;
    }
    
    /**
     * 内存清理（可选，用于大内存测试）
     */
    private static void cleanupMemory() {
        System.gc();
        try {
            Thread.sleep(1000); // 给GC一些时间
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}