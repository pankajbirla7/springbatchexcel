package com.example.utility;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;

import java.io.*;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Iterator;

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
		try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFilePath));
				InputStream inputStream = new BufferedInputStream(new FileInputStream(inputFilePath))) {
			// Decrypt the file using the private key and passphrase
			PGPObjectFactory pgpF = new PGPObjectFactory(inputStream);
			Object o;
			while ((o = pgpF.nextObject()) != null) {
				if (o instanceof org.bouncycastle.openpgp.PGPCompressedData) {
					org.bouncycastle.openpgp.PGPCompressedData cData = (org.bouncycastle.openpgp.PGPCompressedData) o;
					try (InputStream clear = cData.getDataStream()) {
						PGPObjectFactory pgpFact = new PGPObjectFactory(clear);
						Object message = pgpFact.nextObject();
						if (message instanceof org.bouncycastle.openpgp.PGPOnePassSignatureList) {
							message = pgpFact.nextObject();
						}
						if (message instanceof org.bouncycastle.openpgp.PGPLiteralData) {
							org.bouncycastle.openpgp.PGPLiteralData ld = (org.bouncycastle.openpgp.PGPLiteralData) message;
							try (InputStream literalDataInputStream = ld.getInputStream()) {
								byte[] buffer = new byte[1024];
								int bytesRead;
								while ((bytesRead = literalDataInputStream.read(buffer)) != -1) {
									outputStream.write(buffer, 0, bytesRead);
								}
							}
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
						String outputFilePath = outputDirectory + File.separator + file.getName();
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
