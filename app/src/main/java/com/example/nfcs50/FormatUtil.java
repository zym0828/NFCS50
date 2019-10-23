package com.example.nfcs50;

public class FormatUtil {

    //byte[]转hexString
    public static final String byteToHexString(byte[] bArray) {
        StringBuilder sb = new StringBuilder(bArray.length);
        String sTemp;
        for (int i = 0; i < bArray.length; i++) {
            sTemp = Integer.toHexString(0xFF & bArray[i]);
            if (sTemp.length() < 2)
                sb.append(0);
            sb.append(sTemp.toUpperCase());
        }
        return sb.toString();
    }

    //hexString转byte[]
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

    //data字符串转换成byte，长度不足32的补零
    public static byte[] getDataByte(String dataStr) {

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
}
