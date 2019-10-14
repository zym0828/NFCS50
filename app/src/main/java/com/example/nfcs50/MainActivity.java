package com.example.nfcs50;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends BaseNfcActivity {

    private EditText etKey;
    private EditText etData;
    private EditText etNewKey;
    private Button btRandom;
    private Button btIsRead;

    private Spinner spSector;
    private Spinner spBlock;
    private Spinner spKey;
    private TextView etResult;

    private CheckBox cbIsCrack;

    private LinearLayout llData;
    private LinearLayout llKey;

    private Boolean isRead;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    @Override
    public void onNewIntent(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag == null) {
            return;
        }

        Integer sector = spSector.getSelectedItemPosition();
        Integer block = spBlock.getSelectedItemPosition();
        String dataStr = etData.getText().toString();

        boolean isKeyA = false;
        if (spKey.getSelectedItemPosition() == 0) {
            isKeyA = true;
        }

        String keyStr = etKey.getText().toString();
        if (keyStr.length() != 12) {
            Toast.makeText(this, "密钥为16个16进制数！", Toast.LENGTH_SHORT).show();
            return;
        }

        byte[] key = new byte[6];
        for (int i = 0; i < 6; i++) {
            String s = keyStr.substring(i * 2, i * 2 + 2);
            key[i] = Integer.valueOf(s, 16).byteValue();
        }

        if (cbIsCrack.isChecked()) {
            String crackPasswordStr = crackPassword(intent, sector, block, isKeyA);
            if (crackPasswordStr == null) {
                return;
            }
            etResult.setText(crackPasswordStr);
            return;
        }

        byte[] readData = read(intent, sector, block, isKeyA, key);
        if (readData == null) {
            return;
        }

        String readStr = FormatUtil.bytesToHexString(readData);
        String resultStr = "id：" + byteToString(tag.getId()) + "\n扇区" + sector + "  块" + block + "\n原始数据：" + readStr;

        if (!isRead) {
            write(intent, sector, block, isKeyA, key, dataStr);
            resultStr = resultStr + "\n写入数据：" + dataStr;
        }

        etResult.setText(resultStr);
    }

    private void init() {

        etKey = findViewById(R.id.etKey);
        etData = findViewById(R.id.etData);
        etNewKey = findViewById(R.id.etNewKey);
        btRandom = findViewById(R.id.btRandom);
        cbIsCrack = findViewById(R.id.cbIsCrack);

        llData = findViewById(R.id.llData);
        llKey = findViewById(R.id.llKey);

        isRead = true;
        btIsRead = findViewById(R.id.btIsRead);

        //初始化区选择框
        spSector = findViewById(R.id.spSector);
        String[] spSectorStr = new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15"};
        spSector.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spSectorStr));
        spSector.setSelection(8);

        //初始化块选择框
        spBlock = findViewById(R.id.spBlock);
        String[] spBlockStr = new String[]{"0", "1", "2"};
        spBlock.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spBlockStr));

        //初始化密钥选择框
        spKey = findViewById(R.id.spKey);
        String[] spKeyStr = new String[]{"A", "B"};
        spKey.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spKeyStr));

        etResult = findViewById(R.id.etResult);
        etResult.setMovementMethod(ScrollingMovementMethod.getInstance());

        btRandom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int i = (int) (10000000 + Math.random() * (99999999 - 10000000 + 1));
                etData.setText(Integer.toString(i));
            }
        });

        btIsRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRead) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("警告");
                    builder.setMessage("写入模式会同时修改数据和密码！\n是否进入？");
                    builder.setPositiveButton("是", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            isRead = false;
                            btIsRead.setText("写");
                            llData.setVisibility(View.VISIBLE);
                            llKey.setVisibility(View.VISIBLE);

                        }
                    });
                    builder.setNegativeButton("否", null);
                    builder.show();

                } else {
                    isRead = true;
                    btIsRead.setText("读");
                    llData.setVisibility(View.GONE);
                    llKey.setVisibility(View.GONE);

                }
            }
        });
    }

    private byte[] read(Intent intent, Integer sector, Integer block, Boolean isKeyA, byte[] key) {

        byte[] data = null;

        //intent就是onNewIntent方法返回的那个intent
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        MifareClassic mfc = MifareClassic.get(tag);

        NfcA nfcA = NfcA.get(tag);
        MifareUltralight mu = MifareUltralight.get(tag);

        //如果当前IC卡不是这个格式的mfc就会为空
        if (null != nfcA) {
            try {
                nfcA.connect();

                //修改为密码不可见1
                byte[] write1 = {
                        (byte) 0xA2,
                        (byte) 0x11,
                        (byte) 0x80,
                        (byte) 0x05,
                        (byte) 0x00,
                        (byte) 0x00
                };

                //修改为密码不可见2
                byte[] write2 = {
                        (byte) 0xA2,
                        (byte) 0x10,
                        (byte) 0x00,
                        (byte) 0x00,
                        (byte) 0x00,
                        (byte) 0x03
                };

                //修改第12页的密码
                byte[] write3 = {
                        (byte) 0xA2,
                        (byte) 0x12,
                        (byte) 0x12,
                        (byte) 0x34,
                        (byte) 0x56,
                        (byte) 0x78
                };

                //验证的密码
                byte[] PwdAuth = {
                        (byte) 0x1B,
                        (byte) 0x12,
                        (byte) 0x34,
                        (byte) 0x56,
                        (byte) 0x78
                };

                //读第8页的数据
                byte[] readPage = {
                        (byte) 0x30,
                        (byte) 0x08
                };

                //写第8页的数据
                byte[] write4 = {
                        (byte) 0xA2,
                        (byte) 0x08,
                        (byte) 0x11,
                        (byte) 0x11,
                        (byte) 0x11,
                        (byte) 0x11
                };

                byte[] response1 = nfcA.transceive(hexStringToBytes("1B12345678"));
                byte[] response2 = nfcA.transceive(readPage);
//                byte[] response3 = nfcA.transceive(write1);
//                byte[] response4 = nfcA.transceive(write2);
//                byte[] response5 = nfcA.transceive(write3);
//                byte[] response6 = nfcA.transceive(write4);

                Toast.makeText(this, "返回：" + bytesToHexString(response2), Toast.LENGTH_SHORT).show();
                if (1 == 1) {
                    return response2;
                }

                //链接NFC
                mfc.connect();
                //获取扇区数量
                int count = mfc.getSectorCount();
                //如果传进来的扇区大了或者小了直接退出方法
                if (sector > count - 1 || sector < 0) {
                    Toast.makeText(this, "扇区数值错误！", Toast.LENGTH_SHORT).show();
                    return null;
                }
                //获取写的扇区的块的数量
                int bCount = mfc.getBlockCountInSector(sector);
                //如果输入的块大了或者小了也是直接退出
                if (block > bCount - 1 || block < 0) {
                    Toast.makeText(this, "块数值错误！", Toast.LENGTH_SHORT).show();
                    return null;
                }
                //验证扇区密码，否则会报错（链接失败错误）
                //这里验证的是密码A，如果想验证密码B也行，将方法中的A换成B就行
                boolean isOpen;
                if (isKeyA) {
                    isOpen = mfc.authenticateSectorWithKeyA(sector, key);
                } else {
                    isOpen = mfc.authenticateSectorWithKeyB(sector, key);
                }
                if (isOpen) {
                    //获取扇区第一个块对应芯片存储器的位置
                    int bIndex = mfc.sectorToBlock(sector);
                    data = mfc.readBlock(bIndex + block);
                } else {
                    Toast.makeText(this, "密码错误！", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "读取错误！", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            } finally {
                try {
                    nfcA.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return data;
    }

    public byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    private byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    private void write(Intent intent, Integer sector, Integer block, Boolean isKeyA, byte[] key, String dataStr) {
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
                if (sector > count - 1 || sector < 0) {
                    Toast.makeText(this, "失败！", Toast.LENGTH_SHORT).show();
                    return;
                }
                //获取写的扇区的块的数量
                int bCount = mfc.getBlockCountInSector(sector);
                //如果输入的块大了或者小了也是直接退出
                if (block > bCount - 1 || block < 0) {
                    Toast.makeText(this, "失败！", Toast.LENGTH_SHORT).show();
                    return;
                }

                data = getDataByte(dataStr);

                boolean isOpen;
                if (isKeyA) {
                    isOpen = mfc.authenticateSectorWithKeyA(sector, key);
                } else {
                    isOpen = mfc.authenticateSectorWithKeyB(sector, key);
                }

                if (isOpen) {
                    int bIndex = mfc.sectorToBlock(sector);

                    //修改密码区
                    String newKeyStr = etNewKey.getText().toString();
                    if (newKeyStr.length() != 12) {
                        Toast.makeText(this, "新密码错误！", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    byte[] newKey = new byte[6];
                    for (int i = 0; i < 6; i++) {
                        String s = newKeyStr.substring(i * 2, i * 2 + 2);
                        newKey[i] = Integer.valueOf(s, 16).byteValue();
                    }
                    byte[] block3 = mfc.readBlock(bIndex + 3);
                    if (isKeyA) {
                        for (int i = 0; i < 6; i++) {
                            block3[i] = newKey[i];
                        }
                    } else {
                        for (int i = block3.length - 6; i < block3.length; i++) {
                            block3[i] = newKey[i - block3.length + 6];
                        }
                    }
                    mfc.writeBlock(bIndex + 3, block3);

                    mfc.writeBlock(bIndex + block, data);
                    Toast.makeText(this, "密码正确，写入成功！", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "写入密码错误！", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "写入失败！", Toast.LENGTH_SHORT).show();

            } finally {
                try {
                    mfc.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String crackPassword(Intent intent, Integer sector, Integer block, Boolean isKeyA) {

        byte[] data = null;

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

                //如果传进来的扇区大了或者小了直接退出方法
                if (sector > count - 1 || sector < 0) {
                    Toast.makeText(this, "扇区数值错误！", Toast.LENGTH_SHORT).show();
                    return null;
                }
                //获取写的扇区的块的数量
                int bCount = mfc.getBlockCountInSector(sector);
                //如果输入的块大了或者小了也是直接退出
                if (block > bCount - 1 || block < 0) {
                    Toast.makeText(this, "块数值错误！", Toast.LENGTH_SHORT).show();
                    return null;
                }

                //验证扇区密码，否则会报错（链接失败错误）
                //这里验证的是密码A，如果想验证密码B也行，将方法中的A换成B就行
                boolean isOpen;
                if (isKeyA) {
                    for (long i = 0; i < 281474976710655L; i++) {
                        String keyStr = Long.toHexString(i);

                        if (keyStr.length() < 12) {
                            StringBuilder sb = new StringBuilder();
                            int num = 12 - keyStr.length();
                            for (int j = 0; j < num; j++) {
                                sb.append("0");
                            }
                            sb.append(keyStr);
                            keyStr = sb.toString();
                        }

                        byte[] key = new byte[6];
                        for (int j = 0; j < 6; j++) {
                            String s = keyStr.substring(j * 2, j * 2 + 2);
                            key[j] = Integer.valueOf(s, 16).byteValue();
                        }

                        isOpen = mfc.authenticateSectorWithKeyA(sector, key);
                        if (isOpen) {
                            return keyStr;
                        }
                    }

                } else {
                    for (long i = 0; i < 281474976710655L; i++) {
                        String keyStr = Long.toHexString(i);

                        if (keyStr.length() < 12) {
                            StringBuilder sb = new StringBuilder();
                            int num = 12 - keyStr.length();
                            for (int j = 0; j < num; j++) {
                                sb.append("0");
                            }
                            sb.append(keyStr);
                            keyStr = sb.toString();
                        }

                        byte[] key = new byte[6];
                        for (int j = 0; j < 6; j++) {
                            String s = keyStr.substring(j * 2, j * 2 + 2);
                            key[j] = Integer.valueOf(s, 16).byteValue();
                        }

                        isOpen = mfc.authenticateSectorWithKeyB(sector, key);
                        if (isOpen) {
                            return keyStr;
                        }
                    }
                }
            } catch (Exception e) {
                Toast.makeText(this, "读取错误！", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            } finally {
                try {
                    mfc.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }


//    private void readAll(Intent intent) {
//
//        //拿来装读取出来的数据，key代表扇区数，后面list存放四个块的内容
//        Map<String, List<String>> map = new HashMap<>();
//        //intent就是onNewIntent方法返回的那个intent
//        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
//        MifareClassic mfc = MifareClassic.get(tag);
//        //如果当前IC卡不是这个格式的mfc就会为空
//        if (null != mfc) {
//            try {
//                //链接NFC
//                mfc.connect();
//                //获取扇区数量
//                int count = mfc.getSectorCount();
//
//                //用于判断时候有内容读取出来
//                boolean flag = false;
//                for (int i = 0; i < count; i++) {
//                    //默认密码，如果是自己已知密码可以自己设置
//                    byte[] bytes = {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
//
//                    byte[] bytes2 = {(byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11};
//
//                    //验证扇区密码，否则会报错（链接失败错误）
//                    //这里验证的是密码A，如果想验证密码B也行，将方法中的A换成B就行
//                    boolean isOpen = mfc.authenticateSectorWithKeyA(i, bytes2);
//                    if (isOpen) {
//                        //获取扇区里面块的数量
//                        int bCount = mfc.getBlockCountInSector(i);
//                        //获取扇区第一个块对应芯片存储器的位置
//                        //（我是这样理解的，因为第0扇区的这个值是4而不是0）
//                        int bIndex = mfc.sectorToBlock(i);
//                        for (int j = 0; j < bCount; j++) {
//                            //读取数据，这里是循环读取全部的数据
//                            //如果要读取特定扇区的特定块，将i，j换为固定值就行
//                            byte[] data = mfc.readBlock(bIndex + j);
//                        }
//                        flag = true;
//                    } else {
//                    }
//                }
//                if (flag) {
//                    //回调，因为我把方法抽出来了
////                    callback.callBack(map);
//                    print(map);
//                } else {
////                    callback.error();
//                }
//            } catch (Exception e) {
////                callback.error();
//                e.printStackTrace();
//            } finally {
//                try {
//                    mfc.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
//
//    private void print(Map<String, List<String>> map) {
////        StringBuilder sb = new StringBuilder();
//        for (String key : map.keySet()) {
//            List<String> list = map.get(key);
//            for (String s : list) {
////                sb.append(s);
//            }
//        }
////        tvResult.setText(sb.toString());
////        Toast.makeText(this, sb.toString(), Toast.LENGTH_SHORT).show();
//    }

    /**
     * 将byte数组转化为字符串 * * @param src * @return
     */
    private static String byteToString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder();
        if (src == null || src.length <= 0) {
            return null;
        }
        char[] buffer = new char[2];
        for (int i = 0; i < src.length; i++) {
            buffer[0] = Character.forDigit((src[i] >>> 4) & 0x0F, 16);
            buffer[1] = Character.forDigit(src[i] & 0x0F, 16);
            stringBuilder.append(buffer);
        }
        return stringBuilder.toString();
    }

    //data字符串转换成byte
    private byte[] getDataByte(String dataStr) {

        if (dataStr.length() < 32) {
            StringBuilder sb = new StringBuilder();
            sb.append(dataStr);
            for (int i = 0; i < 32 - dataStr.length(); i++) {
                sb.append("0");
            }
            dataStr = sb.toString();
        }

        byte[] key = new byte[16];
        for (int i = 0; i < 16; i++) {
            String s = dataStr.substring(i * 2, i * 2 + 2);
            key[i] = Integer.valueOf(s, 16).byteValue();
        }

        return key;
    }

    /**
     * 字符串转换成十六进制字符串
     */
    public static String str2HexStr(String str) {
        char[] chars = "0123456789ABCDEF".toCharArray();
        StringBuilder sb = new StringBuilder("");
        byte[] bs = str.getBytes();
        int bit;
        for (int i = 0; i < bs.length; i++) {
            bit = (bs[i] & 0x0f0) >> 4;
            sb.append(chars[bit]);
            bit = bs[i] & 0x0f;
            sb.append(chars[bit]);
        }
        return sb.toString();
    }

    /**
     * 把16进制字符串转换成字节数组
     */
    public static byte[] hexStringToByte(String hex) {
        int len = (hex.length() / 2);
        byte[] result = new byte[len];
        char[] achar = hex.toCharArray();
        for (int i = 0; i < len; i++) {
            int pos = i * 2;
            result[i] = (byte) (toByte(achar[pos]) << 4 | toByte(achar[pos + 1]));
        }
        return result;
    }

    private static int toByte(char c) {
        byte b = (byte) "0123456789ABCDEF".indexOf(c);
        return b;
    }

    /**
     * 数组转换成十六进制字符串
     */
    public static final String bytesToHexString(byte[] bArray) {
        StringBuffer sb = new StringBuffer(bArray.length);
        String sTemp;
        for (int i = 0; i < bArray.length; i++) {
            sTemp = Integer.toHexString(0xFF & bArray[i]);
            if (sTemp.length() < 2)
                sb.append(0);
            sb.append(sTemp.toUpperCase());
        }
        return sb.toString();
    }

}
