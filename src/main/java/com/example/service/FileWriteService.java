package com.example.service;

import org.springframework.transaction.PlatformTransactionManager;

public interface FileWriteService {
	
	public void generateFile();
	
	public void downloadAndDecrptFile();
	
	public void downloadAndDecryptProcessedFiles(PlatformTransactionManager transactionManager);
	
	public void migrateCsvToPdfFiles() throws Exception;

	void migrateHtmlToPdfFiles() throws Exception;

}
