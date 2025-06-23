package dataload;

import db.StringData;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class StringReader {

    /**
     * 从指定路径读取指定数量的字符串数据。
     *
     * @param path  文件路径
     * @param count 要读取的字符串数量
     * @return List<StringData>
     * @throws IOException 文件读取失败
     */
    public List<StringData> load(String path, int count) throws IOException {
        List<StringData> dataList = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            int loaded = 0;

            while ((line = br.readLine()) != null && loaded < count) {
                line = line.trim();
                if (!line.isEmpty()) {
                    dataList.add(new StringData(line));
                    loaded++;
                }
            }

            if (loaded < count) {
                throw new IllegalArgumentException("文件中数据不足，期望：" + count + "，实际：" + loaded);
            }
        }

        return dataList;
    }
}
