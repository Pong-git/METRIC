package search;

import db.MetricData;
import distance_function.MetricDistance;
import index.PivotTable;
import index.VPTPivotsNum_limit;

import java.util.ArrayList;
import java.util.List;

/**
 * 专门用于 VPTPivotsNum_limit 索引的范围查询
 * 处理 VPTPivotsNum_limit.VPTInternalNode 类型的节点
 */
public class VPTLimitRangeSearch {

    /**
     * 执行 VPTPivotsNum_limit 的范围查询
     *
     * @param node     当前节点（VPTPivotsNum_limit.VPTInternalNode 或 PivotTable）
     * @param query    查询点
     * @param radius   查询半径
     * @param distance 距离函数（包含计数器）
     * @return 查询结果封装 SearchResult
     */
    public static SearchResult rangeSearch(Object node, MetricData query, double radius, MetricDistance distance) {
        // 保存初始距离计算次数（用于统计本次查询新增的计算次数）
        long initialCount = distance.getCount();
        
        List<MetricData> result = doRangeSearch(node, query, radius, distance);
        
        // 计算本次查询新增的距离计算次数
        long newDistanceCount = distance.getCount() - initialCount;
        
        return new SearchResult(result, newDistanceCount);
    }

    /**
     * 实际递归的查询过程
     */
    private static List<MetricData> doRangeSearch(Object node, MetricData query, double radius, MetricDistance distance) {
        List<MetricData> result = new ArrayList<>();

        // 叶子节点直接交给 PTRangeSearch 处理
        if (node instanceof PivotTable) {
            return PTRangeSearch.rangeSearch((PivotTable) node, query, radius, distance).results;
        }

        // VPTPivotsNum_limit 的内部节点处理
        VPTPivotsNum_limit.VPTInternalNode internal = (VPTPivotsNum_limit.VPTInternalNode) node;

        // 计算查询点到支撑点的距离
        double d_vq = distance.getDistance(query, internal.pivot);
        
        // 如果支撑点在查询半径内，加入结果集
        if (d_vq <= radius) {
            result.add(internal.pivot);
        }

        // 左子树（距离 <= splitRadius 的部分）
        if (internal.left != null) {
            // 球内全部包含：查询球完全包含左子树的覆盖球
            if (d_vq + internal.splitRadius <= radius) {
                result.addAll(getAllData(internal.left));
            }
            // 球内不能排除：查询球与左子树的覆盖球相交
            else if (d_vq <= internal.splitRadius + radius) {
                result.addAll(doRangeSearch(internal.left, query, radius, distance));
            }
        }

        // 右子树（距离 > splitRadius 的部分）
        if (internal.right != null) {
            // 球外不能排除：查询球与右子树的覆盖球相交
            if (d_vq + radius > internal.splitRadius) {
                result.addAll(doRangeSearch(internal.right, query, radius, distance));
            }
        }

        return result;
    }

    /**
     * 获取子树所有数据（当查询球完全包含子树时调用）
     * 包括所有支撑点和数据点
     */
    private static List<MetricData> getAllData(Object node) {
        List<MetricData> all = new ArrayList<>();

        if (node instanceof PivotTable) {
            PivotTable pt = (PivotTable) node;
            // 添加支撑点
            all.addAll(pt.getPivots());
            // 添加数据点
            all.addAll(pt.getDataPoints());
        } 
        else if (node instanceof VPTPivotsNum_limit.VPTInternalNode) {
            VPTPivotsNum_limit.VPTInternalNode internal = (VPTPivotsNum_limit.VPTInternalNode) node;
            // 添加当前支撑点
            all.add(internal.pivot);
            // 递归获取左右子树数据
            if (internal.left != null) {
                all.addAll(getAllData(internal.left));
            }
            if (internal.right != null) {
                all.addAll(getAllData(internal.right));
            }
        }
        // 如果还有其他节点类型可以继续添加

        return all;
    }
    
    /**
     * 统计子树中的总数据点数（用于调试）
     */
    public static int countTotalDataPoints(Object node) {
        if (node instanceof PivotTable) {
            PivotTable pt = (PivotTable) node;
            return pt.getPivots().size() + pt.getDataPoints().size();
        } 
        else if (node instanceof VPTPivotsNum_limit.VPTInternalNode) {
            VPTPivotsNum_limit.VPTInternalNode internal = (VPTPivotsNum_limit.VPTInternalNode) node;
            int count = 1; // 当前支撑点
            
            if (internal.left != null) {
                count += countTotalDataPoints(internal.left);
            }
            if (internal.right != null) {
                count += countTotalDataPoints(internal.right);
            }
            
            return count;
        }
        
        return 0;
    }
    
    /**
     * 打印树结构（用于调试）
     */
    public static void printTree(Object node, int depth) {
        String indent = "  ".repeat(depth);
        
        if (node instanceof PivotTable) {
            PivotTable pt = (PivotTable) node;
            System.out.println(indent + "PivotTable[支撑点:" + pt.getPivots().size() + 
                             ", 数据点:" + pt.getDataPoints().size() + "]");
        } 
        else if (node instanceof VPTPivotsNum_limit.VPTInternalNode) {
            VPTPivotsNum_limit.VPTInternalNode internal = (VPTPivotsNum_limit.VPTInternalNode) node;
            System.out.println(indent + "VPTInternalNode[支撑点:" + internal.pivot + 
                             ", 半径:" + String.format("%.2f", internal.splitRadius) + "]");
            
            if (internal.left != null) {
                System.out.print(indent + "左子树: ");
                printTree(internal.left, depth + 1);
            }
            if (internal.right != null) {
                System.out.print(indent + "右子树: ");
                printTree(internal.right, depth + 1);
            }
        }
    }
    
    /**
     * 验证查询结果（用于调试）
     * 比较索引查询结果与线性扫描结果
     */
    public static SearchResult validateWithLinearScan(Object root, List<MetricData> dataset,
                                                     MetricData query, double radius,
                                                     MetricDistance distance) {
        // 使用索引查询
        SearchResult indexResult = rangeSearch(root, query, radius, distance);
        
        // 使用线性扫描
        List<MetricData> linearResult = new ArrayList<>();
        for (MetricData data : dataset) {
            double dist = distance.getDistance(query, data);
            if (dist <= radius) {
                linearResult.add(data);
            }
        }
        
        // 比较结果
        boolean correct = true;
        if (indexResult.results.size() != linearResult.size()) {
            System.out.println("[验证] 结果数量不一致: 索引=" + indexResult.results.size() + 
                             ", 线性=" + linearResult.size());
            correct = false;
        }
        
        // 检查是否所有索引结果都在线性结果中
        for (MetricData data : indexResult.results) {
            if (!linearResult.contains(data)) {
                System.out.println("[验证] 索引返回了不存在的结果: " + data);
                correct = false;
            }
        }
        
        // 检查是否所有线性结果都在索引结果中
        for (MetricData data : linearResult) {
            if (!indexResult.results.contains(data)) {
                System.out.println("[验证] 索引遗漏了结果: " + data);
                correct = false;
            }
        }
        
        if (correct) {
            System.out.println("[验证] ✓ 查询结果正确");
        } else {
            System.out.println("[验证] ✗ 查询结果有误");
        }
        
        return indexResult;
    }
}