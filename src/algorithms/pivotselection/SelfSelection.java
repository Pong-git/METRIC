package algorithms.pivotselection;

import db.MetricData;
import distance_function.MetricDistance;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * 用户自定义支撑点选择算法。
 * 允许用户手动指定要作为支撑点的数据对象。
 */
public class SelfSelection implements PivotSelectionMethod {

    private Scanner scanner;

    /**
     * 默认构造函数，使用System.in
     */
    public SelfSelection() {
        this.scanner = new Scanner(System.in);
    }

    /**
     * 构造函数，传入共享的Scanner对象
     */
    public SelfSelection(Scanner scanner) {
        this.scanner = scanner;
    }

    /**
     * 从数据集中选择用户指定的支撑点。
     *
     * @param dataSet 数据集
     * @param numPivots 支撑点数量
     * @param distance 距离函数
     * @return 用户选择的支撑点列表
     */
    @Override
    public List<MetricData> selectPivots(List<MetricData> dataSet, int numPivots, MetricDistance distance) {
        if (dataSet == null || dataSet.isEmpty()) {
            throw new IllegalArgumentException("数据集不能为空");
        }
        if (numPivots <= 0 || numPivots > dataSet.size()) {
            throw new IllegalArgumentException("支撑点数量必须在1到数据集大小之间");
        }

        List<MetricData> selectedPivots = new ArrayList<>();
        
        System.out.println("\n=== 自定义支撑点选择 ===");
        System.out.println("数据集大小: " + dataSet.size());
        System.out.println("需要选择 " + numPivots + " 个支撑点");
        
        // 显示数据集中的前一些数据点供用户参考
        System.out.println("\n数据集预览 (前" + Math.min(10, dataSet.size()) + "个数据点):");
        for (int i = 0; i < Math.min(10, dataSet.size()); i++) {
            System.out.println("[" + i + "] " + dataSet.get(i));
        }
        if (dataSet.size() > 10) {
            System.out.println("... (共" + dataSet.size() + "个数据点)");
        }

        // 用户选择支撑点
        for (int i = 0; i < numPivots; i++) {
            while (true) {
                System.out.print("\n请选择第 " + (i + 1) + " 个支撑点的索引 (0-" + (dataSet.size() - 1) + "): ");
                
                try {
                    int index = Integer.parseInt(scanner.nextLine().trim());
                    
                    if (index < 0 || index >= dataSet.size()) {
                        System.out.println("索引超出范围，请输入 0 到 " + (dataSet.size() - 1) + " 之间的数字");
                        continue;
                    }
                    
                    MetricData selectedPivot = dataSet.get(index);
                    
                    // 检查是否已经选择过该点
                    if (selectedPivots.contains(selectedPivot)) {
                        System.out.println("该数据点已被选择，请选择其他数据点");
                        continue;
                    }
                    
                    selectedPivots.add(selectedPivot);
                    System.out.println("已选择: [" + index + "] " + selectedPivot);
                    break;
                    
                } catch (NumberFormatException e) {
                    System.out.println("请输入有效的数字");
                }
            }
        }
        
        System.out.println("\n支撑点选择完成！");
        System.out.println("选择的支撑点:");
        for (int i = 0; i < selectedPivots.size(); i++) {
            System.out.println("支撑点" + (i + 1) + ": " + selectedPivots.get(i));
        }
        
        return selectedPivots;
    }

    /**
     * 不要关闭Scanner，由主程序负责关闭
     */
    public void closeScanner() {
        // 不关闭scanner，由主程序管理
    }
}