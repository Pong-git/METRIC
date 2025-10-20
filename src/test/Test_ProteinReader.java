package test;

import dataload.ProteinReader;
import db.MetricData;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class Test_ProteinReader {

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            // 1. 输入文件路径和读取数量
            System.out.print("请输入蛋白质数据文件路径: ");
            String path = scanner.nextLine();

            System.out.print("请输入要读取的蛋白质序列个数: ");
            int count = Integer.parseInt(scanner.nextLine());

            try {
                // 2. 创建 reader 并读取数据
                ProteinReader reader = new ProteinReader();
                List<MetricData> sequences = reader.load(path, count);

                // 3. 输出结果
                System.out.println("\n成功读取的蛋白质序列如下：");
                int index = 1;
                for (MetricData data : sequences) {
                    System.out.println("[" + index++ + "] " + data.getRawData());
                }

            } catch (IOException e) {
                System.out.println("读取文件时发生错误: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.out.println("读取参数错误: " + e.getMessage());
            }
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        System.out.println("\n测试结束。");
    }
}
