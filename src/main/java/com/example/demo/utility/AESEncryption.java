package com.example.demo.utility;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.Key;

public class AESEncryption {

    private static final String AES_ALGORITHM = "AES";

    public static void encryptFile(String inputFilePath, String outputFilePath, String secretKey) throws Exception {
        Key key = new SecretKeySpec(secretKey.getBytes(), AES_ALGORITHM);
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] inputBytes = Files.readAllBytes(Paths.get(inputFilePath));
        byte[] encryptedBytes = cipher.doFinal(inputBytes);

        Files.write(Paths.get(outputFilePath), encryptedBytes, StandardOpenOption.CREATE);
    }
}