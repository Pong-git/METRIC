package distance_function.matrix;

/**
 * SubstitutionMatrix 接口定义氨基酸之间的替换代价。
 */
public interface SubstitutionMatrix {

    /**
     * 获取字符 a 替换为字符 b 的代价。
     *
     * @param a 第一个字符
     * @param b 第二个字符
     * @return 替换代价（整数）
     */
    double getScore(char a, char b);
}
