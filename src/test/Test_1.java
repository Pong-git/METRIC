package test;

import db.MetricData;
import db.VectorData;
import dataload.VectorReader;
import distance_function.*;
import search.KnnSearch;

import java.io.IOException;
import java.util.*;

public class Test_1 {

    public static void main(String[] args) throws IOException {
        try (Scanner scanner = new Scanner(System.in)) {
            // Step 1: 读取数据集
            System.out.print("请输入数据文件路径: ");
            String path = scanner.nextLine();
            System.out.print("请输入要读取的维度: ");
            int dim = scanner.nextInt();
            System.out.print("请输入要读取的向量个数: ");
            int count = scanner.nextInt();
            scanner.nextLine(); // 处理换行符

            VectorReader reader = new VectorReader();
            List<MetricData> dataset = new ArrayList<>(reader.load(path, dim, count));

            System.out.println("数据加载完成，共加载 " + dataset.size() + " 条向量数据。\n");

            // Step 2: 查询循环
            while (true) {
                System.out.print("请输入查询点（以空格分隔），或输入 exit 结束程序: ");
                String queryLine = scanner.nextLine();
                if (queryLine.trim().equalsIgnoreCase("exit")) break;

                String[] queryParts = queryLine.trim().split("\\s+");
                if (queryParts.length != dim) {
                    System.out.println("维度错误，应为 " + dim + " 维。\n");
                    continue;
                }

                double[] queryVec = new double[dim];
                for (int i = 0; i < dim; i++) queryVec[i] = Double.parseDouble(queryParts[i]);
                MetricData query = new VectorData(queryVec);

                // 选择距离函数
                MetricDistance distance;
                System.out.println("请选择距离函数: 1=闵可夫斯基, 2=孤点距离, 其他=默认欧几里得");
                String option = scanner.nextLine().trim();
                if (option.equals("1")) {
                    System.out.print("请输入闵可夫斯基距离的 p 值（如 1, 2, 3, ...）: ");
                    int p = scanner.nextInt();
                    scanner.nextLine();
                    distance = new LDistance(p);
                } else if (option.equals("2")) {
                    distance = new LoneDistance();
                } else {
                    distance = new LDistance(); // 默认 L2
                }

                // Step 3: 支撑点和最近邻查询循环
                while (true) {
                    System.out.print("请输入支撑点（以空格分隔），或输入 back 返回上层: ");
                    String pivotLine = scanner.nextLine();
                    if (pivotLine.trim().equalsIgnoreCase("back")) break;

                    String[] pivotParts = pivotLine.trim().split("\\s+");
                    if (pivotParts.length != dim) {
                        System.out.println("维度错误，应为 " + dim + " 维。\n");
                        continue;
                    }

                    double[] pivotVec = new double[dim];
                    for (int i = 0; i < dim; i++) pivotVec[i] = Double.parseDouble(pivotParts[i]);
                    MetricData pivot = new VectorData(pivotVec);

                    // 预计算支撑点与数据集中所有点的距离（不计入计数）
                    Map<MetricData, Double> pivotDistances = KnnSearch.precomputePivotDistances(dataset, pivot, distance);

                    // 执行查询
                    KnnSearch knn = new KnnSearch();
                    knn.searchK1(query, dataset, distance, pivot, pivotDistances);
                }
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        System.out.println("程序结束。");
    }
}
