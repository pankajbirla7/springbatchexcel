package com.example.utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
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

import org.apache.commons.io.FilenameUtils;
import org.bouncycastle.openpgp.PGPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.service.StdClaimService;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.VerticalAlignment;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

@Component
public class Utility {
	
	static final Logger logger = LoggerFactory.getLogger(Utility.class);

	@Autowired
	StdClaimService stdClaimService;

///////////////////////////// Encrypt File and Upload File To SFTP server
///////////////////////////// ////////////////////////////

	public void encryptAndUpload(String inputFilePath, String outputFilePath, String publicKeyPath, String passphrase,
			String host, int port, String username, String password, String privateKeyPath, String remoteDirectory)
			throws Exception {
		try {
			// Encrypt the file
			PublicKeyEncryption.encryptFile(inputFilePath, outputFilePath, publicKeyPath);
	
			// Upload the encrypted file to SFTP server
		
			uploadFile(outputFilePath, host, port, username, password, passphrase, privateKeyPath, remoteDirectory);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error occured during encryptAndUpload method due to : "+getStackTrace(e));
			throw e;
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

			// Change to the target directory
			channelSftp.cd(remoteDirectory);

			// Upload file
			try (FileInputStream fis = new FileInputStream(file)) {
				channelSftp.put(fis, file.getName());
			}

			// Disconnecting the channel and session
			channelSftp.disconnect();
			session.disconnect();

			logger.info("File uploaded successfully to path :: "+localFilePath);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error occured during upload file to sftp due to : "+getStackTrace(e));
		}
	}

	public void moveFileToSFTP(String inputFilePath, String outputFilePath, String publicKeyPath, String passphrase,
			String host, int port, String username, String password, String privateKeyPath, String remoteDirectory) throws Exception {
		try {
			encryptAndUpload(inputFilePath, outputFilePath, publicKeyPath, passphrase, host, port, username, password,
					privateKeyPath, remoteDirectory);
		} catch (Exception e) {
			logger.error("Error occured during movingFileToSFTP method due to : "+getStackTrace(e));
			throw e;
		}
	}

/////////////////////////////// Download SFTP File And Decryopt
/////////////////////////////// ///////////////////////////////

	public int downloadFilesFromSftpAndDecrypt(String downloadFilePath, String decryptFilePath, String passphrase,
			String host, int port, String username, String password, String privateKeyPath, String remoteDirectory,
			String sftpRemoteArchiveDirectory, boolean isProcessVoucherDetails, String archiveDownloadDirectory,
			String filePattern) throws Exception {
		int count = 0;
		try {
			count = downloadFiles(host, port, username, password, passphrase, privateKeyPath, downloadFilePath, remoteDirectory,
					filePattern);
			if(count > 0) {
				decryptFile(passphrase, privateKeyPath, downloadFilePath, decryptFilePath, isProcessVoucherDetails);
				archiveSftpFiles(host, port, username, password, passphrase, privateKeyPath, downloadFilePath,
						remoteDirectory, sftpRemoteArchiveDirectory, filePattern);
				archiveFiles(downloadFilePath, archiveDownloadDirectory);
			}
		} catch (Exception e) {
			logger.error("Error occured during downloadFilesFromSftpAndDecrypt method due to : "+getStackTrace(e));
			throw e;
		}
		return count;
	}

	private void archiveFiles(String downloadFilePath, String archiveDownloadDirectory) throws Exception {
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
					}
				} catch (Exception e) {
					logger.error("Error occurred in method archiveFiles , Failed to move file: " + file.getFileName() + " due to :: " + getStackTrace(e));
				}
			});
			logger.error("All files archive successfully in method archiveFiles.");
		} catch (Exception e) {
			logger.error("Error occurred in method archiveFiles , Failed to list files in the source directory: due to :: " + getStackTrace(e));
			throw e;
		}
	}

	private void archiveSftpFiles(String host, int port, String username, String password, String passphrase,
			String privateKeyPath, String downloadFilePath, String remoteDirectory, String sftpRemoteArchiveDirectory,
			String filePattern) throws Exception {

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
							logger.info("Archive SFTP Files - Moved file: " + sourceFilePath + " to " + destinationFilePath);
						}
					} else {
						String sourceFilePath = remoteDirectory + "/" + fileName;
						String destinationFilePath = sftpRemoteArchiveDirectory + "/" + fileName;

						// Move the file
						channelSftp.rename(sourceFilePath, destinationFilePath);
						logger.info("Archive SFTP Files - Moved file: " + sourceFilePath + " to " + destinationFilePath);
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error occured in archiveSftpFiles method, during moving sfto files to archive due to :: "+getStackTrace(e));
			throw e;
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
			boolean isProcessVoucherDetails) throws Exception {

		// Decrypting files
		try {
			List<String> filePaths = PublicKeyEncryption.decryptFiles(downloadFilePath, decryptFilePath, privateKeyPath,
					passphrase);
			if (isProcessVoucherDetails && filePaths != null && filePaths.size() > 0) {
				parseFilesAndSaveVoucherDetails(filePaths);
			}

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error occurred in decryptFile method, during decryptFile due to :: "+getStackTrace(e));
			throw e;
		}
	}

	private static int downloadFiles(String host, int port, String username, String password, String passphrase,
			String privateKeyPath, String downloadFilePath, String remoteDirectory, String filePattern) throws Exception {
		int count = 0;
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

							logger.info("File downloaded : " + remoteFileName);
							count++;
						}
					} else {
						String localFilePath = downloadFilePath + File.separator + remoteFileName;
						channelSftp.get(remoteFileName, localFilePath);

						logger.info("File downloaded: " + remoteFileName);
						count++;
					}
				}
			}

			channelSftp.disconnect();
			session.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error occured in downloadFiles method , during download files :: "+getStackTrace(e));
			throw e;
		}
		logger.info("All files downloaded successfully.");
		return count;
	}

	public void parseFilesAndSaveVoucherDetails(List<String> outputFilePaths) throws Exception {
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
				
				logger.info("Voucher Details Map : " + voucherDetailsAndStatusMap);

				stdClaimService.updateVoucherDetailsAndStatus(voucherDetailsAndStatusMap);

			} catch (Exception e) {
				logger.error("Error occured during parseFilesAndSaveVoucherDetails for file " + outputFilePath + " :: Error is :: "+getStackTrace(e));
				throw e;
			}
			
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

	public void migaretCsvToPdfFiles(String csvFilePath, String pdfFilePath) throws Exception {
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
						Cell headerCell = new Cell().add(new Paragraph(header).setBold())
								.setBackgroundColor(new DeviceRgb(221, 221, 221)) // light grey background
								.setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE)
								.setBorder(new SolidBorder(1));
						table.addHeaderCell(headerCell);
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
			logger.error("Error occured during migaretCsvToPdfFiles due to :: "+getStackTrace(e));
			throw e;
		}
	}
	
	public void migrateHtmlToPdfFiles(String htmlFilePath, String pdfDirectoryPath) throws Exception {
		try {
			List<Path> filePaths = listFiles(htmlFilePath);
			for (Path filePath : filePaths) {
				File file = filePath.toFile();
				String pdfFilePath = pdfDirectoryPath + File.separator + FilenameUtils.removeExtension(new File(htmlFilePath).getName())
				+ ".pdf";
				BufferedReader reader = new BufferedReader(new FileReader(file.getAbsolutePath()));
	            StringWriter writer = new StringWriter();
	            String line;
	            while ((line = reader.readLine()) != null) {
	                writer.write(line);
	            }
	            reader.close();
	            String htmlContent = writer.toString().trim(); // Trim leading/trailing whitespace

	            // Remove BOM if present
	            if (htmlContent.startsWith("\uFEFF")) {
	                htmlContent = htmlContent.substring(1);
	            }

	            // Ensure HTML content starts with the correct tag
	            if (!htmlContent.startsWith("<html>")) {
	                throw new IllegalArgumentException("Invalid HTML content");
	            }

	            // Create a FileOutputStream to write the PDF file
	            try (FileOutputStream os = new FileOutputStream(pdfFilePath)) {
	                // Create the PDF renderer
	                PdfRendererBuilder builder = new PdfRendererBuilder();
	                builder.withHtmlContent(htmlContent, new File(htmlFilePath).toURI().toString());
	                builder.toStream(os);
	                builder.run();
	            }

	            logger.info("PDF created successfully for html file - "+file.getAbsolutePath()+ " :: pdf path - "+pdfFilePath);
			}
		} catch (Exception e) {
			logger.error("Error occured during migaretHtmlToPdfFiles due to :: "+getStackTrace(e));
			throw e;
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

	public static String getStackTrace(Exception ex) {
		Writer buffer = new StringWriter();
		PrintWriter pw = new PrintWriter(buffer);
		ex.printStackTrace(pw);
		return buffer.toString();
	}
}