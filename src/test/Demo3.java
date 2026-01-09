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

import java.io.IOException;
import java.util.*;

public class Demo3 {

    private static final Map<String, String> DATASET_PATHS = Map.of(
        "1", "D:\\code\\metric_pro\\dataset\\uniformvector-20dim-1m.txt",
        "2", "D:\\code\\metric_pro\\dataset\\yeast.aa", 
        "3", "D:\\code\\metric_pro\\dataset\\test_vector.txt"
    );

    private static final Map<String, String> DATASET_TYPES = Map.of(
        "1", "vector",
        "2", "protein",
        "3", "vector"
    );

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.println("\n=== 全局支撑点池索引对比测试 ===");
            System.out.println("请选择要测试的数据集:");
            System.out.println("[1] uniformvector-20dim-1m (向量数据)");
            System.out.println("[2] yeast (蛋白质序列数据)");
            System.out.println("[3] test_vector (测试向量数据)");
            System.out.println("[exit] 退出程序");
            System.out.print("请输入选择: ");
            
            String choice = scanner.nextLine().trim().toLowerCase();
            
            if ("exit".equals(choice)) {
                System.out.println("程序结束");
                break;
            }
            if (!DATASET_PATHS.containsKey(choice)) {
                System.out.println("无效选择，请重新输入");
                continue;
            }
            
            String filePath = DATASET_PATHS.get(choice);
            String dataType = DATASET_TYPES.get(choice);
            boolean isVector = "vector".equals(dataType);
            boolean isProtein = "protein".equals(dataType);
            
            // 读取参数
            int dim = 0;
            if (isVector) {
                System.out.print("请输入向量维度: ");
                dim = Integer.parseInt(scanner.nextLine());
            }
            
            System.out.print("请输入要读取的数据数量: ");
            int count = Integer.parseInt(scanner.nextLine());
            
            // 加载数据
            List<MetricData> dataList;
            try {
                if (isVector) {
                    VectorReader vectorReader = new VectorReader();
                    List<VectorData> vectors = vectorReader.load(filePath, dim, count);
                    dataList = new ArrayList<>(vectors);
                    System.out.println("\n成功加载 " + vectors.size() + " 个向量数据");
                    
                    // 显示读取的向量数据（只显示前5个）
                    System.out.println("\n=== 读取的向量数据（前5个）===");
                    for (int i = 0; i < Math.min(5, vectors.size()); i++) {
                        System.out.println("向量" + (i+1) + ": " + vectors.get(i));
                    }
                    if (vectors.size() > 5) {
                        System.out.println("... (共" + vectors.size() + "个向量)");
                    }
                } else if (isProtein) {
                    ProteinReader proteinReader = new ProteinReader();
                    dataList = proteinReader.load(filePath, count);
                    System.out.println("\n成功加载 " + dataList.size() + " 个蛋白质序列");
                    
                    // 显示读取的蛋白质序列（只显示前3个）
                    System.out.println("\n=== 读取的蛋白质序列（前3个）===");
                    for (int i = 0; i < Math.min(3, dataList.size()); i++) {
                        StringData seq = (StringData) dataList.get(i);
                        String sequence = seq.getValue();
                        System.out.println("序列" + (i+1) + " (长度" + sequence.length() + "): " + 
                                         (sequence.length() > 50 ? sequence.substring(0, 50) + "..." : sequence));
                    }
                    if (dataList.size() > 3) {
                        System.out.println("... (共" + dataList.size() + "个序列)");
                    }
                } else {
                    throw new IllegalArgumentException("不支持的数据类型");
                }
            } catch (Exception e) {
                System.out.println("数据加载失败: " + e.getMessage());
                continue;
            }
            
            // 距离函数设置
            MetricDistance distance;
            if (isVector) {
                System.out.print("请输入闵可夫斯基距离的p值 (1=曼哈顿, 2=欧式, inf=切比雪夫): ");
                String pInput = scanner.nextLine().trim().toLowerCase();
                double p;
                if ("inf".equals(pInput) || "infinity".equals(pInput)) {
                    p = Double.POSITIVE_INFINITY;
                } else {
                    try {
                        p = Double.parseDouble(pInput);
                    } catch (NumberFormatException e) {
                        System.out.println("无效p值，使用默认值2.0");
                        p = 2.0;
                    }
                }
                distance = new LDistance(p);
                System.out.println("使用闵可夫斯基距离，p=" + p);
            } else {
                distance = new WeightedEditDistance(new mPAM());
                System.out.println("使用基于mPAM的加权编辑距离");
            }
            
            // 索引构建参数
            System.out.println("\n=== 索引构建参数设置 ===");
            System.out.print("请输入最大叶子节点容量（用于GHT）: ");
            int maxLeafSize = Integer.parseInt(scanner.nextLine());
            
            System.out.print("请输入每个节点的支撑点数量: ");
            int numPivotsPerNode = Integer.parseInt(scanner.nextLine());
            
            System.out.print("请输入VPT叶子节点支撑点数: ");
            int numPivotsPerLeaf = Integer.parseInt(scanner.nextLine());
            
            // ========== 新的构建流程 ==========
            
            // 1. 创建基础选择器
            RandomSelection baseSelector = new RandomSelection();
            
            // 2. 创建收集代理（用于收集GHT支撑点）
            PivotCollectingProxy collectingProxy = new PivotCollectingProxy(baseSelector);
            
            // 3. 构建GHT并收集支撑点
            System.out.println("\n=== 构建GHT（并收集支撑点） ===");
            collectingProxy.startCollecting();
            
            System.out.println("正在构建GHT...");
            Object ghtRoot = GeneralHyperPlaneTree.GHBulkLoad(
                dataList, maxLeafSize, distance, collectingProxy, numPivotsPerNode);
            
            // 获取收集的支撑点
            List<MetricData> ghtPivots = collectingProxy.getCollectedPivots();
            collectingProxy.stopCollecting();
            
            System.out.println("GHT构建完成！");
            System.out.println("收集到 " + ghtPivots.size() + " 个支撑点");
            
            // 统计GHT中的数据总量
            int ghtTotalData = countTotalDataInTree(ghtRoot);
            
            // 4. 计算VPT参数并获取会使用的支撑点
            System.out.println("\n=== 计算VPT构建参数 ===");
            
            // 使用新方法计算VPT会使用的支撑点
            List<MetricData> vptPivotsToUse = VPTPivotsNum_limit.calculateAndGetUsedPivots(
                ghtPivots, numPivotsPerLeaf);
            
            System.out.println("GHT产生的支撑点数: " + ghtPivots.size());
            System.out.println("VPT将使用的支撑点数: " + vptPivotsToUse.size());
            
            // 5. 准备VPT数据集（移除VPT会使用的支撑点）
            System.out.println("\n=== 准备VPT数据集 ===");
            List<MetricData> vptData = VPTPivotsNum_limit.removePivotsFromData(
                dataList, vptPivotsToUse);
            
            // 6. 构建VPT（使用GHT的支撑点）
            System.out.println("\n=== 构建VPT（使用GHT的支撑点） ===");
            System.out.println("正在构建VPT...");
            
            VPTPivotsNum_limit.BuildResult vptResult = null;
            try {
                vptResult = VPTPivotsNum_limit.buildWithPivotLimit(
                    vptData,           // 已经移除支撑点的数据集
                    numPivotsPerLeaf,  // 叶子节点支撑点数
                    distance,          // 距离函数
                    vptPivotsToUse     // VPT会使用的支撑点
                );
            } catch (Exception e) {
                System.out.println("VPT构建失败: " + e.getMessage());
                e.printStackTrace();
                System.out.println("请检查构建参数，重新尝试");
                continue;
            }
            
            Object vptRoot = vptResult.root;
            
            // 收集所有在索引中的数据
            Set<MetricData> allIndexedData = new HashSet<>();
            
            // 从GHT中收集所有数据
            allIndexedData.addAll(getAllDataFromTree(ghtRoot));
            
            // 从VPT中收集所有数据（包括支撑点）
            allIndexedData.addAll(getAllDataFromTree(vptRoot));

            
            // 检查重叠数据（支撑点是否在两个索引中都存在）
            List<MetricData> ghtData = getAllDataFromTree(ghtRoot);
            List<MetricData> vptDataSet = getAllDataFromTree(vptRoot);
            Set<MetricData> overlap = new HashSet<>(ghtData);
            overlap.retainAll(vptDataSet);
            
            // ========== 索引选择与查询循环 ==========
            
            while (true) {
                System.out.println("\n=== 选择要测试的索引结构 ===");
                System.out.println("[0] GHT (广义超平面树)");
                System.out.println("[1] VPT (支撑点树) - 使用GHT支撑点");
                System.out.println("[2] 验证查询一致性");
                System.out.println("[back] 返回数据集选择");
                System.out.println("[exit] 退出程序");
                System.out.print("请输入选择: ");
                
                String indexChoice = scanner.nextLine().trim().toLowerCase();
                
                if ("exit".equals(indexChoice)) {
                    System.out.println("程序结束");
                    scanner.close();
                    return;
                }
                if ("back".equals(indexChoice)) {
                    break;
                }
                
                if (!indexChoice.equals("0") && !indexChoice.equals("1") && !indexChoice.equals("2")) {
                    System.out.println("无效选择，请重新输入");
                    continue;
                }
                
                if (indexChoice.equals("2")) {
                    // 验证查询一致性
                    validateQueryConsistency(ghtRoot, vptRoot, dataList, distance, scanner, isVector);
                    continue;
                }
                
                // 选择要测试的索引
                Object currentIndexRoot;
                String indexName;
                
                if (indexChoice.equals("0")) {
                    currentIndexRoot = ghtRoot;
                    indexName = "GHT";
                    System.out.println("选择GHT进行查询");
                } else {
                    currentIndexRoot = vptRoot;
                    indexName = "VPT（使用GHT支撑点）";
                    System.out.println("选择VPT进行查询（使用GHT的支撑点序列）");
                }
                
                // 查询循环
                queryLoop(currentIndexRoot, indexName, dataList, distance, 
                         scanner, isVector, dim, indexChoice);
            }
        }
        scanner.close();
    }
    
    /**
     * 查询循环
     */
    private static void queryLoop(Object indexRoot, String indexName, 
                                 List<MetricData> originalData,
                                 MetricDistance distance,
                                 Scanner scanner, boolean isVector, int dim,
                                 String indexChoice) {
        while (true) {
            System.out.println("\n=== " + indexName + " 范围查询 ===");
            System.out.print("请输入查询半径，或输入back返回索引选择: ");
            String radiusInput = scanner.nextLine().trim().toLowerCase();
            
            if ("back".equals(radiusInput)) {
                break;
            }
            
            double radius;
            try {
                radius = Double.parseDouble(radiusInput);
                if (radius < 0) {
                    System.out.println("半径不能为负数");
                    continue;
                }
            } catch (NumberFormatException e) {
                System.out.println("非法半径输入");
                continue;
            }
            
            // 获取查询点
            MetricData query = getQueryPoint(scanner, isVector, dim);
            if (query == null) {
                continue;
            }
            
            // 执行查询
            System.out.println("开始查询...");
            distance.resetCount(); // 重置距离计数器
            
            long queryStartTime = System.currentTimeMillis();
            SearchResult result = executeQuery(indexRoot, query, radius, distance, indexChoice);
            
            if (result == null) {
                continue;
            }
            
            long queryEndTime = System.currentTimeMillis();
            long queryTime = queryEndTime - queryStartTime;
            
            // 显示查询结果
            displayQueryResults(result, indexName, queryTime, scanner, isVector);
            
            // 询问是否继续查询
            System.out.print("\n是否继续查询？(y/n): ");
            if (!"y".equalsIgnoreCase(scanner.nextLine())) {
                break;
            }
        }
    }
    
    /**
     * 验证查询一致性
     */
    private static void validateQueryConsistency(Object ghtRoot, Object vptRoot,
                                                List<MetricData> originalData,
                                                MetricDistance distance,
                                                Scanner scanner, boolean isVector) {
        System.out.println("\n=== 查询一致性验证 ===");
        
        Random random = new Random();
        int numTests = 5;
        int consistentCount = 0;
        
        for (int i = 0; i < numTests; i++) {
            // 随机选择查询点
            MetricData query = originalData.get(random.nextInt(originalData.size()));
            
            // 随机选择半径
            double radius = random.nextDouble() * 10.0;
            
            System.out.println("\n[测试 " + (i+1) + "] 查询点: " + 
                             (isVector ? "向量" : "序列") + 
                             ", 半径: " + String.format("%.2f", radius));
            
            // GHT查询
            distance.resetCount();
            SearchResult ghtResult = GHRangeSearch.rangeSearch(ghtRoot, query, radius, distance);
            long ghtDistances = ghtResult.distanceCount;
            
            // VPT查询
            distance.resetCount();
            SearchResult vptResult = VPTLimitRangeSearch.rangeSearch(vptRoot, query, radius, distance);
            long vptDistances = vptResult.distanceCount;
            
            // 比较结果
            boolean consistent = compareResults(ghtResult.results, vptResult.results);
            
            if (consistent) {
                consistentCount++;
                System.out.println("  ✓ 一致 - GHT结果: " + ghtResult.results.size() + 
                                 ", VPT结果: " + vptResult.results.size() +
                                 ", GHT距离计算: " + ghtDistances +
                                 ", VPT距离计算: " + vptDistances);
            } else {
                System.out.println("  ✗ 不一致 - GHT结果: " + ghtResult.results.size() + 
                                 ", VPT结果: " + vptResult.results.size());
            }
        }
        
        System.out.println("\n=== 验证总结 ===");
        System.out.println("总测试数: " + numTests);
        System.out.println("一致次数: " + consistentCount);
        System.out.println("一致率: " + String.format("%.1f%%", 100.0 * consistentCount / numTests));
        
        System.out.print("\n按Enter键继续...");
        scanner.nextLine();
    }
    
    /**
     * 比较两个结果列表是否一致
     */
    private static boolean compareResults(List<MetricData> list1, List<MetricData> list2) {
        if (list1.size() != list2.size()) {
            return false;
        }
        
        Set<MetricData> set1 = new HashSet<>(list1);
        Set<MetricData> set2 = new HashSet<>(list2);
        
        return set1.equals(set2);
    }
    
    /**
     * 获取查询点
     */
    private static MetricData getQueryPoint(Scanner scanner, boolean isVector, int dim) {
        while (true) {
            if (isVector) {
                System.out.print("请输入查询向量(空格分隔)，或输入back返回半径输入: ");
                String queryInput = scanner.nextLine().trim();
                
                if ("back".equals(queryInput.toLowerCase())) {
                    return null;
                }
                
                String[] parts = queryInput.split("\\s+");
                if (parts.length != dim) {
                    System.out.println("查询向量维度错误，应为 " + dim + " 维");
                    continue;
                }
                double[] vec = new double[dim];
                try {
                    for (int i = 0; i < dim; i++) {
                        vec[i] = Double.parseDouble(parts[i]);
                    }
                    return new VectorData(vec);
                } catch (NumberFormatException e) {
                    System.out.println("非法数字输入，请重新输入");
                }
            } else {
                System.out.print("请输入查询序列，或输入back返回半径输入: ");
                String queryInput = scanner.nextLine().trim();
                
                if ("back".equals(queryInput.toLowerCase())) {
                    return null;
                }
                
                return new StringData(queryInput);
            }
        }
    }
    
    /**
     * 执行查询
     */
    private static SearchResult executeQuery(Object indexRoot, MetricData query, 
                                           double radius, MetricDistance distance,
                                           String indexChoice) {
        try {
            if (indexChoice.equals("0")) {
                // GHT查询
                return GHRangeSearch.rangeSearch(indexRoot, query, radius, distance);
            } else {
                // VPT查询
                return VPTLimitRangeSearch.rangeSearch(indexRoot, query, radius, distance);
            }
        } catch (Exception e) {
            System.out.println("查询错误: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 显示查询结果
     */
    private static void displayQueryResults(SearchResult result, String indexName,
                                          long queryTime, Scanner scanner,
                                          boolean isVector) {
        System.out.println("\n=== 查询结果 ===");
        System.out.println("索引结构: " + indexName);
        System.out.println("查询时间: " + queryTime + " ms");
        System.out.println("距离计算次数: " + result.distanceCount);
        System.out.println("查询结果数量: " + result.results.size());
        
        if (result.results.isEmpty()) {
            System.out.println("无匹配结果");
        } else {
            System.out.print("是否显示查询结果？(y/n): ");
            if ("y".equalsIgnoreCase(scanner.nextLine())) {
                System.out.println("\n查询结果详情:");
                int displayLimit = Math.min(10, result.results.size());
                for (int i = 0; i < displayLimit; i++) {
                    MetricData data = result.results.get(i);
                    if (isVector) {
                        VectorData vector = (VectorData) data;
                        System.out.printf("结果%d: %s\n", i+1, Arrays.toString(vector.getVector()));
                    } else {
                        StringData stringData = (StringData) data;
                        System.out.printf("结果%d: %s\n", i+1, stringData.getValue());
                    }
                }
                if (result.results.size() > displayLimit) {
                    System.out.println("... (共" + result.results.size() + "个结果，只显示前" + displayLimit + "个)");
                }
            }
        }
    }
    
    // 辅助方法：统计树中的数据总量
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
     * 从树中获取所有数据
     */
    private static List<MetricData> getAllDataFromTree(Object node) {
        List<MetricData> allData = new ArrayList<>();
        collectDataFromTree(node, allData);
        return allData;
    }
    
    /**
     * 递归收集树中所有数据
     */
    private static void collectDataFromTree(Object node, List<MetricData> collector) {
        if (node instanceof PivotTable) {
            PivotTable pt = (PivotTable) node;
            collector.addAll(pt.getPivots());
            collector.addAll(pt.getDataPoints());
        } 
        else if (node instanceof GeneralHyperPlaneTree.GHInternalNode) {
            GeneralHyperPlaneTree.GHInternalNode internal = (GeneralHyperPlaneTree.GHInternalNode) node;
            // GHT内部节点有两个支撑点
            collector.add(internal.pivot1);
            collector.add(internal.pivot2);
            if (internal.left != null) collectDataFromTree(internal.left, collector);
            if (internal.right != null) collectDataFromTree(internal.right, collector);
        }
        else if (node instanceof VPTPivotsNum_limit.VPTInternalNode) {
            VPTPivotsNum_limit.VPTInternalNode internal = (VPTPivotsNum_limit.VPTInternalNode) node;
            collector.add(internal.pivot);
            if (internal.left != null) collectDataFromTree(internal.left, collector);
            if (internal.right != null) collectDataFromTree(internal.right, collector);
        }
        else if (node instanceof VantagePointTree.VPTInternalNode) {
            VantagePointTree.VPTInternalNode internal = (VantagePointTree.VPTInternalNode) node;
            collector.add(internal.pivot);
            if (internal.left != null) collectDataFromTree(internal.left, collector);
            if (internal.right != null) collectDataFromTree(internal.right, collector);
        }
    }
}