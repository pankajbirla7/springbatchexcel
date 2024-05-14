package com.example.utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
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
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.bc.BcPGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
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
			PGPPublicKeyRingCollection pgpPublicKeyRingCollection = new BcPGPPublicKeyRingCollection(decoderStream);
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

	public static void decryptFiles(String downloadFilePath, String decryptFilePath, String privateKeyPath,
			String passphrase) {
		try {
			File[] files = new File(downloadFilePath).listFiles();
			if (files != null) {
				for (File file : files) {
					if (file.isFile()) {
						String inputFilePath = file.getAbsolutePath();
						String outputFilePath = decryptFilePath + File.separator
								+ FilenameUtils.removeExtension(file.getName()) + ".txt";
						decryptFileByCommandLine(inputFilePath, outputFilePath, privateKeyPath, passphrase);
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

	private static void decryptFileByCommandLine(String encryptedFilePath, String decryptedFilePath,
			String privateKeyPath, String passphrase) {
		String[] command = { "gpg", "--batch", "--quiet", "--pinentry-mode", "loopback", "--passphrase",
				passphrase, "--decrypt", encryptedFilePath};

		try {
			ProcessBuilder pb = new ProcessBuilder(command);
			// Redirect error stream to output stream
			pb.redirectErrorStream(true);

			// Start the process
			Process process = pb.start();

			// Read the output of the process
			java.util.Scanner scanner = new java.util.Scanner(process.getInputStream());
			FileWriter writer = new FileWriter(decryptedFilePath);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				writer.write(line + "\n");
				System.out.println(line);
			}
			scanner.close();
			writer.close();

			// Wait for the process to complete
			int exitCode = process.waitFor();
			System.out.println("Process exited with code: " + exitCode);

		} catch (IOException | InterruptedException e) {
			System.out.println("File decryption error for encrypted file. " + encryptedFilePath);
			e.printStackTrace();
		}
	}
}
