package com.example.demo;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.service.FileWriteService;

@Component
public class JobRunner {

	private final Job importUserJob;
	private final JobLauncher jobLauncher;
	
	@Autowired
	FileWriteService fileWriteService;

	public JobRunner(Job importUserJob, JobLauncher jobLauncher) {
		this.importUserJob = importUserJob;
		this.jobLauncher = jobLauncher;
	}

	@Scheduled(cron = "0 0/2 * ? * *")
	public void run() throws Exception {
		JobParameters jobParameters = new JobParametersBuilder().addLong("time", System.currentTimeMillis())
				.toJobParameters();
		JobExecution jobExecution = jobLauncher.run(importUserJob, jobParameters);
		BatchStatus batchStatus = jobExecution.getStatus();
	//	System.out.println("Batch Status: " + batchStatus);
	}

	@Scheduled(cron = "0 0/1 * ? * *")
	public void prepareFile() throws Exception {
		System.out.println("Batch Job 2 started ");
		fileWriteService.generateFile();
		
	}

	@Scheduled(cron = "0 0/1 * ? * *")
	public void downloadFileAndDecrpt() throws Exception {
		System.out.println("Batch Job 3 started ");
		fileWriteService.downloadAndDecrptFile();
		
	}
	

	@Scheduled(cron = "0 0/20 * ? * *")
	public void downloadAndDecryptProcessedFiles() throws Exception {
		System.out.println("Batch Job 4 started ");
		fileWriteService.downloadAndDecryptProcessedFiles();
		
	}
	
	@Scheduled(cron = "0 0/20 * ? * *")
	public void csvToPdfMigrationJob() throws Exception {
		System.out.println("Batch Job 5 started ");
		fileWriteService.downloadAndDecryptProcessedFiles();
		
	}
}
