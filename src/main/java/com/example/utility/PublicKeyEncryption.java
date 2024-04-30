package com.example.utility;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;

import java.io.*;
import java.security.SecureRandom;
import java.util.Iterator;

public class PublicKeyEncryption {
	public static void encryptFile(String inputFilePath, String outputFilePath, String publicKeyPath, String passphrase)
			throws IOException, PGPException {
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

}
