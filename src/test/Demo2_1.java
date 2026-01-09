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
import search.*;

import java.io.IOException;
import java.util.*;

public class Demo2_1 {

    private static final Map<String, String> DATASET_PATHS = Map.of(
        "1", "D:\\code\\metric_pro\\dataset\\uniformvector-20dim-1m.txt",
        "2", "D:\\code\\metric_pro\\dataset\\yeast.aa", 
        "3", "D:\\code\\metric_pro\\dataset\\test_vector.txt",
        "4", "D:\\code\\metric_pro\\dataset\\test_protein.txt"
    );

    private static final Map<String, String> DATASET_TYPES = Map.of(
        "1", "vector",
        "2", "protein",
        "3", "vector",
        "4","protein"
    );

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.println("\n=== 度量空间查询系统测试 ===");
            System.out.println("请选择要测试的数据集:");
            System.out.println("[1] uniformvector-20dim-1m (向量数据)");
            System.out.println("[2] yeast (蛋白质序列数据)");
            System.out.println("[3] test_vector (测试向量数据)");
            System.out.println("[4] test_protein (测试蛋白质序列数据)");
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
                    
                    // 显示所有读取的向量数据
                    System.out.println("\n=== 所有读取的向量数据 ===");
                    for (int i = 0; i < vectors.size(); i++) {
                        System.out.println("向量" + (i+1) + ": " + vectors.get(i));
                    }
                } else if (isProtein) {
                    ProteinReader proteinReader = new ProteinReader();
                    dataList = proteinReader.load(filePath, count);
                    System.out.println("\n成功加载 " + dataList.size() + " 个蛋白质序列");
                    
                    // 显示所有读取的蛋白质序列
                    System.out.println("\n=== 所有读取的蛋白质序列 ===");
                    for (int i = 0; i < dataList.size(); i++) {
                        StringData seq = (StringData) dataList.get(i);
                        String sequence = seq.getValue();
                        System.out.println("序列" + (i+1) + " (长度" + sequence.length() + "): " + sequence);
                    }
                } else {
                    throw new IllegalArgumentException("不支持的数据类型");
                }
            } catch (Exception e) {
                System.out.println("数据加载失败: " + e.getMessage());
                continue;
            }
            
            // 距离函数设置循环
            MetricDistance distance = null;
            while (distance == null) {
                if (isVector) {
                    System.out.print("请输入闵可夫斯基距离的p值 (1=曼哈顿, 2=欧式, inf=切比雪夫) 或输入back返回数据集选择: ");
                    String pInput = scanner.nextLine().trim().toLowerCase();
                    
                    if ("back".equals(pInput)) {
                        break;
                    }
                    
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
                    break;
                }
            }
            
            // 如果用户选择back，重新选择数据集
            if (distance == null) {
                continue;
            }
            
            // 查询循环
            while (true) {
                System.out.println("\n=== 查询方式选择 ===");
                System.out.println("[1] 范围查询 (Range Search)");
                System.out.println("[2] k近邻查询 (kNN Search)");
                System.out.println("[3] 距离受限k近邻查询 (DkNN Search)");
                System.out.println("[back] 返回数据集选择");
                System.out.println("[exit] 退出程序");
                System.out.print("请选择查询方式: ");
                
                String queryChoice = scanner.nextLine().trim().toLowerCase();
                
                if ("exit".equals(queryChoice)) {
                    System.out.println("程序结束");
                    scanner.close();
                    return;
                }
                if ("back".equals(queryChoice)) {
                    break;
                }
                
                // 获取查询点
                MetricData query = null;
                while (query == null) {
                    if (isVector) {
                        System.out.print("请输入查询向量(空格分隔): ");
                        String queryInput = scanner.nextLine().trim();
                        
                        if ("back".equals(queryInput.toLowerCase())) {
                            break;
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
                            query = new VectorData(vec);
                        } catch (NumberFormatException e) {
                            System.out.println("非法数字输入，请重新输入");
                        }
                    } else {
                        System.out.print("请输入查询序列 或输入back返回查询方式选择: ");
                        String queryInput = scanner.nextLine().trim();
                        
                        if ("back".equals(queryInput.toLowerCase())) {
                            break;
                        }
                        
                        query = new StringData(queryInput);
                    }
                }
                
                // 如果用户选择back，重新选择查询方式
                if (query == null) {
                    continue;
                }
                
                // 执行查询
                switch (queryChoice) {
                    case "1":
                        // 范围查询
                        System.out.print("请输入查询半径: ");
                        double radius = Double.parseDouble(scanner.nextLine());
                        SearchResult rangeResult = LinearRangeSearch.rangeSearch(dataList, query, radius, distance);
                        displayResult("范围查询", rangeResult, isVector);
                        break;
                        
                    case "2":
                        // k近邻查询
                        System.out.print("请输入k值: ");
                        int k = Integer.parseInt(scanner.nextLine());
                        SearchResult knnResult = LinearKnnSearch.knnSearch(dataList, query, k, distance);
                        displayResult(k + "近邻查询", knnResult, isVector);
                        break;
                        
                    case "3":
                        // 距离受限k近邻查询
                        System.out.print("请输入k值: ");
                        int dk = Integer.parseInt(scanner.nextLine());
                        System.out.print("请输入查询半径: ");
                        double dkRadius = Double.parseDouble(scanner.nextLine());
                        SearchResult dknnResult = LinearDknnSearch.dknnSearch(dataList, query, dk, dkRadius, distance);
                        displayResult("距离受限" + dk + "近邻查询", dknnResult, isVector);
                        break;
                        
                    default:
                        System.out.println("无效选择，请重新输入");
                        continue;
                }
                
                // 重置距离计数器
                distance.resetCount();
            }
        }
        scanner.close();
    }
    
    /**
     * 显示查询结果
     */
    private static void displayResult(String queryType, SearchResult result, boolean isVector) {
        System.out.println("\n=== " + queryType + "结果 ===");
        System.out.println("距离计算次数: " + result.distanceCount);
        System.out.println("查询结果数量: " + result.results.size());
        
        if (result.results.isEmpty()) {
            System.out.println("无匹配结果");
            return;
        }
        
        System.out.println("\n查询结果详情:");
        for (int i = 0; i < result.results.size(); i++) {
            MetricData data = result.results.get(i);
            if (isVector) {
                VectorData vector = (VectorData) data;
                System.out.printf("结果%d: %s\n", i+1, Arrays.toString(vector.getVector()));
            } else {
                StringData stringData = (StringData) data;
                System.out.printf("结果%d: %s\n", i+1, stringData.getValue());
            }
        }
    }
}