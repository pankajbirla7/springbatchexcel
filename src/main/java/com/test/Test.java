package com.test;

import java.io.FileWriter;

import com.example.utility.Utility;

public class Test {

	public static void main(String[] args) {
		Test t = new Test();
		String encryptedFilePath = "";
		String decryptedFilePath = "";
		String privateKeyPath = "";
		String passphrase = "";
		boolean isSuccess = t.decryptFileByCommandLine(null, null, null, null);
		System.out.println("File decrypted success :: "+isSuccess);
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

		} catch (Exception e) {
			System.out.println("File decryption error for encrypted file. " + encryptedFilePath+" :: error occured due to :: "+Utility.getStackTrace(e));
			e.printStackTrace();
			return false;
		}
		return true;
	}

}
