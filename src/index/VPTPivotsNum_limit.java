package index;

import db.MetricData;
import distance_function.MetricDistance;
import algorithms.pivotselection.PivotSelectionMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 基于支撑点数量限制的VPT构建器
 * 使用GHT的支撑点构建VPT，支撑点会从数据集中移除
 */
public class VPTPivotsNum_limit {

    /**
     * VPT 内部节点
     */
    public static class VPTInternalNode {
        public MetricData pivot;
        public double splitRadius;
        public Object left;
        public Object right;

        public VPTInternalNode(MetricData pivot, double splitRadius, Object left, Object right) {
            this.pivot = pivot;
            this.splitRadius = splitRadius;
            this.left = left;
            this.right = right;
        }
        
        @Override
        public String toString() {
            return String.format("VPTInternalNode{pivot=%s, radius=%.2f}", 
                               pivot, splitRadius);
        }
    }

    /**
     * 构建结果
     */
    public static class BuildResult {
        public final Object root;
        public final int depth;
        public final int totalNodes;
        public final int pivotsUsed;
        public final int totalDataPoints;  // 树中总数据点数
        
        public BuildResult(Object root, int depth, int totalNodes, int pivotsUsed, int totalDataPoints) {
            this.root = root;
            this.depth = depth;
            this.totalNodes = totalNodes;
            this.pivotsUsed = pivotsUsed;
            this.totalDataPoints = totalDataPoints;
        }
        
        @Override
        public String toString() {
            return String.format("BuildResult{深度=%d, 节点数=%d, 使用支撑点=%d, 总数据点=%d}", 
                               depth, totalNodes, pivotsUsed, totalDataPoints);
        }
    }

    /**
     * 用于距离排序的数据结构
     */
    private static class DistancePointPair {
        double distance;
        MetricData point;

        DistancePointPair(double distance, MetricData point) {
            this.distance = distance;
            this.point = point;
        }
    }
    
    /**
     * 基于支撑点数量限制构建VPT（主入口）
     * @param data 数据集（应该已经移除了VPT会使用的支撑点）
     * @param numPivotsPerLeaf 每个叶子节点的支撑点数
     * @param distance 距离函数
     * @param pivotSequence 从GHT获取的支撑点序列
     * @return 构建结果
     */
    public static BuildResult buildWithPivotLimit(List<MetricData> data,
                                                 int numPivotsPerLeaf,
                                                 MetricDistance distance,
                                                 List<MetricData> pivotSequence) {
        if (data == null || data.isEmpty()) {
            return new BuildResult(null, 0, 0, 0, 0);
        }
        if (pivotSequence == null || pivotSequence.isEmpty()) {
            throw new IllegalArgumentException("支撑点序列不能为空");
        }
        
        System.out.println("\n[VPT] === 开始构建 ===");
        System.out.println(String.format("[VPT] 输入数据点数量: %d", data.size()));
        System.out.println(String.format("[VPT] 可用支撑点数量: %d", pivotSequence.size()));
        System.out.println(String.format("[VPT] 每个叶子节点支撑点数: %d", numPivotsPerLeaf));
        
        // 步骤1：根据支撑点数量自动计算合适的深度
        int maxDepth = calculateBestDepth(pivotSequence.size(), numPivotsPerLeaf);
        System.out.println(String.format("[VPT] 自动计算的最佳深度: %d", maxDepth));
        
        // 步骤2：计算该深度需要的支撑点数
        int neededPivots = calculatePivotsNeededForDepth(maxDepth, numPivotsPerLeaf);
        System.out.println(String.format("[VPT] 需要支撑点数: %d", neededPivots));
        
        // 步骤3：获取实际使用的支撑点（只取需要的数量）
        List<MetricData> usedPivots = getUsedPivots(pivotSequence, neededPivots);
        System.out.println(String.format("[VPT] 实际使用支撑点数: %d", usedPivots.size()));
        
        // 步骤4：按照计划构建树
        BuildStats stats = new BuildStats();
        Object root = buildRecursive(data, 0, maxDepth, 
                                   numPivotsPerLeaf, distance, usedPivots, stats);
        
        // 统计树中总数据点数
        int totalDataPoints = countTotalDataInTree(root);
        
        System.out.println(String.format("\n[VPT] === 构建完成 ==="));
        System.out.println(String.format("[VPT] 实际构建深度: %d", stats.maxDepth));
        System.out.println(String.format("[VPT] 总节点数: %d", stats.totalNodes));
        System.out.println(String.format("[VPT] 使用支撑点数: %d/%d", stats.pivotsUsed, neededPivots));
        System.out.println(String.format("[VPT] 树中总数据点数: %d (支撑点+数据点)", totalDataPoints));
        System.out.println(String.format("[VPT] 输入数据点数: %d", data.size()));
        
        return new BuildResult(root, stats.maxDepth, stats.totalNodes, 
                             stats.pivotsUsed, totalDataPoints);
    }
    
    /**
     * 计算最佳深度（根据支撑点数量自动计算）
     */
    public static int calculateBestDepth(int availablePivots, int numPivotsPerLeaf) {
        if (availablePivots <= 0) {
            return 0;
        }
        
        int bestDepth = 0;
        
        // 尝试不同深度，找到能充分利用支撑点的最佳深度
        for (int depth = 0; depth <= 10; depth++) {  // 限制最大深度为10
            int neededPivots = calculatePivotsNeededForDepth(depth, numPivotsPerLeaf);
            
            // 如果需要的支撑点数不超过可用数，则这个深度可行
            if (neededPivots <= availablePivots) {
                bestDepth = depth;
            } else {
                break;  // 一旦超过可用支撑点数，停止搜索
            }
        }
        
        return bestDepth;
    }
    
    /**
     * 计算特定深度需要的支撑点数
     */
    public static int calculatePivotsNeededForDepth(int depth, int numPivotsPerLeaf) {
        if (depth == 0) {
            // 深度0：只有一个叶子节点
            return Math.max(1, numPivotsPerLeaf);
        }
        
        // 内部节点数：2^depth - 1
        int internalPivots = (int) Math.pow(2, depth) - 1;
        
        // 叶子节点数：2^depth
        int leafNodes = (int) Math.pow(2, depth);
        
        // 叶子节点支撑点数（每个叶子至少1个支撑点）
        int leafPivots = leafNodes * Math.max(1, numPivotsPerLeaf);
        
        return internalPivots + leafPivots;
    }
    
    /**
     * 向后兼容：计算建议深度
     */
    public static int calculateSuggestedDepth(int availablePivots, int numPivotsPerLeaf) {
        return calculateBestDepth(availablePivots, numPivotsPerLeaf);
    }
    
    /**
     * 向后兼容：计算理想支撑点数
     */
    public static int calculateIdealPivotsNeeded(int depth, int numPivotsPerLeaf) {
        return calculatePivotsNeededForDepth(depth, numPivotsPerLeaf);
    }
    
    /**
     * 递归构建VPT
     */
    private static Object buildRecursive(List<MetricData> data,
                                       int currentDepth,
                                       int maxDepth,
                                       int numPivotsPerLeaf,
                                       MetricDistance distance,
                                       List<MetricData> availablePivots,
                                       BuildStats stats) {
        
        stats.totalNodes++;
        stats.maxDepth = Math.max(stats.maxDepth, currentDepth);
        
        // 如果没有数据了，返回空叶子节点
        if (data.isEmpty()) {
            System.out.println(String.format("[VPT] 深度%d: 数据为空，创建空叶子节点", currentDepth));
            return createEmptyLeaf();
        }
        
        // 如果是叶子节点层或数据太少
        if (currentDepth == maxDepth || data.size() <= numPivotsPerLeaf) {
            System.out.println(String.format("[VPT] 深度%d: 创建叶子节点，数据点数量=%d", 
                                           currentDepth, data.size()));
            return createLeafNode(data, numPivotsPerLeaf, distance, availablePivots, stats);
        }
        
        // 如果没有可用的支撑点，创建叶子节点
        if (availablePivots.isEmpty()) {
            System.out.println(String.format("[VPT] 深度%d: 没有可用支撑点，创建叶子节点", currentDepth));
            return createLeafNode(data, numPivotsPerLeaf, distance, availablePivots, stats);
        }
        
        // 内部节点：使用第一个可用的支撑点
        MetricData pivot = availablePivots.remove(0);
        stats.pivotsUsed++;
        
        // 输出内部节点使用的支撑点
        System.out.println(String.format("[VPT内部节点] 深度%d: 使用支撑点=%s, 剩余可用支撑点=%d", 
                                       currentDepth, pivot, availablePivots.size()));
        
        // 计算所有点到支撑点的距离
        List<DistancePointPair> pairs = new ArrayList<>();
        for (MetricData point : data) {
            double d = distance.getDistance(pivot, point);
            pairs.add(new DistancePointPair(d, point));
        }
        
        // 按照距离排序
        Collections.sort(pairs, Comparator.comparingDouble(p -> p.distance));
        
        // 使用中位数距离作为划分半径
        int medianIndex = pairs.size() / 2;
        double splitRadius = pairs.get(medianIndex).distance;
        
        // 按照划分半径划分为左右子集
        List<MetricData> leftData = new ArrayList<>();
        List<MetricData> rightData = new ArrayList<>();
        
        for (int i = 0; i < pairs.size(); i++) {
            MetricData point = pairs.get(i).point;
            if (i < medianIndex) {
                leftData.add(point);
            } else {
                rightData.add(point);
            }
        }
        
        System.out.println(String.format("[VPT内部节点] 深度%d划分结果: 左子树数据点=%d, 右子树数据点=%d, 划分半径=%.4f",
                                       currentDepth, leftData.size(), rightData.size(), splitRadius));
        
        // 分配剩余支撑点给左右子树（简单分配：各一半）
        int remainingPivots = availablePivots.size();
        int leftPivotCount = remainingPivots / 2;
        int rightPivotCount = remainingPivots - leftPivotCount;
        
        List<MetricData> leftPivots = new ArrayList<>();
        List<MetricData> rightPivots = new ArrayList<>();
        
        for (int i = 0; i < leftPivotCount && i < availablePivots.size(); i++) {
            leftPivots.add(availablePivots.get(i));
        }
        for (int i = leftPivotCount; i < leftPivotCount + rightPivotCount && i < availablePivots.size(); i++) {
            rightPivots.add(availablePivots.get(i));
        }
        
        // 清空原始列表，避免重复使用
        availablePivots.clear();
        
        System.out.println(String.format("[VPT内部节点] 深度%d支撑点分配: 左子树分配=%d个, 右子树分配=%d个",
                                       currentDepth, leftPivots.size(), rightPivots.size()));
        
        // 递归构建左右子树
        Object left = buildRecursive(leftData, currentDepth + 1, maxDepth,
                                   numPivotsPerLeaf, distance, leftPivots, stats);
        Object right = buildRecursive(rightData, currentDepth + 1, maxDepth,
                                    numPivotsPerLeaf, distance, rightPivots, stats);
        
        return new VPTInternalNode(pivot, splitRadius, left, right);
    }
    
    /**
     * 创建叶子节点
     */
    private static Object createLeafNode(List<MetricData> data,
                                       int numPivotsPerLeaf,
                                       MetricDistance distance,
                                       List<MetricData> availablePivots,
                                       BuildStats stats) {
        
        if (data.isEmpty()) {
            System.out.println("[VPT叶子节点] 数据为空，创建空叶子节点");
            return createEmptyLeaf();
        }
        
        // 确定叶子节点使用的支撑点数
        int pivotsToUse = Math.min(Math.max(1, numPivotsPerLeaf), data.size());
        List<MetricData> leafPivots = new ArrayList<>();
        
        // 先尝试从可用支撑点中获取
        int fromAvailable = Math.min(pivotsToUse, availablePivots.size());
        for (int i = 0; i < fromAvailable; i++) {
            MetricData pivot = availablePivots.remove(0);
            leafPivots.add(pivot);
            stats.pivotsUsed++;
            
            // 输出叶子节点使用的支撑点（从可用支撑点中获取的）
            System.out.println(String.format("[VPT叶子节点] 从可用支撑点中获取: %s", pivot));
        }
        
        // 如果还不够，理论上不应该发生，因为数据中不应该有支撑点
        // 但如果发生，说明之前的支撑点移除不彻底
        if (leafPivots.size() < pivotsToUse) {
            int needed = pivotsToUse - leafPivots.size();
            System.out.println(String.format("[VPT叶子节点] 可用支撑点不足，需要从数据中补充 %d 个", needed));
            
            // 从数据中选择前几个作为支撑点
            // 注意：这些点应该是数据点，不是支撑点
            for (int i = 0; i < needed && i < data.size(); i++) {
                MetricData dataAsPivot = data.get(i);
                leafPivots.add(dataAsPivot);
                System.out.println(String.format("[VPT叶子节点] 从数据中补充支撑点: %s", dataAsPivot));
            }
        }
        
        // 输出叶子节点总结信息
        System.out.println(String.format("[VPT叶子节点] 总结: 数据点数量=%d, 使用支撑点数量=%d, 其中从可用支撑点获取=%d, 从数据补充=%d",
                                       data.size(), leafPivots.size(), fromAvailable, leafPivots.size() - fromAvailable));
        
        // 使用固定支撑点选择器
        FixedPivotSelection fixedSelector = new FixedPivotSelection(leafPivots);
        
        return new PivotTable(data, Integer.MAX_VALUE, leafPivots.size(), 
                            fixedSelector, distance);
    }
    
    /**
     * 创建空叶子节点
     */
    private static Object createEmptyLeaf() {
        return new PivotTable(new ArrayList<>(), Integer.MAX_VALUE, 0, 
                            new EmptyPivotSelection(), null);
    }
    
    /**
     * 获取实际使用的支撑点（只取前neededCount个）
     */
    private static List<MetricData> getUsedPivots(List<MetricData> pivotSequence, 
                                                int neededCount) {
        int actualCount = Math.min(neededCount, pivotSequence.size());
        List<MetricData> usedPivots = new ArrayList<>(pivotSequence.subList(0, actualCount));
        
        // 输出前几个将被使用的支撑点
        System.out.println(String.format("[VPT] 获取前%d个支撑点作为VPT使用:", Math.min(5, usedPivots.size())));
        for (int i = 0; i < Math.min(5, usedPivots.size()); i++) {
            System.out.println(String.format("  支撑点%d: %s", i+1, usedPivots.get(i)));
        }
        if (usedPivots.size() > 5) {
            System.out.println(String.format("  ... (共%d个支撑点)", usedPivots.size()));
        }
        
        return usedPivots;
    }
    
    /**
     * 统计树中总数据点数
     */
    public static int countTotalDataInTree(Object node) {
        if (node instanceof PivotTable) {
            PivotTable pt = (PivotTable) node;
            return pt.getPivots().size() + pt.getDataPoints().size();
        } 
        else if (node instanceof VPTInternalNode) {
            VPTInternalNode internal = (VPTInternalNode) node;
            int count = 1; // 当前支撑点
            if (internal.left != null) count += countTotalDataInTree(internal.left);
            if (internal.right != null) count += countTotalDataInTree(internal.right);
            return count;
        }
        
        return 0;
    }
    
    /**
     * 固定支撑点选择器
     */
    private static class FixedPivotSelection implements PivotSelectionMethod {
        private final List<MetricData> fixedPivots;
        
        public FixedPivotSelection(List<MetricData> fixedPivots) {
            this.fixedPivots = new ArrayList<>(fixedPivots);
        }
        
        @Override
        public List<MetricData> selectPivots(List<MetricData> dataSet,
                                           int numPivots,
                                           MetricDistance distance) {
            if (numPivots >= fixedPivots.size()) {
                return new ArrayList<>(fixedPivots);
            } else {
                return new ArrayList<>(fixedPivots.subList(0, numPivots));
            }
        }
    }
    
    /**
     * 空支撑点选择器
     */
    private static class EmptyPivotSelection implements PivotSelectionMethod {
        @Override
        public List<MetricData> selectPivots(List<MetricData> dataSet,
                                           int numPivots,
                                           MetricDistance distance) {
            return new ArrayList<>();
        }
    }
    
    /**
     * 构建统计信息
     */
    private static class BuildStats {
        int maxDepth = 0;
        int totalNodes = 0;
        int pivotsUsed = 0;
    }
    
    /**
     * 简易构建方法（兼容原有接口）
     */
    public static Object buildSimple(List<MetricData> data,
                                    int numPivotsPerLeaf,
                                    MetricDistance distance,
                                    List<MetricData> pivotSequence) {
        BuildResult result = buildWithPivotLimit(data, numPivotsPerLeaf, 
                                               distance, pivotSequence);
        return result.root;
    }
    
    /**
     * 从数据集中移除支撑点（供外部调用）
     * @param data 原始数据集
     * @param pivotsToRemove 要移除的支撑点
     * @return 移除支撑点后的数据集
     */
    public static List<MetricData> removePivotsFromData(List<MetricData> data, 
                                                       List<MetricData> pivotsToRemove) {
        List<MetricData> result = new ArrayList<>(data);
        int before = result.size();
        result.removeAll(pivotsToRemove);
        int after = result.size();
        System.out.println(String.format("[VPT] 从数据集中移除支撑点: %d -> %d (移除%d个)", 
                                       before, after, before - after));
        
        // 输出被移除的支撑点（前几个）
        System.out.println(String.format("[VPT] 被移除的支撑点（前%d个）:", Math.min(5, pivotsToRemove.size())));
        for (int i = 0; i < Math.min(5, pivotsToRemove.size()); i++) {
            System.out.println(String.format("  移除支撑点%d: %s", i+1, pivotsToRemove.get(i)));
        }
        if (pivotsToRemove.size() > 5) {
            System.out.println(String.format("  ... (共移除%d个支撑点)", pivotsToRemove.size()));
        }
        
        return result;
    }
    
    /**
     * 计算并获取VPT会使用的支撑点
     * @param ghtPivots GHT的所有支撑点
     * @param numPivotsPerLeaf VPT叶子节点支撑点数
     * @return 实际会使用的支撑点列表
     */
    public static List<MetricData> calculateAndGetUsedPivots(List<MetricData> ghtPivots,
                                                           int numPivotsPerLeaf) {
        if (ghtPivots == null || ghtPivots.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 计算最佳深度
        int bestDepth = calculateBestDepth(ghtPivots.size(), numPivotsPerLeaf);
        
        // 计算需要的支撑点数
        int neededPivots = calculatePivotsNeededForDepth(bestDepth, numPivotsPerLeaf);
        
        // 只取前neededPivots个
        int actualCount = Math.min(neededPivots, ghtPivots.size());
        
        System.out.println(String.format("[VPT] 计算支撑点使用: GHT支撑点=%d, 最佳深度=%d, 需要支撑点=%d, 实际使用=%d",
                                       ghtPivots.size(), bestDepth, neededPivots, actualCount));
        
        return new ArrayList<>(ghtPivots.subList(0, actualCount));
    }
    
    /**
     * 获取树中使用的所有支撑点（用于调试）
     */
    public static List<MetricData> getAllPivotsFromTree(Object node) {
        List<MetricData> pivots = new ArrayList<>();
        collectPivotsFromTree(node, pivots);
        return pivots;
    }
    
    /**
     * 递归收集树中所有支撑点
     */
    private static void collectPivotsFromTree(Object node, List<MetricData> collector) {
        if (node instanceof PivotTable) {
            PivotTable pt = (PivotTable) node;
            collector.addAll(pt.getPivots());
        } 
        else if (node instanceof VPTInternalNode) {
            VPTInternalNode internal = (VPTInternalNode) node;
            collector.add(internal.pivot);
            if (internal.left != null) collectPivotsFromTree(internal.left, collector);
            if (internal.right != null) collectPivotsFromTree(internal.right, collector);
        }
    }
}