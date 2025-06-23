package distance_function;

import db.MetricData;
import db.StringData;
import distance_function.matrix.SubstitutionMatrix;

/**
 * 加权编辑距离类，支持蛋白质等使用替换矩阵计算的场景。
 */
public class WeightedEditDistance extends MetricDistance {

    private final SubstitutionMatrix substitutionMatrix;

    public WeightedEditDistance(SubstitutionMatrix matrix) {
        this.substitutionMatrix = matrix;
    }

    @Override
    protected double compute(MetricData a, MetricData b) {
        if (!(a instanceof StringData) || !(b instanceof StringData)) {
            throw new IllegalArgumentException("WeightedEditDistance 仅支持 StringData 类型");
        }

        String s1 = (String) a.getRawData();
        String s2 = (String) b.getRawData();
        int m = s1.length(), n = s2.length();
        double[][] dp = new double[m + 1][n + 1];

        // 初始化边界：s1[0..i] 和 空串的代价
        for (int i = 1; i <= m; i++) {
            dp[i][0] = dp[i - 1][0] + _score(s1.charAt(i - 1), '-');  // 删除字符
        }

        for (int j = 1; j <= n; j++) {
            dp[0][j] = dp[0][j - 1] + _score('-', s2.charAt(j - 1));  // 插入字符
        }

        // 动态规划填表
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                double costSub = dp[i - 1][j - 1] + _score(s1.charAt(i - 1), s2.charAt(j - 1)); // 替换
                double costDel = dp[i - 1][j] + _score(s1.charAt(i - 1), '-'); // 删除
                double costIns = dp[i][j - 1] + _score('-', s2.charAt(j - 1)); // 插入

                dp[i][j] = Math.min(costSub, Math.min(costDel, costIns));
            }
        }

        return dp[m][n];
    }

    private double _score(char a, char b) {
        return substitutionMatrix.getScore(a, b);
    }
}
