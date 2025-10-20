package search;

import db.MetricData;
import distance_function.MetricDistance;
import index.MultipleVantagePointTree.MVPInternalNode;
import index.PivotTable;

import java.util.ArrayList;
import java.util.List;

/**
 * MVPT（多优势点树）的范围查询类。
 */
public class MVPRangeSearch {

    /**
     * 执行 MVPT 的范围查询。
     *
     * @param node     当前节点（MVPInternalNode 或 PivotTable）
     * @param query    查询点
     * @param radius   查询半径
     * @param distance 距离函数（含计数器）
     * @return 查询结果封装类（命中对象 + 距离计算次数）
     */
    public static SearchResult rangeSearch(Object node, MetricData query, double radius, MetricDistance distance) {

        List<MetricData> result = doRangeSearch(node, query, radius, distance);
        return new SearchResult(result, distance.getCount());
    }

    /**
     * 递归搜索过程。
     */
    private static List<MetricData> doRangeSearch(Object node,  MetricData query, double radius,  MetricDistance distance) {
        List<MetricData> result = new ArrayList<>();

        // 如果是叶子节点，调用通用 PTRangeSearch
        if (node instanceof PivotTable) {
            return PTRangeSearch.rangeSearch((PivotTable) node, query, radius, distance).results;
        }

        MVPInternalNode internal = (MVPInternalNode) node;
        int numPivots = internal.pivots.size();
        int numChildren = internal.children.size();

        // Step 1: 计算查询点到每个支撑点的距离
        double[] distToPivots = new double[numPivots];
        for (int i = 0; i < numPivots; i++) {
            MetricData pivot = internal.pivots.get(i);
            double d = distance.getDistance(query, pivot);
            distToPivots[i] = d;
            if (d <= radius) {
                result.add(pivot);
            }
        }

        // Step 2: 对每个子节点判断是否剪枝或包含
        for (int i = 0; i < numChildren; i++) {
            Object child = internal.children.get(i);
            if (child == null) continue;

            boolean skip = false;
            boolean fullyContained = false;

            for (int j = 0; j < numPivots; j++) {
                double d_q_p = distToPivots[j];
                double lb = internal.lowerBounds[j][i];
                double ub = internal.upperBounds[j][i];

                // 完全包含：整个子节点在查询球内
                if (d_q_p + ub <= radius) {
                    result.addAll(getAllData(child));
                    skip = true;
                    fullyContained = true;
                    break;
                }

                // 排除规则：球外不相交
                if (d_q_p + radius <= lb || d_q_p - radius >= ub) {
                    skip = true;
                    break;
                }
            }

            // 既不能排除也不能直接包含
            if (!skip || (!fullyContained && !skip)) {
                result.addAll(doRangeSearch(child, query, radius, distance));
            }
        }

        return result;
    }

    /**
     * 递归获取节点下所有数据（包括支撑点和数据点）。
     */
    private static List<MetricData> getAllData(Object node) {
        List<MetricData> all = new ArrayList<>();

        if (node instanceof PivotTable) {
            PivotTable pt = (PivotTable) node;
            all.addAll(pt.getPivots());
            all.addAll(pt.getDataPoints());
            return all;
        }

        MVPInternalNode internal = (MVPInternalNode) node;
        all.addAll(internal.pivots);
        for (Object child : internal.children) {
            all.addAll(getAllData(child));
        }
        return all;
    }
}
