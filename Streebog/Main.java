package Streebog;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

public class Main {

    private static String bytesToHexStr(byte[] bytes) {
        String res = "";
        for (byte aByte : bytes) {
            res += String.format("%02x", aByte);
        }
        return res;
    }

    public static void main(String[] args) {
        if (Security.getProvider("GOST") == null) {
            Security.addProvider(new StreebogProvider());
        }

        try {
            MessageDigest md = MessageDigest.getInstance("GOST3411-2012.512");
            byte[] result = md.digest("Hello, World!".getBytes());
            System.out.println(bytesToHexStr(result));
        } catch (NoSuchAlgorithmException e) {
            System.out.println(e.getMessage());
        }
    }
}
