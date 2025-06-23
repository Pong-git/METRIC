package test;

import dataload.StringReader;
import dataload.VectorReader;
import dataload.ProteinReader;
import db.MetricData;
import db.StringData;
import db.VectorData;
import distance_function.*;
import distance_function.matrix.mPAM;
import search.KnnSearch;

import java.io.IOException;
import java.util.*;

public class Test_2 {

    public static void main(String[] args) throws IOException {
        try (Scanner scanner = new Scanner(System.in)) {

            // Step 0: 选择数据类型
            System.out.print("请输入数据类型（vector/string/protein）：");
            String dataType = scanner.nextLine().trim().toLowerCase();

            List<MetricData> dataset = new ArrayList<>();
            int dim = 0;

            // Step 1: 根据类型加载数据
            System.out.print("请输入数据文件路径: ");
            String path = scanner.nextLine();

            switch (dataType) {
                case "vector":
                    System.out.print("请输入要读取的维度: ");
                    dim = Integer.parseInt(scanner.nextLine());
                    System.out.print("请输入要读取的向量个数: ");
                    int count = Integer.parseInt(scanner.nextLine());

                    VectorReader vr = new VectorReader();
                    dataset = new ArrayList<>(vr.load(path, dim, count));
                    System.out.println("已加载 " + dataset.size() + " 条向量数据。\n");
                    break;

                case "string":
                    System.out.print("请输入要读取的字符串个数: ");
                    int countStr = Integer.parseInt(scanner.nextLine());

                    StringReader sr = new StringReader();
                    dataset = new ArrayList<>(sr.load(path, countStr));
                    dim = 0; // 字符串类型不使用维度约束
                    System.out.println("已加载 " + dataset.size() + " 条字符串数据。\n");
                    break;

                    case "protein":
                    System.out.print("请输入要读取的蛋白质序列个数: ");
                    int countProt = Integer.parseInt(scanner.nextLine());

                    ProteinReader pr = new ProteinReader();
                    dataset = new ArrayList<>(pr.load(path, countProt));
                    dim = 0; // 蛋白质字符串同样不约束维度
                    System.out.println("已加载 " + dataset.size() + " 条蛋白质序列数据。\n");
                    break;

                default:
                    System.out.println("不支持的数据类型: " + dataType);
                    return;
            }

            // Step 2: 查询循环
            while (true) {
                System.out.print("请输入查询对象（以空格分隔或完整字符串），或输入 exit 结束程序: ");
                String queryInput = scanner.nextLine().trim();
                if (queryInput.equalsIgnoreCase("exit")) break;

                MetricData query;
                if (dataType.equals("vector")) {
                    String[] queryParts = queryInput.split("\\s+");
                    if (queryParts.length != dim) {
                        System.out.println("输入维度错误，应为 " + dim + " 维。\n");
                        continue;
                    }
                    double[] vec = new double[dim];
                    for (int i = 0; i < dim; i++) vec[i] = Double.parseDouble(queryParts[i]);
                    query = new VectorData(vec);
                } else {
                    query = new StringData(queryInput);
                }

                // Step 3: 距离函数选择
                MetricDistance distance;
                System.out.println("请选择距离函数: \n" +
                        "1=闵可夫斯基 (vector)\n" +
                        "2=孤点距离 (vector)\n" +
                        "3=汉明距离 (string)\n" +
                        "4=编辑距离 (string)\n" +
                        "5=加权编辑距离 (protein)\n" +
                        "其他=默认欧几里得（vector）");
                String option = scanner.nextLine().trim();

                switch (option) {
                    case "1":
                        System.out.print("请输入 p 值（如 1, 2, 3, ...inf）: ");
                        String pStr = scanner.nextLine().trim().toLowerCase();
                        double p;
                        if (pStr.equals("inf") || pStr.equals("infinity")) {
                            p = Double.POSITIVE_INFINITY;
                        } else {
                            try {
                                p = Double.parseDouble(pStr);
                            } catch (NumberFormatException e) {
                                System.out.println("非法的 p 值输入，默认使用 L2 距离（欧几里得）");
                                p = 2.0;
                            }
                        }
                        distance = new LDistance(p);
                        break;
                    case "2":
                        distance = new LoneDistance();
                        break;
                    case "3":
                        distance = new HammingDistance();
                        break;
                    case "4":
                        distance = new EditDistance();
                        break;
                    case "5":
                        distance = new WeightedEditDistance(new mPAM());
                        break;
                    default:
                        distance = new LDistance();  // 默认欧几里得
                        break;
                }

                // Step 4: 支撑点和查询循环
                while (true) {
                    System.out.print("请输入支撑点（与查询对象格式一致），或输入 back 返回上层: ");
                    String pivotInput = scanner.nextLine().trim();
                    if (pivotInput.equalsIgnoreCase("back")) break;

                    MetricData pivot;
                    if (dataType.equals("vector")) {
                        String[] pivotParts = pivotInput.split("\\s+");
                        if (pivotParts.length != dim) {
                            System.out.println("输入维度错误，应为 " + dim + " 维。\n");
                            continue;
                        }
                        double[] vec = new double[dim];
                        for (int i = 0; i < dim; i++) vec[i] = Double.parseDouble(pivotParts[i]);
                        pivot = new VectorData(vec);
                    } else {
                        pivot = new StringData(pivotInput);
                    }

                    // 预计算阶段（不计入距离计算）
                    Map<MetricData, Double> pivotDistances =
                            KnnSearch.precomputePivotDistances(dataset, pivot, distance);

                    // 执行查询
                    KnnSearch knn = new KnnSearch();
                    knn.searchK1(query, dataset, distance, pivot, pivotDistances);
                }
            }

            System.out.println("程序结束。");
        }
    }
}
