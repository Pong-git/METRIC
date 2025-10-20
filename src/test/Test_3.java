package test;

import dataload.StringReader;
import dataload.VectorReader;
import dataload.ProteinReader;
import db.MetricData;
import db.StringData;
import db.VectorData;
import distance_function.*;
import distance_function.matrix.mPAM;
import index.*;
import algorithms.pivotselection.*;
import search.*;

import java.io.IOException;
import java.util.*;

public class Test_3 {

    private static final Map<String, String> DATASET_PATHS = Map.of(
        "clusteredvector-2d-100k-100c", "D:\\code\\metric_pro\\dataset\\clusteredvector-2d-100k-100c.txt",
        "uniformvector-20dim-1m", "D:\\code\\metric_pro\\dataset\\uniformvector-20dim-1m.txt",
        "English", "D:\\code\\metric_pro\\dataset\\English.dic",
        "yeast", "D:\\code\\metric_pro\\dataset\\yeast.aa",
        "test","C:\\Users\\q'w\\Desktop\\test_metric.txt"
    );

    private static final Map<String, String> DATASET_TYPES = Map.of(
        "clusteredvector-2d-100k-100c", "vector",
        "uniformvector-20dim-1m", "vector",
        "English", "string",
        "yeast", "protein",
        "test", "vector"
    );

    public static void main(String[] args) throws IOException {
        try (Scanner scanner = new Scanner(System.in)) {

            // 1. 数据集选择
            System.out.println("可用数据集:");
            int idx = 0;
            List<String> datasetNames = new ArrayList<>(DATASET_PATHS.keySet());
            for (String name : datasetNames) {
                System.out.printf("[%d] %s\n", idx++, name);
            }
            System.out.print("请选择数据集编号：");
            int datasetChoice = Integer.parseInt(scanner.nextLine());
            String datasetName = datasetNames.get(datasetChoice);
            String datasetPath = DATASET_PATHS.get(datasetName);
            String dataType = DATASET_TYPES.get(datasetName);

            boolean isVector = "vector".equals(dataType);
            boolean isProtein = "protein".equals(dataType);
            boolean isString = "string".equals(dataType);

            // 2. 加载参数
            System.out.print("请输入加载数据的数量：");
            int dataCount = Integer.parseInt(scanner.nextLine());
            int dim = 0;
            if (isVector) {
                System.out.print("请输入向量维度：");
                dim = Integer.parseInt(scanner.nextLine());
            }

            // 3. 加载数据
            List<MetricData> dataset;
            if (isVector) {
                dataset = new ArrayList<>(new VectorReader().load(datasetPath, dim, dataCount));
            } else if (isProtein) {
                dataset = new ArrayList<>(new ProteinReader().load(datasetPath, dataCount));
            } else {
                dataset = new ArrayList<>(new StringReader().load(datasetPath, dataCount));
            }
            System.out.println("已加载数据条数: " + dataset.size());

            // 4. 距离函数选择
            MetricDistance distance;
            if (isVector) {
                System.out.println("请选择距离函数:");
                System.out.println("[0] 闵可夫斯基距离 (需输入p值)");
                System.out.println("[1] 孤点距离");
                System.out.print("输入编号选择：");
                int distOpt = Integer.parseInt(scanner.nextLine());

                switch (distOpt) {
                    case 0:
                        System.out.print("请输入p值 (e.g. 1, 2, 3, inf)：");
                        String pStr = scanner.nextLine().toLowerCase();
                        double p;
                        if ("inf".equals(pStr) || "infinity".equals(pStr)) {
                            p = Double.POSITIVE_INFINITY;
                        } else {
                            try {
                                p = Double.parseDouble(pStr);
                            } catch (NumberFormatException e) {
                                System.out.println("非法p值，默认2");
                                p = 2.0;
                            }
                        }
                        distance = new LDistance(p);
                        break;
                    case 1:
                        distance = new LoneDistance();
                        break;
                    default:
                        System.out.println("默认欧几里得距离");
                        distance = new LDistance(2.0);
                }
            } else if (isProtein) {
                distance = new WeightedEditDistance(new mPAM());
            } else {
                System.out.println("请选择距离函数:");
                System.out.println("[0] 汉明距离");
                System.out.println("[1] 编辑距离");
                System.out.print("输入编号选择：");
                int distOpt = Integer.parseInt(scanner.nextLine());

                switch (distOpt) {
                    case 0:
                        distance = new HammingDistance();
                        break;
                    case 1:
                        distance = new EditDistance();
                        break;
                    default:
                        System.out.println("默认编辑距离");
                        distance = new EditDistance();
                }
            }

            // 5. 支撑点选择策略
            System.out.println("请选择支撑点选择算法:");
            System.out.println("[0] 随机选择");
            System.out.print("输入编号选择：");
            int pivotOpt = Integer.parseInt(scanner.nextLine());
            PivotSelectionMethod pivotSelector;
            switch (pivotOpt) {
                case 0:
                    pivotSelector = new RandomSelection();
                    break;
                default:
                    System.out.println("默认随机选择");
                    pivotSelector = new RandomSelection();
            }

            // 6. 索引选择
            System.out.println("请选择索引结构:");
            System.out.println("[0] GHT");
            System.out.println("[1] VPT");
            System.out.println("[2] MVPT");
            System.out.println("[3] PivotTable");
            System.out.print("输入编号选择：");
            int indexOpt = Integer.parseInt(scanner.nextLine());

            System.out.print("请输入叶子节点最大容量: ");
            int maxLeafSize = Integer.parseInt(scanner.nextLine());

            System.out.print("请输入叶子节点的支撑点数量: ");
            int numPivots = Integer.parseInt(scanner.nextLine());

            Object indexRoot;
            switch (indexOpt) {
                case 0:
                    indexRoot = GeneralHyperPlaneTree.GHBulkLoad(dataset, maxLeafSize, distance, pivotSelector, numPivots);
                    break;
                case 1:
                    indexRoot = VantagePointTree.VPBulkLoad(dataset, maxLeafSize, distance, pivotSelector, numPivots);
                    break;
                case 2:
                    System.out.print("请输入 MVPT 内部节点的支撑点数量: ");
                    int internal_numPivots = Integer.parseInt(scanner.nextLine());
                    System.out.print("请输入每个支撑点划分的区域数: ");
                    int numRegions = Integer.parseInt(scanner.nextLine());
                    indexRoot = MultipleVantagePointTree.MVPBulkLoad(dataset, maxLeafSize, distance, pivotSelector,
                            numPivots, numRegions, internal_numPivots);
                    break;
                case 3:
                    indexRoot = new PivotTable(dataset, maxLeafSize, numPivots, pivotSelector, distance);
                    break;
                default:
                    System.out.println("无效索引结构，程序退出");
                    return;
            }

            // 7. 查询循环
            while (true) {
                distance.resetCount();
                System.out.print("请输入查询半径，或输入exit退出: ");
                String rInput = scanner.nextLine().trim();
                if (rInput.equalsIgnoreCase("exit")) break;

                double radius;
                try {
                    radius = Double.parseDouble(rInput);
                } catch (NumberFormatException e) {
                    System.out.println("非法半径输入");
                    continue;
                }

                System.out.print(isVector ? "请输入查询向量(空格分隔): " : "请输入查询字符串: ");
                String queryInput = scanner.nextLine();
                MetricData query;

                if (isVector) {
                    String[] parts = queryInput.split("\\s+");
                    if (parts.length != dim) {
                        System.out.println("查询向量维度错误，应为 " + dim);
                        continue;
                    }
                    double[] vec = new double[dim];
                    try {
                        for (int i = 0; i < dim; i++) {
                            vec[i] = Double.parseDouble(parts[i]);
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("非法数字输入");
                        continue;
                    }
                    query = new VectorData(vec);
                } else {
                    query = new StringData(queryInput);
                }

                // 8. 查询分发
                SearchResult res = null;
                switch (indexOpt) {
                    case 0:
                        res = GHRangeSearch.rangeSearch(indexRoot, query, radius, distance);
                        break;
                    case 1:
                        res = VPRangeSearch.rangeSearch(indexRoot, query, radius, distance);
                        break;
                    case 2:
                        res = MVPRangeSearch.rangeSearch(indexRoot, query, radius, distance);
                        break;
                    case 3:
                        res = PTRangeSearch.rangeSearch((PivotTable) indexRoot, query, radius, distance);
                        break;
                    default:
                        res = null;
                }

                if (res == null) {
                    System.out.println("无匹配结果");
                    continue;
                }

                System.out.printf("查询结果数：%d ，距离计算次数：%d\n", res.results.size(), distance.getCount());
                System.out.print("是否输出查询结果(y/n): ");
                if ("y".equalsIgnoreCase(scanner.nextLine())) {
                    for (MetricData d : res.results) {
                        System.out.println(d);
                    }
                }
            }
            System.out.println("程序结束。");
        }
    }
}
