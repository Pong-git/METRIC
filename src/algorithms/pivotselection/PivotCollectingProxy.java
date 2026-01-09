package algorithms.pivotselection;

import db.MetricData;
import distance_function.MetricDistance;

import java.util.ArrayList;
import java.util.List;

/**
 * 支撑点收集代理
 * 包装一个真实的支撑点选择器，在调用时收集所有选择的支撑点
 * 用于记录索引构建过程中使用的所有支撑点
 */
public class PivotCollectingProxy implements PivotSelectionMethod {
    
    private final PivotSelectionMethod realSelector;  // 真实的支撑点选择器
    private final List<MetricData> collectedPivots;   // 收集到的支撑点
    private boolean isCollecting;                     // 是否正在收集
    
    /**
     * 构造函数
     * @param realSelector 真实的支撑点选择器
     */
    public PivotCollectingProxy(PivotSelectionMethod realSelector) {
        if (realSelector == null) {
            throw new IllegalArgumentException("真实选择器不能为null");
        }
        this.realSelector = realSelector;
        this.collectedPivots = new ArrayList<>();
        this.isCollecting = false;
    }
    
    /**
     * 开始收集支撑点
     */
    public void startCollecting() {
        collectedPivots.clear();
        isCollecting = true;
        System.out.println("[PivotCollectingProxy] 开始收集支撑点");
    }
    
    /**
     * 停止收集支撑点
     */
    public void stopCollecting() {
        isCollecting = false;
        System.out.println("[PivotCollectingProxy] 停止收集支撑点，共收集 " + 
                          collectedPivots.size() + " 个");
    }
    
    /**
     * 实现PivotSelectionMethod接口
     * 调用真实选择器，并收集返回的支撑点
     */
    @Override
    public List<MetricData> selectPivots(List<MetricData> dataSet,
                                       int numPivots,
                                       MetricDistance distance) {
        // 调用真实选择器
        List<MetricData> pivots = realSelector.selectPivots(dataSet, numPivots, distance);
        
        // 如果正在收集，记录支撑点
        if (isCollecting) {
            synchronized (collectedPivots) {
                // 只添加未收集过的支撑点（避免重复）
                for (MetricData pivot : pivots) {
                    if (!collectedPivots.contains(pivot)) {
                        collectedPivots.add(pivot);
                    }
                }
                // 或者直接添加所有（保持顺序）：
                // collectedPivots.addAll(pivots);
            }
        }
        
        return pivots;
    }
    
    /**
     * 获取收集到的所有支撑点
     * @return 收集到的支撑点列表（副本）
     */
    public List<MetricData> getCollectedPivots() {
        return new ArrayList<>(collectedPivots);
    }
    
    /**
     * 获取收集到的支撑点数量
     */
    public int getCollectedCount() {
        return collectedPivots.size();
    }
    
    /**
     * 清空收集的支撑点
     */
    public void clearCollectedPivots() {
        collectedPivots.clear();
        System.out.println("[PivotCollectingProxy] 已清空收集的支撑点");
    }
    
    /**
     * 检查是否正在收集
     */
    public boolean isCollecting() {
        return isCollecting;
    }
    
    /**
     * 获取真实的选择器
     */
    public PivotSelectionMethod getRealSelector() {
        return realSelector;
    }
    
    /**
     * 打印收集状态
     */
    public void printCollectionStatus() {
        System.out.println("=== PivotCollectingProxy 状态 ===");
        System.out.println("收集状态: " + (isCollecting ? "进行中" : "已停止"));
        System.out.println("收集数量: " + collectedPivots.size());
        System.out.println("真实选择器: " + realSelector.getClass().getSimpleName());
        
        if (!collectedPivots.isEmpty()) {
            System.out.println("最后收集的支撑点: " + 
                collectedPivots.get(collectedPivots.size() - 1));
        }
    }
    
    /**
     * 静态工厂方法：创建收集代理
     */
    public static PivotCollectingProxy createFor(PivotSelectionMethod selector) {
        return new PivotCollectingProxy(selector);
    }
    
    /**
     * 快速使用：创建代理，开始收集，构建索引，获取支撑点
     */
    public static List<MetricData> collectPivotsFromBuild(
            List<MetricData> dataset,
            int maxLeafSize,
            MetricDistance distance,
            PivotSelectionMethod selector,
            int numPivots) {
        
        PivotCollectingProxy proxy = new PivotCollectingProxy(selector);
        proxy.startCollecting();
        
        // 这里需要根据实际构建方法调用
        // 例如：GeneralHyperPlaneTree.GHBulkLoad(dataset, maxLeafSize, distance, proxy, numPivots);
        
        proxy.stopCollecting();
        return proxy.getCollectedPivots();
    }
}