package test;

import dataload.ProteinReader;
import dataload.VectorReader;
import db.MetricData;
import db.StringData;
import db.VectorData;
import distance_function.LDistance;
import distance_function.MetricDistance;
import distance_function.WeightedEditDistance;
import distance_function.matrix.mPAM;
import algorithms.pivotselection.PivotCollectingProxy;
import algorithms.pivotselection.RandomSelection;
import search.GHRangeSearch;
import search.VPTLimitRangeSearch;
import search.SearchResult;
import index.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * 索引批量测试类
 * 对两个数据集进行GHT和VPT索引的批量性能测试
 */
public class IndexBatchTest {
    
    // 测试配置
    private static final int DATA_COUNT = 1000;          // 读取数据数量
    private static final int QUERY_COUNT = 50;          // 查询次数
    private static final int MAX_LEAF_SIZE = 50;        // 最大叶子节点容量
    private static final int PIVOTS_PER_NODE = 2;       // 每个节点的支撑点数量
    private static final int VPT_LEAF_PIVOTS = 2;       // VPT叶子节点支撑点数
    
    // 向量数据配置
    private static final int[] VECTOR_DIMS = {2, 10};   // 测试的向量维度
    private static final double VECTOR_QUERY_RADIUS = 2.5; // 向量查询半径
    
    // 蛋白质数据配置
    private static final double PROTEIN_QUERY_RADIUS = 800.0; // 蛋白质查询半径
    
    // 数据集路径
    private static final String VECTOR_DATASET = "D:\\code\\metric_pro\\dataset\\uniformvector-20dim-1m.txt";
    private static final String PROTEIN_DATASET = "D:\\code\\metric_pro\\dataset\\yeast.aa";
    
    // 结果输出文件
    private static final String OUTPUT_FILE = "D:\\code\\metric_pro\\test_results.txt";
    
    public static void main(String[] args) {
        System.out.println("=== 索引批量测试开始 ===");
        System.out.println("时间: " + new Date());
        
        // 创建结果输出文件
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE, false))) {
            writer.write("=== 索引批量测试报告 ===\n");
            writer.write("测试时间: " + new Date() + "\n");
            writer.write("========================================\n\n");
            
            // 测试向量数据集
            testVectorDatasets(writer);
            
            writer.write("\n\n");
            
            // // 测试蛋白质数据集
            // testProteinDataset(writer);
            
            System.out.println("\n=== 测试完成 ===");
            System.out.println("结果已保存到: " + OUTPUT_FILE);
            
        } catch (IOException e) {
            System.err.println("无法写入结果文件: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 测试向量数据集（不同维度）
     */
    private static void testVectorDatasets(BufferedWriter writer) throws IOException {
        writer.write("=== 向量数据集测试 ===\n");
        writer.write("数据集: " + VECTOR_DATASET + "\n");
        writer.write("读取数据数量: " + DATA_COUNT + "\n");
        writer.write("查询次数: " + QUERY_COUNT + "\n");
        writer.write("查询半径: " + VECTOR_QUERY_RADIUS + "\n");
        writer.write("最大叶子节点容量: " + MAX_LEAF_SIZE + "\n");
        writer.write("节点支撑点数量: " + PIVOTS_PER_NODE + "\n");
        writer.write("VPT叶子节点支撑点数: " + VPT_LEAF_PIVOTS + "\n\n");
        
        for (int dim : VECTOR_DIMS) {
            System.out.println("\n=== 测试向量数据集，维度=" + dim + " ===");
            writer.write("--- 维度 " + dim + " ---\n");
            
            try {
                // 加载数据
                System.out.println("正在加载数据...");
                VectorReader vectorReader = new VectorReader();
                List<VectorData> vectors = vectorReader.load(VECTOR_DATASET, dim, DATA_COUNT);
                List<MetricData> dataList = new ArrayList<>(vectors);
                
                System.out.println("成功加载 " + vectors.size() + " 个向量数据");
                writer.write("成功加载 " + vectors.size() + " 个" + dim + "维向量数据\n");
                
                // 创建距离函数（p=1的闵可夫斯基距离）
                MetricDistance distance = new LDistance(1.0);
                System.out.println("使用欧式距离（p=2）");
                
                // 进行测试
                TestResult ghtResult = testGHT(dataList, distance, writer, dim);
                TestResult vptResult = testVPT(dataList, distance, writer, dim);
                
                // 输出对比结果
                writer.write("\n维度 " + dim + " 对比结果:\n");
                writer.write(String.format("GHT构建时间: %.2f ms, VPT构建时间: %.2f ms\n", 
                    ghtResult.buildTime, vptResult.buildTime));
                writer.write(String.format("GHT平均查询时间: %.2f ms, VPT平均查询时间: %.2f ms\n", 
                    ghtResult.avgQueryTime, vptResult.avgQueryTime));
                writer.write(String.format("GHT平均距离计算次数: %.1f, VPT平均距离计算次数: %.1f\n", 
                    ghtResult.avgDistanceCount, vptResult.avgDistanceCount));
                writer.write(String.format("查询时间加速比(GHT/VPT): %.2fx\n", 
                    ghtResult.avgQueryTime / vptResult.avgQueryTime));
                writer.write(String.format("距离计算加速比(GHT/VPT): %.2fx\n\n", 
                    ghtResult.avgDistanceCount / vptResult.avgDistanceCount));
                
            } catch (Exception e) {
                System.err.println("维度 " + dim + " 测试失败: " + e.getMessage());
                e.printStackTrace();
                writer.write("维度 " + dim + " 测试失败: " + e.getMessage() + "\n");
            }
        }
    }
    
    // /**
    //  * 测试蛋白质数据集
    //  */
    // private static void testProteinDataset(BufferedWriter writer) throws IOException {
    //     System.out.println("\n=== 测试蛋白质数据集 ===");
    //     writer.write("=== 蛋白质数据集测试 ===\n");
    //     writer.write("数据集: " + PROTEIN_DATASET + "\n");
    //     writer.write("读取数据数量: " + DATA_COUNT + "\n");
    //     writer.write("查询次数: " + QUERY_COUNT + "\n");
    //     writer.write("查询半径: " + PROTEIN_QUERY_RADIUS + "\n");
    //     writer.write("最大叶子节点容量: " + MAX_LEAF_SIZE + "\n");
    //     writer.write("节点支撑点数量: " + PIVOTS_PER_NODE + "\n");
    //     writer.write("VPT叶子节点支撑点数: " + VPT_LEAF_PIVOTS + "\n\n");
        
    //     try {
    //         // 加载数据
    //         System.out.println("正在加载蛋白质数据...");
    //         ProteinReader proteinReader = new ProteinReader();
    //         List<MetricData> dataList = proteinReader.load(PROTEIN_DATASET, DATA_COUNT);
            
    //         System.out.println("成功加载 " + dataList.size() + " 个蛋白质序列");
    //         writer.write("成功加载 " + dataList.size() + " 个蛋白质序列\n");
            
    //         // 创建距离函数（基于mPAM的加权编辑距离）
    //         MetricDistance distance = new WeightedEditDistance(new mPAM());
    //         System.out.println("使用基于mPAM的加权编辑距离");
            
    //         // 进行测试
    //         TestResult ghtResult = testGHT(dataList, distance, writer, -1);
    //         TestResult vptResult = testVPT(dataList, distance, writer, -1);
            
    //         // 输出对比结果
    //         writer.write("\n蛋白质数据集对比结果:\n");
    //         writer.write(String.format("GHT构建时间: %.2f ms, VPT构建时间: %.2f ms\n", 
    //             ghtResult.buildTime, vptResult.buildTime));
    //         writer.write(String.format("GHT平均查询时间: %.2f ms, VPT平均查询时间: %.2f ms\n", 
    //             ghtResult.avgQueryTime, vptResult.avgQueryTime));
    //         writer.write(String.format("GHT平均距离计算次数: %.1f, VPT平均距离计算次数: %.1f\n", 
    //             ghtResult.avgDistanceCount, vptResult.avgDistanceCount));
    //         writer.write(String.format("查询时间加速比(GHT/VPT): %.2fx\n", 
    //             ghtResult.avgQueryTime / vptResult.avgQueryTime));
    //         writer.write(String.format("距离计算加速比(GHT/VPT): %.2fx\n\n", 
    //             ghtResult.avgDistanceCount / vptResult.avgDistanceCount));
            
    //     } catch (Exception e) {
    //         System.err.println("蛋白质数据集测试失败: " + e.getMessage());
    //         e.printStackTrace();
    //         writer.write("蛋白质数据集测试失败: " + e.getMessage() + "\n");
    //     }
    // }
    
    /**
     * 测试GHT索引
     */
    private static TestResult testGHT(List<MetricData> dataList, MetricDistance distance,
                                     BufferedWriter writer, int dim) throws IOException {
        System.out.println("\n--- 测试GHT索引 ---");
        writer.write("[GHT测试开始]\n");
        
        TestResult result = new TestResult();
        
        // 构建GHT索引
        System.out.println("正在构建GHT索引...");
        long buildStartTime = System.currentTimeMillis();
        
        // 创建收集代理（用于收集GHT支撑点）
        RandomSelection baseSelector = new RandomSelection();
        PivotCollectingProxy collectingProxy = new PivotCollectingProxy(baseSelector);
        collectingProxy.startCollecting();
        
        // 构建GHT
        Object ghtRoot = GeneralHyperPlaneTree.GHBulkLoad(
            dataList, MAX_LEAF_SIZE, distance, collectingProxy, PIVOTS_PER_NODE);
        
        // 获取收集的支撑点
        List<MetricData> ghtPivots = collectingProxy.getCollectedPivots();
        collectingProxy.stopCollecting();
        
        long buildEndTime = System.currentTimeMillis();
        result.buildTime = buildEndTime - buildStartTime;
        
        // 统计GHT数据
        int ghtTotalData = countTotalDataInTree(ghtRoot);
        
        System.out.println("GHT构建完成，耗时: " + result.buildTime + " ms");
        System.out.println("收集到 " + ghtPivots.size() + " 个支撑点");
        System.out.println("GHT树中总数据点数: " + ghtTotalData);
        
        writer.write("GHT构建时间: " + result.buildTime + " ms\n");
        writer.write("GHT支撑点数量: " + ghtPivots.size() + "\n");
        writer.write("GHT树中数据点数: " + ghtTotalData + "\n");
        
        // 执行查询测试
        System.out.println("正在执行 " + QUERY_COUNT + " 次查询测试...");
        performQueries(ghtRoot, dataList, distance, result, "GHT", writer, dim);
        
        writer.write("[GHT测试结束]\n\n");
        return result;
    }
    
    /**
     * 测试VPT索引（使用GHT的支撑点）
     */
    private static TestResult testVPT(List<MetricData> dataList, MetricDistance distance,
                                     BufferedWriter writer, int dim) throws IOException {
        System.out.println("\n--- 测试VPT索引 ---");
        writer.write("[VPT测试开始]\n");
        
        TestResult result = new TestResult();
        
        // 先构建GHT获取支撑点
        System.out.println("先构建GHT获取支撑点...");
        RandomSelection baseSelector = new RandomSelection();
        PivotCollectingProxy collectingProxy = new PivotCollectingProxy(baseSelector);
        collectingProxy.startCollecting();
        
        // 构建GHT但不保存树，只为了收集支撑点
        GeneralHyperPlaneTree.GHBulkLoad(
            dataList, MAX_LEAF_SIZE, distance, collectingProxy, PIVOTS_PER_NODE);
        
        List<MetricData> ghtPivots = collectingProxy.getCollectedPivots();
        collectingProxy.stopCollecting();
        
        System.out.println("从GHT收集到 " + ghtPivots.size() + " 个支撑点");
        
        // 计算VPT会使用的支撑点
        List<MetricData> vptPivotsToUse = VPTPivotsNum_limit.calculateAndGetUsedPivots(
            ghtPivots, VPT_LEAF_PIVOTS);
        
        // 准备VPT数据集（移除VPT会使用的支撑点）
        List<MetricData> vptData = VPTPivotsNum_limit.removePivotsFromData(
            dataList, vptPivotsToUse);
        
        // 构建VPT索引
        System.out.println("正在构建VPT索引...");
        long buildStartTime = System.currentTimeMillis();
        
        VPTPivotsNum_limit.BuildResult vptBuildResult = 
            VPTPivotsNum_limit.buildWithPivotLimit(
                vptData, VPT_LEAF_PIVOTS, distance, vptPivotsToUse);
        
        Object vptRoot = vptBuildResult.root;
        
        long buildEndTime = System.currentTimeMillis();
        result.buildTime = buildEndTime - buildStartTime;
        
        System.out.println("VPT构建完成，耗时: " + result.buildTime + " ms");
        System.out.println("VPT使用支撑点数: " + vptBuildResult.pivotsUsed);
        System.out.println("VPT树中总数据点数: " + vptBuildResult.totalDataPoints);
        
        writer.write("VPT构建时间: " + result.buildTime + " ms\n");
        writer.write("VPT使用支撑点数: " + vptBuildResult.pivotsUsed + "/" + vptPivotsToUse.size() + "\n");
        writer.write("VPT树中数据点数: " + vptBuildResult.totalDataPoints + "\n");
        
        // 执行查询测试
        System.out.println("正在执行 " + QUERY_COUNT + " 次查询测试...");
        performQueries(vptRoot, dataList, distance, result, "VPT", writer, dim);
        
        writer.write("[VPT测试结束]\n\n");
        return result;
    }
    
    /**
     * 执行查询测试
     */
    private static void performQueries(Object indexRoot, List<MetricData> dataList,
                                      MetricDistance distance, TestResult result,
                                      String indexName, BufferedWriter writer,
                                      int dim) throws IOException {
        // 使用前QUERY_COUNT个数据点作为查询点
        int actualQueryCount = Math.min(QUERY_COUNT, dataList.size());
        
        // 确定查询半径
        double queryRadius = (dim > 0) ? VECTOR_QUERY_RADIUS : PROTEIN_QUERY_RADIUS;
        
        long totalQueryTime = 0;
        long totalDistanceCount = 0;
        
        writer.write("查询测试详情 (半径=" + queryRadius + "):\n");
        writer.write("查询序号,结果数量,距离计算次数,查询时间(ms)\n");
        
        for (int i = 0; i < actualQueryCount; i++) {
            MetricData query = dataList.get(i);
            
            // 重置距离计数器
            distance.resetCount();
            
            // 执行查询
            long queryStartTime = System.nanoTime();
            SearchResult searchResult;
            
            if ("GHT".equals(indexName)) {
                searchResult = GHRangeSearch.rangeSearch(indexRoot, query, queryRadius, distance);
            } else {
                searchResult = VPTLimitRangeSearch.rangeSearch(indexRoot, query, queryRadius, distance);
            }
            
            long queryEndTime = System.nanoTime();
            long queryTimeNano = queryEndTime - queryStartTime;
            long queryTimeMs = queryTimeNano / 1_000_000;
            
            // 累计统计
            totalQueryTime += queryTimeMs;
            totalDistanceCount += searchResult.distanceCount;
            
            // 记录每次查询的详细结果
            writer.write(String.format("%d,%d,%d,%d\n", 
                i+1, searchResult.results.size(), searchResult.distanceCount, queryTimeMs));
            
            // 每10次查询输出一次进度
            if ((i + 1) % 10 == 0) {
                System.out.println("  已完成 " + (i + 1) + "/" + actualQueryCount + " 次查询");
            }
        }
        
        // 计算平均值
        result.avgQueryTime = (double) totalQueryTime / actualQueryCount;
        result.avgDistanceCount = (double) totalDistanceCount / actualQueryCount;
        
        System.out.println(indexName + " 查询测试完成:");
        System.out.println("  平均查询时间: " + String.format("%.2f", result.avgQueryTime) + " ms");
        System.out.println("  平均距离计算次数: " + String.format("%.1f", result.avgDistanceCount));
        
        writer.write("平均查询时间: " + String.format("%.2f", result.avgQueryTime) + " ms\n");
        writer.write("平均距离计算次数: " + String.format("%.1f", result.avgDistanceCount) + "\n");
    }
    
    /**
     * 统计树中的数据总量
     */
    private static int countTotalDataInTree(Object node) {
        if (node instanceof PivotTable) {
            PivotTable pt = (PivotTable) node;
            return pt.getPivots().size() + pt.getDataPoints().size();
        } 
        else if (node instanceof GeneralHyperPlaneTree.GHInternalNode) {
            GeneralHyperPlaneTree.GHInternalNode internal = (GeneralHyperPlaneTree.GHInternalNode) node;
            int count = 2; // pivot1 和 pivot2
            if (internal.left != null) count += countTotalDataInTree(internal.left);
            if (internal.right != null) count += countTotalDataInTree(internal.right);
            return count;
        }
        else if (node instanceof VPTPivotsNum_limit.VPTInternalNode) {
            VPTPivotsNum_limit.VPTInternalNode internal = (VPTPivotsNum_limit.VPTInternalNode) node;
            int count = 1; // pivot
            if (internal.left != null) count += countTotalDataInTree(internal.left);
            if (internal.right != null) count += countTotalDataInTree(internal.right);
            return count;
        }
        else if (node instanceof VantagePointTree.VPTInternalNode) {
            VantagePointTree.VPTInternalNode internal = (VantagePointTree.VPTInternalNode) node;
            int count = 1; // pivot
            if (internal.left != null) count += countTotalDataInTree(internal.left);
            if (internal.right != null) count += countTotalDataInTree(internal.right);
            return count;
        }
        
        return 0;
    }
    
    /**
     * 测试结果类
     */
    private static class TestResult {
        double buildTime;           // 构建时间(ms)
        double avgQueryTime;        // 平均查询时间(ms)
        double avgDistanceCount;    // 平均距离计算次数
        
        @Override
        public String toString() {
            return String.format("BuildTime=%.2fms, AvgQueryTime=%.2fms, AvgDistanceCount=%.1f",
                buildTime, avgQueryTime, avgDistanceCount);
        }
    }
    
    /**
     * 简单测试（用于快速验证）
     */
    public static void quickTest() {
        System.out.println("=== 快速测试 ===");
        
        try {
            // 测试2维向量
            System.out.println("\n1. 测试2维向量数据");
            VectorReader vectorReader = new VectorReader();
            List<VectorData> vectors2d = vectorReader.load(VECTOR_DATASET, 2, 100);
            List<MetricData> data2d = new ArrayList<>(vectors2d);
            MetricDistance distance2d = new LDistance(1.0);
            
            TestResult ght2d = quickTestGHT(data2d, distance2d, "2D向量");
            TestResult vpt2d = quickTestVPT(data2d, distance2d, "2D向量");
            
            System.out.println("2D GHT: " + ght2d);
            System.out.println("2D VPT: " + vpt2d);
            
            // 测试蛋白质
            System.out.println("\n2. 测试蛋白质数据");
            ProteinReader proteinReader = new ProteinReader();
            List<MetricData> proteins = proteinReader.load(PROTEIN_DATASET, 100);
            MetricDistance proteinDistance = new WeightedEditDistance(new mPAM());
            
            TestResult ghtProtein = quickTestGHT(proteins, proteinDistance, "蛋白质");
            TestResult vptProtein = quickTestVPT(proteins, proteinDistance, "蛋白质");
            
            System.out.println("Protein GHT: " + ghtProtein);
            System.out.println("Protein VPT: " + vptProtein);
            
        } catch (Exception e) {
            System.err.println("快速测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 快速测试GHT
     */
    private static TestResult quickTestGHT(List<MetricData> dataList, MetricDistance distance, String dataType) {
        TestResult result = new TestResult();
        
        try {
            // 构建
            long start = System.currentTimeMillis();
            RandomSelection baseSelector = new RandomSelection();
            PivotCollectingProxy collectingProxy = new PivotCollectingProxy(baseSelector);
            collectingProxy.startCollecting();
            
            Object ghtRoot = GeneralHyperPlaneTree.GHBulkLoad(
                dataList, 50, distance, collectingProxy, 2);
            
            List<MetricData> pivots = collectingProxy.getCollectedPivots();
            collectingProxy.stopCollecting();
            
            result.buildTime = System.currentTimeMillis() - start;
            
            // 简单查询测试（5次）
            int testQueries = Math.min(5, dataList.size());
            long totalTime = 0;
            long totalDistances = 0;
            
            for (int i = 0; i < testQueries; i++) {
                distance.resetCount();
                long queryStart = System.nanoTime();
                SearchResult searchResult = GHRangeSearch.rangeSearch(
                    ghtRoot, dataList.get(i), 0.1, distance);
                long queryEnd = System.nanoTime();
                
                totalTime += (queryEnd - queryStart) / 1_000_000;
                totalDistances += searchResult.distanceCount;
            }
            
            result.avgQueryTime = (double) totalTime / testQueries;
            result.avgDistanceCount = (double) totalDistances / testQueries;
            
        } catch (Exception e) {
            System.err.println(dataType + " GHT快速测试失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 快速测试VPT
     */
    private static TestResult quickTestVPT(List<MetricData> dataList, MetricDistance distance, String dataType) {
        TestResult result = new TestResult();
        
        try {
            // 先获取GHT支撑点
            RandomSelection baseSelector = new RandomSelection();
            PivotCollectingProxy collectingProxy = new PivotCollectingProxy(baseSelector);
            collectingProxy.startCollecting();
            
            GeneralHyperPlaneTree.GHBulkLoad(dataList, 50, distance, collectingProxy, 2);
            List<MetricData> ghtPivots = collectingProxy.getCollectedPivots();
            collectingProxy.stopCollecting();
            
            // 计算VPT支撑点
            List<MetricData> vptPivots = VPTPivotsNum_limit.calculateAndGetUsedPivots(ghtPivots, 2);
            List<MetricData> vptData = VPTPivotsNum_limit.removePivotsFromData(dataList, vptPivots);
            
            // 构建VPT
            long start = System.currentTimeMillis();
            VPTPivotsNum_limit.BuildResult vptResult = 
                VPTPivotsNum_limit.buildWithPivotLimit(vptData, 2, distance, vptPivots);
            
            result.buildTime = System.currentTimeMillis() - start;
            
            // 简单查询测试（5次）
            int testQueries = Math.min(5, dataList.size());
            long totalTime = 0;
            long totalDistances = 0;
            
            for (int i = 0; i < testQueries; i++) {
                distance.resetCount();
                long queryStart = System.nanoTime();
                SearchResult searchResult = VPTLimitRangeSearch.rangeSearch(
                    vptResult.root, dataList.get(i), 0.1, distance);
                long queryEnd = System.nanoTime();
                
                totalTime += (queryEnd - queryStart) / 1_000_000;
                totalDistances += searchResult.distanceCount;
            }
            
            result.avgQueryTime = (double) totalTime / testQueries;
            result.avgDistanceCount = (double) totalDistances / testQueries;
            
        } catch (Exception e) {
            System.err.println(dataType + " VPT快速测试失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }
}