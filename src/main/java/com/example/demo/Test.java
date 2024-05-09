package com.example.demo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Iterator;

import org.apache.commons.io.FilenameUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPBEEncryptedData;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcePBEDataDecryptorFactoryBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.bouncycastle.util.io.Streams;

public class Test {

	public static void main(String[] args) throws FileNotFoundException, IOException, PGPException {
		Test t = new Test();
	//	t.test();
		Security.addProvider(new BouncyCastleProvider());
		String inputFIle = "D:\\Projects\\Ronak\\inputfiles\\new1.txt";
		String encryptedFile = "D:\\Projects\\Ronak\\encryptedfiles\\test_ronak2.pgp";
		String deryptedFile = "D:\\Projects\\Ronak\\decryptedfiles\\output_ronak2.txt";
		String publikey = "D:\\Projects\\Encryption\\ronakpublickey.asc";
		String privatekey = "D:\\Projects\\Encryption\\ronakprivatekey.asc";
	
		encryptFile(inputFIle, encryptedFile, new FileInputStream(publikey));
		
		decryptFile(encryptedFile, deryptedFile, new FileInputStream(privatekey), "f01T39gH");
	}

	private void test() {
		File f = new File("C:\\Users\\acer\\Downloads\\my.txt");
		
		System.out.println("file name - "+f.getName());
		
		System.out.println("File name exten without - "+FilenameUtils.removeExtension(f.getName()));
	}
	
	public static void encryptFile(String inputFilePath, String outputFilePath, InputStream publicKeyInputStream) throws IOException, PGPException {
        try (
            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFilePath));
            InputStream inputStream = new BufferedInputStream(new FileInputStream(inputFilePath))
        ) {
            PGPPublicKey publicKey = readPublicKey(publicKeyInputStream);
            PGPEncryptedDataGenerator encryptedDataGenerator = new PGPEncryptedDataGenerator(
                new JcePGPDataEncryptorBuilder(PGPEncryptedData.CAST5).setWithIntegrityPacket(true).setSecureRandom(new SecureRandom())
            );
            encryptedDataGenerator.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(publicKey));
            try (OutputStream encryptedOutputStream = encryptedDataGenerator.open(outputStream, new byte[4096])) {
                Streams.pipeAll(inputStream, encryptedOutputStream);
            }
        }
    }

	private static PGPPublicKey readPublicKey(InputStream publicKeyInputStream) throws IOException, PGPException {
	    try (
	        InputStream decodedInputStream = PGPUtil.getDecoderStream(publicKeyInputStream);
	    ) {
	        PGPPublicKeyRingCollection publicKeyRingCollection = new PGPPublicKeyRingCollection(decodedInputStream, new JcaKeyFingerprintCalculator());
	        Iterator<PGPPublicKeyRing> publicKeyRingIterator = publicKeyRingCollection.getKeyRings();
	        while (publicKeyRingIterator.hasNext()) {
	            PGPPublicKeyRing publicKeyRing = publicKeyRingIterator.next();
	            Iterator<PGPPublicKey> publicKeyIterator = publicKeyRing.getPublicKeys();
	            while (publicKeyIterator.hasNext()) {
	                PGPPublicKey publicKey = publicKeyIterator.next();
	                if (publicKey.isEncryptionKey()) {
	                    return publicKey;
	                }
	            }
	        }
	        throw new IllegalArgumentException("No encryption key found in the provided public key file.");
	    }
	}
	
	public static void decryptFile(String inputFilePath, String outputFilePath, InputStream privateKeyInputStream, String passphrase) throws IOException, PGPException {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider()); // Register Bouncy Castle provider

        try (
                InputStream inputStream = new BufferedInputStream(new FileInputStream(inputFilePath));
                OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFilePath))
        ) {
            PGPObjectFactory pgpObjectFactory = new PGPObjectFactory(PGPUtil.getDecoderStream(inputStream), new JcaKeyFingerprintCalculator());
            Object object;
            while ((object = pgpObjectFactory.nextObject()) != null) {
                if (object instanceof PGPEncryptedDataList) {
                    PGPEncryptedDataList encryptedDataList = (PGPEncryptedDataList) object;
                    Iterator<PGPEncryptedData> encryptedDataIterator = encryptedDataList.getEncryptedDataObjects();
                    while (encryptedDataIterator.hasNext()) {
                        PGPEncryptedData encryptedData = encryptedDataIterator.next();
                        if (encryptedData instanceof PGPPublicKeyEncryptedData) {
                            PGPPublicKeyEncryptedData publicKeyEncryptedData = (PGPPublicKeyEncryptedData) encryptedData;
                            PGPPrivateKey privateKey = findPrivateKey(privateKeyInputStream, publicKeyEncryptedData.getKeyID(), passphrase);
                            InputStream decryptedStream = publicKeyEncryptedData.getDataStream(new JcePublicKeyDataDecryptorFactoryBuilder().setProvider("BC").build(privateKey));
                            Streams.pipeAll(decryptedStream, outputStream);
                            decryptedStream.close();
                        } else if (encryptedData instanceof PGPPBEEncryptedData) {
                            PGPPBEEncryptedData pbeEncryptedData = (PGPPBEEncryptedData) encryptedData;
                            InputStream decryptedStream = pbeEncryptedData.getDataStream(new JcePBEDataDecryptorFactoryBuilder().setProvider("BC").build(passphrase.toCharArray()));
                            Streams.pipeAll(decryptedStream, outputStream);
                            decryptedStream.close();
                        }
                    }
                }
            }
        }
    }

    private static PGPPrivateKey findPrivateKey(InputStream privateKeyInputStream, long keyID, String passphrase) throws IOException, PGPException {
        PGPSecretKeyRingCollection pgpSecretKeyRingCollection = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(privateKeyInputStream), new JcaKeyFingerprintCalculator());
        PGPSecretKey secretKey = pgpSecretKeyRingCollection.getSecretKey(keyID);
        if (secretKey == null) {
            throw new IllegalArgumentException("Private key not found for key ID: " + Long.toHexString(keyID));
        }
        return secretKey.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder().setProvider("BC").build(passphrase.toCharArray()));
    }

}
