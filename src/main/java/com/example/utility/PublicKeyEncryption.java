package com.example.utility;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Iterator;

import org.apache.commons.io.FilenameUtils;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPBEEncryptedData;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcePBEDataDecryptorFactoryBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;

public class PublicKeyEncryption {
	public static void encryptFile(String inputFilePath, String outputFilePath, String publicKeyPath, String passphrase)
			throws IOException, PGPException {
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider()); // Register Bouncy Castle
																						// provider
		try (OutputStream outputStream = new FileOutputStream(outputFilePath)) {
			PGPPublicKey publicKey = readPublicKey(publicKeyPath);

			PGPEncryptedDataGenerator encryptedDataGenerator = new PGPEncryptedDataGenerator(
					new JcePGPDataEncryptorBuilder(PGPEncryptedData.CAST5).setWithIntegrityPacket(true)
							.setSecureRandom(new SecureRandom()));
			encryptedDataGenerator.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(publicKey).setProvider("BC"));

			OutputStream encryptedOutputStream = encryptedDataGenerator.open(new ArmoredOutputStream(outputStream),
					new byte[4096]);

			PGPCompressedDataGenerator compressedDataGenerator = new PGPCompressedDataGenerator(PGPCompressedData.ZIP);
			try (OutputStream compressedOutputStream = compressedDataGenerator.open(encryptedOutputStream)) {
				PGPLiteralDataGenerator literalDataGenerator = new PGPLiteralDataGenerator();
				try (OutputStream literalOutputStream = literalDataGenerator.open(compressedOutputStream,
						PGPLiteralData.BINARY, inputFilePath, new File(inputFilePath).length(), new java.util.Date())) {
					try (InputStream inputFileStream = new FileInputStream(inputFilePath)) {
						byte[] buffer = new byte[4096];
						int len;
						while ((len = inputFileStream.read(buffer)) > 0) {
							literalOutputStream.write(buffer, 0, len);
						}
					}
				}
			}

			encryptedOutputStream.close();
		}
	}

	private static PGPPublicKey readPublicKey(String publicKeyPath) throws IOException, PGPException {
		try (InputStream inputStream = new FileInputStream(publicKeyPath)) {
			InputStream decoderStream = PGPUtil.getDecoderStream(inputStream);
			PGPPublicKeyRingCollection pgpPublicKeyRingCollection = new PGPPublicKeyRingCollection(decoderStream);
			Iterator<PGPPublicKeyRing> keyRingIterator = pgpPublicKeyRingCollection.getKeyRings();
			while (keyRingIterator.hasNext()) {
				PGPPublicKeyRing keyRing = keyRingIterator.next();
				Iterator<PGPPublicKey> publicKeyIterator = keyRing.getPublicKeys();
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

	public static void deryptFile(String passphrase, String privateKeyPath, String downloadFilePath,
			String remoteDirectory) {

	}

	// Decrypt file using private key and passphrase
	public static void decryptFile(String inputFilePath, String outputFilePath, InputStream privateKeyInputStream,
			String passphrase) throws Exception {
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        try (
                InputStream inputStream = new BufferedInputStream(new FileInputStream(inputFilePath));
                OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFilePath))
        ) {
            // Load private key
            PGPSecretKeyRingCollection pgpSecretKeyRingCollection = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(privateKeyInputStream));
            PGPSecretKey secretKey = null;

            // Iterate over secret key rings
            Iterator<PGPSecretKeyRing> keyRingIterator = pgpSecretKeyRingCollection.getKeyRings();
            while (keyRingIterator.hasNext()) {
                PGPSecretKeyRing keyRing = keyRingIterator.next();
                Iterator<PGPSecretKey> secretKeyIterator = keyRing.getSecretKeys();
                while (secretKeyIterator.hasNext()) {
                    secretKey = secretKeyIterator.next();
                }
            }

            // Extract private key
            PGPPrivateKey privateKey = secretKey.extractPrivateKey(new org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder().setProvider("BC").build(passphrase.toCharArray()));

            // Decrypt the file
            PGPObjectFactory pgpObjectFactory = new PGPObjectFactory(inputStream, new JcaKeyFingerprintCalculator());
            Object object = pgpObjectFactory.nextObject();

            if (object instanceof PGPEncryptedDataList) {
                PGPEncryptedDataList encryptedDataList = (PGPEncryptedDataList) object;
                Iterator<PGPPublicKeyEncryptedData> publicKeyEncryptedDataIterator = encryptedDataList.getEncryptedDataObjects();
                while (publicKeyEncryptedDataIterator.hasNext()) {
                    PGPPublicKeyEncryptedData publicKeyEncryptedData = publicKeyEncryptedDataIterator.next();
                    InputStream clear = publicKeyEncryptedData.getDataStream(new JcePublicKeyDataDecryptorFactoryBuilder().setProvider("BC").build(privateKey));
                    pgpObjectFactory = new PGPObjectFactory(clear, new JcaKeyFingerprintCalculator());
                    object = pgpObjectFactory.nextObject();
                    if (object instanceof PGPCompressedData) {
                        PGPCompressedData compressedData = (PGPCompressedData) object;
                        pgpObjectFactory = new PGPObjectFactory(compressedData.getDataStream(), new JcaKeyFingerprintCalculator());
                        object = pgpObjectFactory.nextObject();
                    }
                    if (object instanceof PGPLiteralData) {
                        PGPLiteralData literalData = (PGPLiteralData) object;
                        InputStream literalDataStream = literalData.getInputStream();
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = literalDataStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }
                }
            }
        }
	}

	// Decrypt all files in a directory and move them to another directory
	public static void decryptAndMoveFiles(String inputDirectory, String outputDirectory,
			InputStream privateKeyInputStream, String passphrase) {
		try {
			File[] files = new File(inputDirectory).listFiles();
			if (files != null) {
				for (File file : files) {
					if (file.isFile()) {
						String inputFilePath = file.getAbsolutePath();
						String outputFilePath = outputDirectory + File.separator + file.getName();
						decryptFile(inputFilePath, outputFilePath, privateKeyInputStream, passphrase);
						System.out.println("File decrypted: " + file.getName());
						file.delete(); // Delete the encrypted file after decryption
					}
				}
			} else {
				System.err.println("No files found in the input directory.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Decrypt all files in a directory
	public static void decryptFilesInDirectory(String inputDirectory, String outputDirectory, String privateKeyPath,
			String passphrase) throws IOException, PGPException {
		try {
			InputStream privateKeyInputStream = new FileInputStream(new File(privateKeyPath)); // Private key input
																								// stream
			File[] files = new File(inputDirectory).listFiles();
			if (files != null) {
				for (File file : files) {
					if (file.isFile()) {
						String inputFilePath = file.getAbsolutePath();
						String outputFilePath = outputDirectory + File.separator + FilenameUtils.removeExtension(file.getName()+".txt");
						decryptFile(inputFilePath, outputFilePath, privateKeyInputStream, passphrase);
						System.out.println("File decrypted: " + file.getName());
					}
				}
			} else {
				System.err.println("No files found in the input directory.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
