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

import java.io.IOException;
import java.util.*;

public class Demo1 {

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
        "4", "protein"
    );

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.println("\n=== 度量空间数据处理系统验证测试 ===");
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
            
            // 计算并显示前至多5个数据对之间的距离
            System.out.println("\n=== 前" + Math.min(5, dataList.size()) + "个数据对象的两两距离 ===");
            
            int displayCount = Math.min(5, dataList.size());
            for (int i = 0; i < displayCount; i++) {
                for (int j = i + 1; j < displayCount; j++) {
                    MetricData data1 = dataList.get(i);
                    MetricData data2 = dataList.get(j);
                    
                    double dist = distance.getDistance(data1, data2);
                    
                    // 显示具体是哪些数据对象之间的距离
                    System.out.printf("\n距离计算 %d-%d:\n", i+1, j+1);
                    if (isVector) {
                        VectorData vec1 = (VectorData) data1;
                        VectorData vec2 = (VectorData) data2;
                        System.out.println("  数据1: " + Arrays.toString(vec1.getVector()));
                        System.out.println("  数据2: " + Arrays.toString(vec2.getVector()));
                    } else {
                        StringData str1 = (StringData) data1;
                        StringData str2 = (StringData) data2;
                        System.out.println("  数据1: " + str1.getValue());
                        System.out.println("  数据2: " + str2.getValue());
                    }
                    System.out.printf("  距离: %.4f\n", dist);
                }
            }
        }
        scanner.close();
    }
}