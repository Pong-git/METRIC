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
import algorithms.pivotselection.SelfSelection;
import index.PivotTable;
import search.PTRangeSearch;
import search.SearchResult;

import java.io.IOException;
import java.util.*;

public class Demo2_2 {

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
            System.out.println("\n=== 支撑点表结构索引查询测试 ===");
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
            
            // 构建支撑点表结构索引
            System.out.println("\n=== 构建支撑点表结构索引 ===");
            System.out.print("请输入支撑点数量: ");
            int numPivots = Integer.parseInt(scanner.nextLine());
            
            System.out.print("请输入叶子节点最大容量 (建议 >= 数据数量): ");
            int maxLeafSize = Integer.parseInt(scanner.nextLine());
            
            // 使用SelfSelection选择支撑点
            SelfSelection pivotSelector = new SelfSelection(scanner);
            
            System.out.println("开始构建PivotTable索引...");
            
            PivotTable pivotTable = new PivotTable(dataList, maxLeafSize, numPivots, pivotSelector, distance);
            
            System.out.println("索引构建完成！");
            System.out.println("支撑点数量: " + pivotTable.getPivots().size());
            System.out.println("数据点数量: " + pivotTable.size());
            System.out.println("索引构建阶段距离计算次数: " + distance.getCount());
            
            // 重置距离计数器，准备查询
            distance.resetCount();
            
            // 查询循环
            while (true) {
                System.out.println("\n=== 支撑点表结构查询 ===");
                System.out.print("请输入查询半径，或输入back返回数据集选择: ");
                String radiusInput = scanner.nextLine().trim().toLowerCase();
                
                if ("back".equals(radiusInput)) {
                    break;
                }
                
                double radius;
                try {
                    radius = Double.parseDouble(radiusInput);
                } catch (NumberFormatException e) {
                    System.out.println("非法半径输入");
                    continue;
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
                        System.out.print("请输入查询序列 或输入back返回半径输入: ");
                        String queryInput = scanner.nextLine().trim();
                        
                        if ("back".equals(queryInput.toLowerCase())) {
                            break;
                        }
                        
                        query = new StringData(queryInput);
                    }
                }
                
                // 如果用户选择back，重新输入半径
                if (query == null) {
                    continue;
                }
                
                // 执行查询
                System.out.println("开始查询...");
                long queryStartTime = System.currentTimeMillis();
                
                SearchResult result = PTRangeSearch.rangeSearch(pivotTable, query, radius, distance);
                
                long queryEndTime = System.currentTimeMillis();
                long queryTime = queryEndTime - queryStartTime;
                
                // 显示查询结果
                System.out.println("\n=== 查询结果 ===");
                System.out.println("查询时间: " + queryTime + " ms");
                System.out.println("查询阶段距离计算次数: " + result.distanceCount);
                System.out.println("查询结果数量: " + result.results.size());
                
                if (result.results.isEmpty()) {
                    System.out.println("无匹配结果");
                } else {
                    System.out.println("\n查询结果详情:");
                    for (int i = 0; i < Math.min(10, result.results.size()); i++) {
                        MetricData data = result.results.get(i);
                        if (isVector) {
                            VectorData vector = (VectorData) data;
                            System.out.printf("结果%d: %s\n", i+1, Arrays.toString(vector.getVector()));
                        } else {
                            StringData stringData = (StringData) data;
                            System.out.printf("结果%d: %s\n", i+1, stringData.getValue());
                        }
                    }
                    if (result.results.size() > 10) {
                        System.out.println("... (共" + result.results.size() + "个结果，只显示前10个)");
                    }
                }
                
                // 重置距离计数器，准备下一次查询
                distance.resetCount();
            }
        }
        scanner.close();
    }
}