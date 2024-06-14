package com.example.demo;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private DataSource dataSource;

//    @Autowired
//    private Step moveFileStep;
//    
//	@Value("${input.file.paths}")
//	private String inputFiles;
//
//	@Value("${output.file.path}")
//	private String outputFileDirectory;

	// @Bean
//	public ExcelItemReader excelItemReader() {
//		return new ExcelItemReader();
//	}

	@Bean
	public ExcelItemWriter excelItemWriter() {
		return new ExcelItemWriter(dataSource);
	}
	
	@Bean
	public FileDetailsWriter fileDetails() {
		return new FileDetailsWriter(dataSource);
	}
	
	@Bean
    public ExcelItemReader customExcelItemReader() {
        return new ExcelItemReader(excelItemWriter(), fileDetails(), dataSource, transactionManager);
    }

	@Bean
	public Step step1(ItemReader<MyDataObject> reader, ItemWriter<MyDataObject> writer) {
		return new StepBuilder("step1", jobRepository)
				.<MyDataObject, MyDataObject>chunk(100000)
				.reader(customExcelItemReader())
				.writer(excelItemWriter())
				.transactionManager(transactionManager)
				.build();
	}

	@Bean
	public Job importUserJob(Step step1) {
		return new JobBuilder("importUserJob", jobRepository).
				incrementer(new RunIdIncrementer())
				.flow(step1).end()
				.build();
	}
}
