package com.example.demo;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.extensions.excel.RowMapper;
import org.springframework.batch.extensions.excel.poi.PoiItemReader;
import org.springframework.batch.extensions.excel.support.rowset.RowSet;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Bean
    public PoiItemReader<MyDataObject> reader() {
        PoiItemReader<MyDataObject> reader = new PoiItemReader<>();
        reader.setResource(new FileSystemResource("C:\\Users\\acer\\Documents\\myfile.xlsx"));
        reader.setRowMapper(new FinDataCustomRowMapper());
        return reader;
    }

    @Bean
    public RowMapper<MyDataObject> excelRowMapper() {
        return (RowSet rs) -> {
            MyDataObject dataObject = new MyDataObject();
            dataObject.setFin(rs.getCurrentRow()[0]);
            dataObject.setCin(rs.getCurrentRow()[1]);
            dataObject.setDateEntered(rs.getCurrentRow()[2]);
            dataObject.setNdc(rs.getCurrentRow()[3]);
            return dataObject;
        };
    }

    @Bean
    public ItemWriter<MyDataObject> writer(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<MyDataObject>()
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .sql("INSERT INTO findata (FIN, CIN, DateEntered, NDC) VALUES (:fin, :cin, :dateEntered, :ndc)")
                .dataSource(dataSource)
                .build();
    }

    @Bean
    public Step step1(ItemReader<MyDataObject> reader, ItemWriter<MyDataObject> writer) {
        return new StepBuilder("step1", jobRepository)
                .<MyDataObject, MyDataObject>chunk(10)
                .reader(reader)
                .writer(writer)
                .transactionManager(transactionManager)
                .build();
    }

    @Bean
    public Job importUserJob(Step step1) {
        return new JobBuilder("importUserJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .flow(step1)
                .end()
                .build();
    }
}