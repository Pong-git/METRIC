package algorithms.pivotselection;

import db.MetricData;
import distance_function.MetricDistance;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * RandomSelection 是支撑点选择的一种实现方式。
 * 它从数据集中随机选择 numPivots 个数据点作为支撑点（允许重复）。
 */
public class RandomSelection implements PivotSelectionMethod {

    private final Random random;

    public RandomSelection() {
        this.random = new Random();
    }

    public RandomSelection(long seed) {
        this.random = new Random(seed);
    }

    @Override
    public List<MetricData> selectPivots(List<MetricData> dataSet, int numPivots, MetricDistance distance) {
        int n = dataSet.size();
        if (numPivots > n) {
            throw new IllegalArgumentException("支撑点数量不能超过数据集大小");
        }

        List<MetricData> pivots = new ArrayList<>(numPivots);
        for (int i = 0; i < numPivots; i++) {
            int idx = random.nextInt(n);
            pivots.add(dataSet.get(idx));
        }

        return pivots;
    }
}
