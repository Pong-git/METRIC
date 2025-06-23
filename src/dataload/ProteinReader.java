package dataload;

import db.MetricData;
import db.StringData;

import java.io.*;
import java.util.*;

public class ProteinReader {

    /**
     * 读取蛋白质数据，返回前 count 个蛋白质序列（StringData）。
     * 数据以 '>' 开头作为标识符，下一行及后续行为序列，直到下一个 '>'。
     */
    public List<MetricData> load(String path, int count) throws IOException {
        List<MetricData> sequences = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            StringBuilder currentSeq = new StringBuilder();
            boolean readingSeq = false;

            while ((line = br.readLine()) != null) {
                if (line.startsWith(">")) {
                    if (readingSeq && currentSeq.length() > 0) {
                        sequences.add(new StringData(currentSeq.toString()));
                        if (sequences.size() >= count) break;
                    }
                    currentSeq.setLength(0);  // reset
                    readingSeq = true;
                } else if (readingSeq) {
                    currentSeq.append(line.trim());
                }
            }

            // 添加最后一个序列
            if (readingSeq && currentSeq.length() > 0 && sequences.size() < count) {
                sequences.add(new StringData(currentSeq.toString()));
            }
        }

        if (sequences.size() < count) {
            throw new IllegalArgumentException("请求读取 " + count + " 条序列，但文件中仅有 " + sequences.size() + " 条。");
        }

        return sequences;
    }
}

