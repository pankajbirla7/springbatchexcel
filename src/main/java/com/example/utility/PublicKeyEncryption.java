package com.example.utility;

import java.io.BufferedInputStream;
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
import java.util.List;
import java.util.ArrayList;
import org.apache.commons.io.FilenameUtils;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.bc.BcPGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;

public class PublicKeyEncryption {

	public static void encryptFile(String inputFilePath, String outputFilePath, String publicKeyPath)
			throws IOException, PGPException {
		try (OutputStream outputStream = new FileOutputStream(outputFilePath)) {
			PGPPublicKey publicKey = readPublicKey(publicKeyPath);

			PGPEncryptedDataGenerator encryptedDataGenerator = new PGPEncryptedDataGenerator(
					new JcePGPDataEncryptorBuilder(PGPEncryptedData.AES_256).setWithIntegrityPacket(true)
							.setSecureRandom(new SecureRandom()).setProvider("BC"));
			encryptedDataGenerator.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(publicKey).setProvider("BC"));

			try (OutputStream encryptedOut = encryptedDataGenerator.open(outputStream, new byte[4096])) {
				PGPCompressedDataGenerator compressedDataGenerator = new PGPCompressedDataGenerator(
						PGPCompressedData.ZIP);
				try (OutputStream compressedOut = compressedDataGenerator.open(encryptedOut)) {
					PGPLiteralDataGenerator literalDataGenerator = new PGPLiteralDataGenerator();
					try (OutputStream literalOut = literalDataGenerator.open(compressedOut, PGPLiteralData.BINARY,
							new File(inputFilePath))) {
						try (InputStream inputFile = new FileInputStream(inputFilePath)) {
							byte[] buffer = new byte[4096];
							int len;
							while ((len = inputFile.read(buffer)) > 0) {
								literalOut.write(buffer, 0, len);
							}
						}
					}
				}
			}
		}
	}

	private static PGPPublicKey readPublicKey(String publicKeyPath) throws IOException, PGPException {
		try (InputStream keyIn = new BufferedInputStream(new FileInputStream(publicKeyPath))) {
			InputStream decoderStream = PGPUtil.getDecoderStream(keyIn);
			PGPObjectFactory pgpFactory = new PGPObjectFactory(decoderStream, new JcaKeyFingerprintCalculator());
			Object object = pgpFactory.nextObject();
			PGPPublicKeyRing keyRing = null;

			while (object != null) {
				if (object instanceof PGPPublicKeyRing) {
					keyRing = (PGPPublicKeyRing) object;
					break;
				}
				object = pgpFactory.nextObject();
			}

			if (keyRing == null) {
				throw new IllegalArgumentException("No public key ring found in the provided file.");
			}

			Iterator<PGPPublicKey> keyIter = keyRing.getPublicKeys();
			while (keyIter.hasNext()) {
				PGPPublicKey key = keyIter.next();
				if (key.isEncryptionKey()) {
					return key;
				}
			}
		}
		throw new IllegalArgumentException("No encryption key found in the provided public key file.");
	}

	public static List<String> decryptFiles(String downloadFilePath, String decryptFilePath, String privateKeyPath,
			String passphrase) {
		List<String> decryptedFiles = new ArrayList<>();
		try {
			File[] files = new File(downloadFilePath).listFiles();
			if (files != null) {
				for (File file : files) {
					if (file.isFile()) {
						String inputFilePath = file.getAbsolutePath();
						String outputFilePath = decryptFilePath + File.separator
								+ FilenameUtils.removeExtension(file.getName()) + ".txt";
						boolean isDecryptionSuccess = decryptFileByCommandLine(inputFilePath, outputFilePath, privateKeyPath, passphrase);
						if(isDecryptionSuccess) {
							System.out.println("File decrypted: " + file.getName());
							decryptedFiles.add(outputFilePath);
						}else {
							System.out.println("decryption failed for the file : "+inputFilePath);
						}
					}
				}
			} else {
				System.err.println("No files found in the input directory.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return decryptedFiles;
	}

	private static boolean decryptFileByCommandLine(String encryptedFilePath, String decryptedFilePath,
			String privateKeyPath, String passphrase) {
		String[] command = { "gpg", "--batch", "--quiet", "--pinentry-mode", "loopback", "--passphrase", passphrase,
				"--decrypt", encryptedFilePath };

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
			return false;
		}
		return true;
	}
}