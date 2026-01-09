package algorithms.pivotselection;

import db.MetricData;
import distance_function.MetricDistance;

import java.util.*;

/**
 * 全局支撑点池选择器
 * 预先选出所有支撑点，然后按顺序分配给各个节点
 * 确保返回的支撑点在当前数据集中，同时保证GHT和MVPT使用相同的支撑点序列
 */
public class PivotPoolSelection implements PivotSelectionMethod {
    
    private final List<MetricData> pivotPool;    // 全局支撑点池
    private int nextIndex = 0;                   // 下一个要检查的支撑点索引
    private final Map<String, List<MetricData>> allocationCache; // 分配缓存
    private final Random random;                 // 用于本地补充选择
    
    /**
     * 构造函数：从完整数据集中预选所有支撑点
     */
    public PivotPoolSelection(List<MetricData> fullDataset,
                             int totalPivots,
                             PivotSelectionMethod baseSelector,
                             MetricDistance distance) {
        if (fullDataset == null || fullDataset.isEmpty()) {
            throw new IllegalArgumentException("数据集不能为空");
        }
        if (totalPivots <= 0) {
            totalPivots = 1;
            System.out.println("[PivotPoolSelection] 警告：支撑点数量必须大于0，已调整为1");
        } else if (totalPivots > fullDataset.size()) {
            totalPivots = fullDataset.size();
            System.out.println("[PivotPoolSelection] 警告：支撑点数量超过数据集大小，已调整为" + totalPivots);
        }
        
        // 确保参数不为null
        if (baseSelector == null) {
            throw new IllegalArgumentException("基础选择器不能为null");
        }
        if (distance == null) {
            throw new IllegalArgumentException("距离函数不能为null");
        }
        
        // 预选所有支撑点
        this.pivotPool = baseSelector.selectPivots(fullDataset, totalPivots, distance);
        this.allocationCache = new HashMap<>();
        this.random = new Random(42); // 固定种子保证可重现
        
        System.out.println("[PivotPoolSelection] 创建全局支撑点池，包含 " + pivotPool.size() + " 个支撑点");
    }
    
    /**
     * 从全局池中按顺序分配支撑点，确保支撑点在当前数据集中
     * 如果全局池中的支撑点不在当前数据集，从当前数据集中补充
     */
    @Override
    public List<MetricData> selectPivots(List<MetricData> currentDataset,
                                       int numPivots,
                                       MetricDistance distance) {
        if (numPivots <= 0) {
            throw new IllegalArgumentException("需要的支撑点数量必须大于0");
        }
        if (currentDataset == null || currentDataset.isEmpty()) {
            throw new IllegalArgumentException("当前数据集不能为空");
        }
        if (pivotPool.isEmpty()) {
            throw new IllegalStateException("支撑点池为空，无法分配支撑点");
        }
        
        // 生成当前数据集的唯一标识（用于缓存）
        String datasetKey = generateDatasetKey(currentDataset, numPivots);
        
        // 检查缓存：相同数据集返回相同的支撑点
        if (allocationCache.containsKey(datasetKey)) {
            return new ArrayList<>(allocationCache.get(datasetKey));
        }
        
        List<MetricData> allocatedPivots = new ArrayList<>();
        
        // 第一阶段：从全局池中寻找在当前数据集中的支撑点
        int attempts = 0;
        int startIndex = nextIndex; // 记录起始位置
        int maxAttempts = pivotPool.size(); // 最多尝试一轮
        
        while (allocatedPivots.size() < numPivots && attempts < maxAttempts) {
            if (nextIndex >= pivotPool.size()) {
                nextIndex = 0; // 循环到开头
            }
            
            MetricData candidate = pivotPool.get(nextIndex);
            nextIndex++;
            attempts++;
            
            // 只添加在当前数据集中的支撑点
            if (currentDataset.contains(candidate)) {
                allocatedPivots.add(candidate);
            }
        }
        
        // 第二阶段：如果不够，从当前数据集中补充
        if (allocatedPivots.size() < numPivots) {
            int needed = numPivots - allocatedPivots.size();
            List<MetricData> localPivots = selectLocalPivots(currentDataset, needed, allocatedPivots);
            allocatedPivots.addAll(localPivots);
        }
        
        // 如果还是不够（不应该发生），使用所有可用的数据点
        if (allocatedPivots.size() < numPivots) {
            System.out.println("[PivotPoolSelection] 警告：无法选择足够的支撑点，返回所有可用数据");
            // 从当前数据集中取所有不同的点
            Set<MetricData> allPoints = new HashSet<>(currentDataset);
            allPoints.removeAll(allocatedPivots); // 排除已经选择的
            allocatedPivots.addAll(allPoints);
            
            // 如果还是不够，说明数据集太小，返回已有的
            if (allocatedPivots.size() < numPivots) {
                System.out.println("[PivotPoolSelection] 数据集大小不足，只返回 " + allocatedPivots.size() + " 个支撑点");
            }
        }
        
        // 确保数量正确（可能超过）
        if (allocatedPivots.size() > numPivots) {
            allocatedPivots = allocatedPivots.subList(0, numPivots);
        }
        
        // 缓存结果
        allocationCache.put(datasetKey, new ArrayList<>(allocatedPivots));
        
        return allocatedPivots;
    }
    
    /**
     * 从当前数据集中选择本地支撑点（补充用）
     */
    private List<MetricData> selectLocalPivots(List<MetricData> currentDataset,
                                             int numPivots,
                                             List<MetricData> excludePivots) {
        if (numPivots <= 0) {
            return new ArrayList<>();
        }
        
        // 创建候选列表
        List<MetricData> candidates = new ArrayList<>(currentDataset);
        
        // 排除已经选择的支撑点
        candidates.removeAll(excludePivots);
        
        // 排除已经是全局支撑点的数据（可选）
        candidates.removeAll(pivotPool);
        
        if (candidates.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 如果候选数据不够，返回所有可用的
        if (candidates.size() <= numPivots) {
            return new ArrayList<>(candidates);
        }
        
        // 随机选择指定数量的支撑点（确定性随机）
        List<MetricData> shuffled = new ArrayList<>(candidates);
        Collections.shuffle(shuffled, random);
        
        return shuffled.subList(0, numPivots);
    }
    
    /**
     * 生成数据集的唯一标识键
     */
    private String generateDatasetKey(List<MetricData> dataset, int numPivots) {
        // 结合数据集特征和需要的支撑点数量
        StringBuilder key = new StringBuilder();
        key.append("n").append(dataset.size()).append("_p").append(numPivots);
        
        // 添加数据集特征（前几个点的哈希）
        int limit = Math.min(3, dataset.size());
        List<Integer> hashes = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            hashes.add(dataset.get(i).hashCode());
        }
        Collections.sort(hashes); // 排序确保顺序一致
        
        for (int hash : hashes) {
            key.append("_").append(hash);
        }
        
        return key.toString();
    }
    
    /**
     * 获取全局支撑点池的大小
     */
    public int getPoolSize() {
        return pivotPool.size();
    }
    
    /**
     * 获取已检查的支撑点数量（不是已分配的数量）
     */
    public int getCheckedCount() {
        return nextIndex;
    }
    
    /**
     * 重置分配指针（用于构建新索引时重新开始）
     */
    public void reset() {
        nextIndex = 0;
        allocationCache.clear();
        System.out.println("[PivotPoolSelection] 重置分配指针和缓存");
    }
    
    /**
     * 获取全局支撑点池的副本
     */
    public List<MetricData> getPivotPoolCopy() {
        return new ArrayList<>(pivotPool);
    }
    
    /**
     * 检查支撑点是否在指定数据集中
     */
    public boolean isPivotInDataset(MetricData pivot, List<MetricData> dataset) {
        return dataset.contains(pivot);
    }
    
    /**
     * 获取下一个要检查的支撑点（不移除）
     */
    public MetricData peekNextPivot() {
        if (pivotPool.isEmpty()) {
            return null;
        }
        return pivotPool.get(nextIndex % pivotPool.size());
    }
    
    /**
     * 获取在指定数据集中的全局支撑点
     */
    public List<MetricData> getPivotsInDataset(List<MetricData> dataset) {
        List<MetricData> result = new ArrayList<>();
        for (MetricData pivot : pivotPool) {
            if (dataset.contains(pivot)) {
                result.add(pivot);
            }
        }
        return result;
    }
    
    /**
     * 打印支撑点池状态
     */
    public void printStatus() {
        System.out.println("=== PivotPoolSelection 状态 ===");
        System.out.println("支撑点池大小: " + pivotPool.size());
        System.out.println("已检查指针位置: " + nextIndex);
        System.out.println("缓存条目数: " + allocationCache.size());
    }
    
    /**
     * 获取分配历史（用于调试）
     */
    public Map<String, List<MetricData>> getAllocationHistory() {
        return new HashMap<>(allocationCache);
    }
}