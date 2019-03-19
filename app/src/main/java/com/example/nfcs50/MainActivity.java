package com.example.nfcs50;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends BaseNfcActivity {

    private TextView tvResult;

    private List<String> list;

    private StringBuilder sb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvResult = findViewById(R.id.tvResult);
        tvResult.setMovementMethod(ScrollingMovementMethod.getInstance());

        list = new ArrayList<>();
    }

    @Override
    public void onNewIntent(Intent intent) {
        write(intent, 7, 1);
    }


    private void read(Intent intent) {
        sb = new StringBuilder();

        //拿来装读取出来的数据，key代表扇区数，后面list存放四个块的内容
        Map<String, List<String>> map = new HashMap<>();
        //intent就是onNewIntent方法返回的那个intent
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        MifareClassic mfc = MifareClassic.get(tag);
        //如果当前IC卡不是这个格式的mfc就会为空
        if (null != mfc) {
            try {
                //链接NFC
                mfc.connect();
                //获取扇区数量
                int count = mfc.getSectorCount();

                sb.append(count);

                //用于判断时候有内容读取出来
                boolean flag = false;
                for (int i = 0; i < count; i++) {
                    //默认密码，如果是自己已知密码可以自己设置
                    byte[] bytes = {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};

                    byte[] bytes2 = {(byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11};

                    //验证扇区密码，否则会报错（链接失败错误）
                    //这里验证的是密码A，如果想验证密码B也行，将方法中的A换成B就行
                    boolean isOpen = mfc.authenticateSectorWithKeyA(i, bytes2);
                    if (isOpen) {
                        //获取扇区里面块的数量
                        int bCount = mfc.getBlockCountInSector(i);
                        //获取扇区第一个块对应芯片存储器的位置
                        //（我是这样理解的，因为第0扇区的这个值是4而不是0）
                        int bIndex = mfc.sectorToBlock(i);
                        for (int j = 0; j < bCount; j++) {
                            //读取数据，这里是循环读取全部的数据
                            //如果要读取特定扇区的特定块，将i，j换为固定值就行
                            byte[] data = mfc.readBlock(bIndex + j);
                            list.add(byteToString(data));
                        }
                        flag = true;
                    } else {
                        sb.append("读取失败");
                    }
                    map.put(i + "", list);
                }
                if (flag) {
                    //回调，因为我把方法抽出来了
//                    callback.callBack(map);
                    print(map);
                } else {
//                    callback.error();
                }
            } catch (Exception e) {
//                callback.error();
                e.printStackTrace();
            } finally {
                try {
                    mfc.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void write(Intent intent, int a, int b) {
        String str = "a";

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        MifareClassic mfc = MifareClassic.get(tag);
        byte[] data = new byte[16];
        if (null != mfc) {
            try {
                //连接NFC
                mfc.connect();
                //获取扇区数量
                int count = mfc.getSectorCount();
                //如果传进来的扇区大了或者小了直接退出方法
                if (a > count - 1 || a < 0) {
//                    callback.isSusses(false);
                    Toast.makeText(this, "失败！", Toast.LENGTH_SHORT).show();
                    return;
                }
                //获取写的扇区的块的数量
                int bCount = mfc.getBlockCountInSector(a);
                //如果输入的块大了或者小了也是直接退出
                if (b > bCount - 1 || b < 0) {
//                    callback.isSusses(false);
                    Toast.makeText(this, "失败！", Toast.LENGTH_SHORT).show();
                    return;
                }
                //将字符转换为字节数组，这样其实并不好，无法转换汉字，因为汉字转出来要占三个字节
                for (int i = 0; i < 16; i++) {
                    if (i < str.length()) {
                        data[i] = (byte) str.charAt(i);
                    } else {
                        data[i] = (byte) ' ';
                    }
                }
                //验证扇区密码 bytes也是默认密码

                byte[] bytes = {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
                byte[] bytes2 = {(byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11};

                boolean isOpen = mfc.authenticateSectorWithKeyA(a, bytes2);
                if (isOpen) {
                    int bIndex = mfc.sectorToBlock(a);
                    //写卡

                    byte[] data2 = {(byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};

                    mfc.writeBlock(bIndex + b, data2);
                    Toast.makeText(this, "成功！", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "失败！", Toast.LENGTH_SHORT).show();
                }
//                callback.isSusses(true);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "失败！", Toast.LENGTH_SHORT).show();

//                callback.isSusses(false);
            } finally {
                try {
                    mfc.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void print(Map<String, List<String>> map) {
//        StringBuilder sb = new StringBuilder();
        for (String key : map.keySet()) {
            List<String> list = map.get(key);
            for (String s : list) {
                sb.append(s);
            }
        }

        tvResult.setText(sb.toString());
//        Toast.makeText(this, sb.toString(), Toast.LENGTH_SHORT).show();
    }

    /**
     * 将byte数组转化为字符串 * * @param src * @return
     */
    public static String byteToString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder();
        if (src == null || src.length <= 0) {
            return null;
        }
        char[] buffer = new char[2];
        for (int i = 0; i < src.length; i++) {
            buffer[0] = Character.forDigit((src[i] >>> 4) & 0x0F, 16);
            buffer[1] = Character.forDigit(src[i] & 0x0F, 16);
            System.out.println(buffer);
            stringBuilder.append(buffer);
        }
        return stringBuilder.toString();
    }
}
