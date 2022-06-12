package org.zywx.wbpalmstar.plugin.uexxunfei;

import android.content.Context;
import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * File Description: 文件操作
 * <p>
 * Created by sandy with Email: sandy1108@163.com at Date: 2022/6/12.
 */
public class FileUtils {
    /**
     * 读取asset目录下文件。
     *
     * @return content
     */
    public static String readFile(Context mContext, String filePath) {
        String result = null;
        InputStream in = null;
        try {
            if (!TextUtils.isEmpty(filePath) && filePath.startsWith("widget/")) {
                in = mContext.getAssets().open(filePath);
            } else {
                in = new FileInputStream(filePath);
            }
            result = FileUtils.InputStreamTOString(in, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }
        return result;
    }

    /**
     * 将InputStream转换成某种字符编码的String
     *
     * @param in
     * @param encoding
     * @return InputStream转化后String
     */
    public static String InputStreamTOString(InputStream in, String encoding) {
        final int BUFFER_SIZE = 4096;

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] data = new byte[BUFFER_SIZE];
        int count = -1;
        String string = null;
        try {
            while ((count = in.read(data, 0, BUFFER_SIZE)) != -1) {
                outStream.write(data, 0, count);
            }
            data = null;
            string = new String(outStream.toByteArray(), encoding);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return string;
    }
}
