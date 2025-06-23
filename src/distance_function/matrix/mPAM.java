package distance_function.matrix;

import java.util.*;

public class mPAM implements SubstitutionMatrix {

    private final Map<Character, Map<Character, Double>> matrix;
    private final char[] aminoAcids = "ARNDCQEGHILKMFPSTWYV".toCharArray(); // 20种
    private final double[][] raw = {
        {0,2,2,2,3,2,2,2,2,2,2,2,2,3,2,2,2,5,4,2,7}, // A
        {2,0,2,2,4,2,2,2,2,3,3,2,2,4,2,2,2,4,4,3,7}, // R
        {2,2,0,2,4,2,2,2,2,3,3,2,2,4,2,2,2,5,4,2,7}, // N
        {2,2,2,0,4,2,2,2,2,3,3,2,3,4,2,2,2,6,4,2,7}, // D
        {3,4,4,4,0,4,4,3,4,3,4,4,4,4,3,3,3,7,3,3,7}, // C
        {2,2,2,2,4,0,2,2,2,3,3,2,2,4,2,2,2,5,4,3,7}, // Q
        {2,2,2,2,4,2,0,2,2,3,3,2,3,4,2,2,2,6,4,2,7}, // E
        {2,2,2,2,3,2,2,0,2,2,3,2,2,4,2,2,2,6,4,2,7}, // G
        {2,2,2,2,4,2,2,2,0,3,3,2,3,3,2,2,2,5,3,3,7}, // H
        {2,3,3,3,3,3,3,2,3,0,1,3,2,2,2,2,2,5,3,2,7}, // I
        {2,3,3,3,4,3,3,3,3,1,0,3,1,2,3,3,2,4,2,1,7}, // L
        {2,2,2,2,4,2,2,2,2,3,3,0,2,4,2,2,2,4,4,3,7}, // K
        {2,2,2,3,4,2,3,2,3,2,1,2,0,2,2,2,2,4,3,2,7}, // M
        {3,4,4,4,4,4,4,4,3,2,2,4,2,0,4,3,3,3,1,2,7}, // F
        {2,2,2,2,3,2,2,2,2,2,3,2,2,4,0,2,2,5,4,2,7}, // P
        {2,2,2,2,3,2,2,2,2,2,3,2,2,3,2,0,2,5,4,2,7}, // S
        {2,2,2,2,3,2,2,2,2,2,2,2,2,3,2,2,0,5,3,2,7}, // T
        {5,4,5,6,7,5,6,6,5,5,4,4,4,3,5,5,5,0,4,5,7}, // W
        {4,4,4,4,3,4,4,4,3,3,2,4,3,1,4,4,3,4,0,3,7}, // Y
        {2,3,2,2,3,3,2,2,3,2,1,3,2,2,2,2,2,5,3,0,7}, // V
        {7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,0}  // OTHER
    };

    private final Map<Character, Integer> indexMap = new HashMap<>();

    public mPAM() {
        matrix = new HashMap<>();
        for (int i = 0; i < aminoAcids.length; i++) {
            char rowChar = aminoAcids[i];
            indexMap.put(rowChar, i);
            Map<Character, Double> inner = new HashMap<>();
            for (int j = 0; j < aminoAcids.length; j++) {
                inner.put(aminoAcids[j], raw[i][j]);
            }
            inner.put('-', raw[i][20]); // gap penalty
            matrix.put(rowChar, inner);
        }

        // 添加 OTHER 行
        Map<Character, Double> otherRow = new HashMap<>();
        for (int j = 0; j < aminoAcids.length; j++) {
            otherRow.put(aminoAcids[j], raw[20][j]);
        }
        otherRow.put('-', raw[20][20]);
        matrix.put('*', otherRow); // OTHER -> *
    }

    private char resolve(char c) {
        return indexMap.containsKey(c) ? c : '*';
    }

    @Override
    public double getScore(char a, char b) {
        char ca = resolve(Character.toUpperCase(a));
        char cb = resolve(Character.toUpperCase(b));
        return matrix.get(ca).getOrDefault(cb, 7.0); 
    }
}
