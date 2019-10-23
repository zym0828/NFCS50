# NFCS50

公司是专门做NFC/RFID的，前不久要用S50卡开发一个应用，一开始对这款型号的卡不是很熟悉，就在网上搜集了资料，再针对这款芯片开发了一个小Demo。话不多说，先看图吧。
![](https://user-gold-cdn.xitu.io/2019/10/23/16df674ad2a0c9ac?w=1080&h=2280&f=jpeg&s=177160)
![](https://user-gold-cdn.xitu.io/2019/10/23/16df674fadebf9d3?w=1080&h=2280&f=jpeg&s=219708)
S50（F08是国产卡，使用起来跟S50基本相同）是MifareClassic系列的卡，所以可以直接调用安卓官方的SDK。


读数据：
```
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
```

写数据：
```
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
```
google developer：[NFC MifareClassic Android 文档](https://developer.android.google.cn/reference/kotlin/android/nfc/tech/MifareClassic)。

github：[NFCS50](https://github.com/zym0828/NFCS50)。

掘金：[Android NFC——分享一个使用安卓手机读写S50/F08芯片的程序](https://juejin.im/post/5dafba476fb9a04ddb3b8bde)。
