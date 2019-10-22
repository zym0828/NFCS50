package com.example.nfcs50;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
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

        byte[] readData = read(intent, sector, block, isKeyA, key);
        if (readData == null) {
            return;
        }

        String readStr = FormatUtil.bytesToHexString(readData);
        String resultStr = "id：" + FormatUtil.byteToString(tag.getId()) + "\n扇区" + sector + "  块" + block + "\n原始数据：" + readStr;

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
                    mfc.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return data;
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

                data = FormatUtil.getDataByte(dataStr);

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
}
