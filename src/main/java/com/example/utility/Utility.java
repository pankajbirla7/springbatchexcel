package com.example.utility;

import com.example.service.StdClaimService;
import com.jcraft.jsch.*;

import org.bouncycastle.openpgp.PGPException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;

import org.apache.commons.io.FilenameUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Component
public class Utility {

	@Autowired
	StdClaimService stdClaimService;

///////////////////////////// Encrypt File and Upload File To SFTP server
///////////////////////////// ////////////////////////////

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
// FileInputStream encryptedFileInputStream = new FileInputStream(file);
// channelSftp.put(encryptedFileInputStream, remoteDirectory + file.getName());

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
// encryptedFileInputStream.close();

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

/////////////////////////////// Download SFTP File And Decryopt
/////////////////////////////// ///////////////////////////////

	public void downloadFilesFromSftpAndDecrypt(String downloadFilePath, String decryptFilePath, String passphrase,
			String host, int port, String username, String password, String privateKeyPath, String remoteDirectory,
			String sftpRemoteArchiveDirectory, boolean isProcessVoucherDetails, String archiveDownloadDirectory,
			String filePattern) {
		try {
			downloadFiles(host, port, username, password, passphrase, privateKeyPath, downloadFilePath, remoteDirectory,
					filePattern);
			decryptFile(passphrase, privateKeyPath, downloadFilePath, decryptFilePath, isProcessVoucherDetails);
			archiveSftpFiles(host, port, username, password, passphrase, privateKeyPath, downloadFilePath,
					remoteDirectory, sftpRemoteArchiveDirectory, filePattern);
			archiveFiles(downloadFilePath, archiveDownloadDirectory);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void archiveFiles(String downloadFilePath, String archiveDownloadDirectory) {
		Path sourceDir = Paths.get(downloadFilePath);
		Path destinationDir = Paths.get(archiveDownloadDirectory);

		try (Stream<Path> files = Files.list(sourceDir)) {
			files.forEach(file -> {
				try {
					// Check if the current path is a regular file (not a directory or symbolic
					// link)
					if (Files.isRegularFile(file)) {
						// Define the target path in the destination directory
						Path targetPath = destinationDir.resolve(file.getFileName());

						// Move the file to the target directory
						Files.move(file, targetPath, StandardCopyOption.REPLACE_EXISTING);
						System.out.println("Moved: " + file.getFileName());
					}
				} catch (IOException e) {
					System.err.println("Failed to move file: " + file.getFileName() + " due to: " + e.getMessage());
				}
			});
		} catch (IOException e) {
			System.err.println("Failed to list files in the source directory: " + e.getMessage());
		}
	}

	private void archiveSftpFiles(String host, int port, String username, String password, String passphrase,
			String privateKeyPath, String downloadFilePath, String remoteDirectory, String sftpRemoteArchiveDirectory,
			String filePattern) {

		Session session = null;
		ChannelSftp channelSftp = null;
		try {
			JSch jsch = new JSch();
			session = jsch.getSession(username, host, port);
			session.setPassword(password);

			// Configure the session
			Properties config = new Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);

			// Connect to the session
			session.connect();

			// Open the SFTP channel
			channelSftp = (ChannelSftp) session.openChannel("sftp");
			channelSftp.connect();

			// List files in the source directory
			Vector<ChannelSftp.LsEntry> files = channelSftp.ls(remoteDirectory);

			for (ChannelSftp.LsEntry file : files) {
				if (!file.getAttrs().isDir()) {
					String fileName = file.getFilename();
					if (filePattern != null) {
						if (fileName.contains(filePattern)) {
							String sourceFilePath = remoteDirectory + "/" + fileName;
							String destinationFilePath = sftpRemoteArchiveDirectory + "/" + fileName;

							// Move the file
							channelSftp.rename(sourceFilePath, destinationFilePath);
							System.out.println("Moved file: " + sourceFilePath + " to " + destinationFilePath);
						}
					} else {
						String sourceFilePath = remoteDirectory + "/" + fileName;
						String destinationFilePath = sftpRemoteArchiveDirectory + "/" + fileName;

						// Move the file
						channelSftp.rename(sourceFilePath, destinationFilePath);
						System.out.println("Moved file: " + sourceFilePath + " to " + destinationFilePath);
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (channelSftp != null) {
				channelSftp.disconnect();
			}
			if (session != null) {
				session.disconnect();
			}
		}
	}

	private void decryptFile(String passphrase, String privateKeyPath, String downloadFilePath, String decryptFilePath,
			boolean isProcessVoucherDetails) {

		// Decrypting files
		try {
			List<String> filePaths = PublicKeyEncryption.decryptFiles(downloadFilePath, decryptFilePath, privateKeyPath,
					passphrase);
			if (isProcessVoucherDetails && filePaths != null && filePaths.size() > 0) {
				parseFilesAndSaveVoucherDetails(filePaths);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void downloadFiles(String host, int port, String username, String password, String passphrase,
			String privateKeyPath, String downloadFilePath, String remoteDirectory, String filePattern) {
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
					if (filePattern != null) {
						if (remoteFileName.contains(filePattern)) {
							String localFilePath = downloadFilePath + File.separator + remoteFileName;
							channelSftp.get(remoteFileName, localFilePath);

							System.out.println("File downloaded: " + remoteFileName);
						}
					} else {
						String localFilePath = downloadFilePath + File.separator + remoteFileName;
						channelSftp.get(remoteFileName, localFilePath);

						System.out.println("File downloaded: " + remoteFileName);
					}
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
								if (!voucherDetailsAndStatusMap.get(docNumber).equalsIgnoreCase("SUCCESS")
										&& !voucherDetailsAndStatusMap.get(docNumber).equalsIgnoreCase("ERROR")) {
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

	public void migaretCsvToPdfFiles(String csvFilePath, String pdfFilePath) {
		try {
			List<Path> filePaths = listFiles(csvFilePath);
			for (Path filePath : filePaths) {
				File file = filePath.toFile();
				PdfWriter writer = new PdfWriter(
						pdfFilePath + File.separator + FilenameUtils.removeExtension(file.getName()) + ".pdf");

				PdfDocument pdfDoc = new PdfDocument(writer);

				Document document = new Document(pdfDoc);

				Path path = Paths.get(file.getAbsolutePath());
				List<String> lines = Files.readAllLines(path);

				if (!lines.isEmpty()) {
					String[] headers = lines.get(0).split(",");
					Table table = new Table(headers.length);

					for (String header : headers) {
						table.addHeaderCell(new Cell().add(new Paragraph(header)));
					}

					for (int i = 1; i < lines.size(); i++) {
						String[] values = lines.get(i).split(",");
						for (String value : values) {
							table.addCell(new Cell().add(new Paragraph(value)));
						}
					}

					document.add(table);
				}

				document.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static List<Path> listFiles(String directoryPath) throws IOException {
		List<Path> filePaths = new ArrayList<>();
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(directoryPath))) {
			for (Path path : directoryStream) {
				if (Files.isRegularFile(path)) {
					filePaths.add(path);
				}
			}
		}
		return filePaths;
	}
}