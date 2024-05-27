package com.example.utility;

import com.example.service.StdClaimService;
import com.jcraft.jsch.*;

import org.bouncycastle.openpgp.PGPException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class Utility {

	@Autowired
	StdClaimService stdClaimService;

///////////////////////////// Encrypt File and Upload File To SFTP server //////////////////////////// 

	public void encryptAndUpload(String inputFilePath, String outputFilePath, String publicKeyPath, String passphrase,
			String host, int port, String username, String password, String privateKeyPath, String remoteDirectory)
			throws IOException, PGPException, JSchException {
		// Encrypt the file
		PublicKeyEncryption.encryptFile(inputFilePath, outputFilePath, publicKeyPath);

		// Upload the encrypted file to SFTP server
		try {
			uploadFile(outputFilePath, host, port, username, password, passphrase, privateKeyPath, remoteDirectory);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

// Upload file to SFTP server
		public void uploadFile(String localFilePath, String host, int port, String username, String password,
				String passphrase, String privateKeyPath, String remoteDirectory)
				throws JSchException, SftpException, FileNotFoundException {
			try {

				File file = new File(localFilePath);
				// Establishing the session
				JSch jsch = new JSch();
				Session session = jsch.getSession(username, host, port);
				session.setPassword(password);
				session.setConfig("StrictHostKeyChecking", "no");
				session.connect();

				// Opening the SFTP channel
				ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
				channelSftp.connect();

				// Uploading the encrypted file
//				FileInputStream encryptedFileInputStream = new FileInputStream(file);
//				channelSftp.put(encryptedFileInputStream, remoteDirectory + file.getName());
				
				// Change to the target directory
	            channelSftp.cd(remoteDirectory);

	            // Upload file
	            try (FileInputStream fis = new FileInputStream(file)) {
	                channelSftp.put(fis, file.getName());
	            }

	            System.out.println("File uploaded successfully to " + localFilePath);

				// Disconnecting the channel and session
				channelSftp.disconnect();
				session.disconnect();

				// Closing the file input stream
			//	encryptedFileInputStream.close();

				System.out.println("File uploaded successfully.");
			} catch (JSchException | SftpException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	public void moveFileToSFTP(String inputFilePath, String outputFilePath, String publicKeyPath, String passphrase,
			String host, int port, String username, String password, String privateKeyPath, String remoteDirectory) {
		try {
			encryptAndUpload(inputFilePath, outputFilePath, publicKeyPath, passphrase, host, port, username, password,
					privateKeyPath, remoteDirectory);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

/////////////////////////////// Download SFTP File And Decryopt /////////////////////////////// 

	public void downloadFilesFromSftpAndDecrypt(String downloadFilePath, String decryptFilePath, String passphrase,
			String host, int port, String username, String password, String privateKeyPath, String remoteDirectory) {
		try {
			downloadFiles(host, port, username, password, passphrase, privateKeyPath, downloadFilePath,
					remoteDirectory);
			decryptFile(passphrase, privateKeyPath, downloadFilePath, decryptFilePath);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void decryptFile(String passphrase, String privateKeyPath, String downloadFilePath,
			String decryptFilePath) {

		// Decrypting files
		try {
			List<String> filePaths = PublicKeyEncryption.decryptFiles(downloadFilePath, decryptFilePath, privateKeyPath,
					passphrase);
			if (filePaths != null && filePaths.size() > 0) {
				parseFilesAndSaveVoucherDetails(filePaths);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void downloadFiles(String host, int port, String username, String password, String passphrase,
			String privateKeyPath, String downloadFilePath, String remoteDirectory) {
		try {
			JSch jsch = new JSch();
			Session session = jsch.getSession(username, host, port);
			session.setPassword(password);
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect();

			ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
			channelSftp.connect();

			channelSftp.cd(remoteDirectory);

			@SuppressWarnings("unchecked")
			Vector<ChannelSftp.LsEntry> fileList = channelSftp.ls(".");
			for (ChannelSftp.LsEntry entry : fileList) {
				if (!entry.getAttrs().isDir()) {
					String remoteFileName = entry.getFilename();
					String localFilePath = downloadFilePath + File.separator + remoteFileName;
					channelSftp.get(remoteFileName, localFilePath);

					System.out.println("File downloaded: " + remoteFileName);
				}
			}

			channelSftp.disconnect();
			session.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("All files downloaded successfully.");
	}

	public void parseFilesAndSaveVoucherDetails(List<String> outputFilePaths) {
		for (String outputFilePath : outputFilePaths) {
			Map<String, String> voucherDetailsAndStatusMap = new HashMap<>();
			try {
				File file = new File(outputFilePath);
				Scanner scanner = new Scanner(file);

				// Skip the first two lines (header rows)
				scanner.nextLine(); // Skip first header row
				scanner.nextLine(); // Skip second header row

				while (scanner.hasNextLine()) {
					String dataLine = scanner.nextLine();
					String[] values = dataLine.trim().split("\\s+");

					for (String value : values) {
						System.out.print(value + " ");
						String docNumber = values[3];
						String error = values[7];
						if (docNumber != null && docNumber.startsWith("1S")) {
							if (voucherDetailsAndStatusMap.get(docNumber) != null) {
								if (!voucherDetailsAndStatusMap.get(docNumber).equals("SUCCESS")
										&& !voucherDetailsAndStatusMap.get(docNumber).equals("ERROR")) {
									voucherDetailsAndStatusMap.put(docNumber, error);
								}
							} else {
								voucherDetailsAndStatusMap.put(docNumber, error);
							}
						}
					}
					System.out.println();
				}

				scanner.close();

			} catch (FileNotFoundException e) {
				System.out.println("File not found: " + outputFilePath);
				e.printStackTrace();
			}
			System.out.println("Voucher Details Map : " + voucherDetailsAndStatusMap);

			stdClaimService.updateVoucherDetailsAndStatus(voucherDetailsAndStatusMap);
		}
	}

	public static int getClaimIdFromVoucher(String voucher) {
		int claimId = 0;
		// Define the regular expression pattern
		Pattern pattern = Pattern.compile("^1S0*([1-9][0-9]*)");

		// Match the pattern against the input string
		Matcher matcher = pattern.matcher(voucher);

		// Check if the pattern matches
		if (matcher.find()) {
			// Extract the matched group
			String extracted = matcher.group(1);
			claimId = Integer.parseInt(extracted);
		}
		return claimId;
	}
}