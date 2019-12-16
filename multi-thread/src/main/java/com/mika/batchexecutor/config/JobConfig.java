package com.mika.batchexecutor.config;

import com.mika.batchexecutor.listeners.JobEventListener;
import com.mika.batchexecutor.model.Sale;
import com.mika.batchexecutor.processors.DataProcessor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Configuration
public class JobConfig {

    private static final int CHUNK_SIZE = 100_000;

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Autowired
    public DataSource dataSource;


    @Bean
    public Job importUserJob(JobEventListener listener, @Qualifier("asyncReadWriteStep") Step step) {
        return jobBuilderFactory.get("Job")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(step)
                .build();
    }


  //////////////  multi thread
//    @Bean
//    @Qualifier("multiThreadStep")
//    public Step multiThreadStep(ItemReader<Sale> reader, ItemWriter<Sale> writer) {
//        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
//        threadPoolTaskExecutor.setCorePoolSize(5);
//        threadPoolTaskExecutor.setMaxPoolSize(5);
//        threadPoolTaskExecutor.afterPropertiesSet();
//
//        return stepBuilderFactory.get("multiThreadStep")
//                .<Sale, Sale> chunk(CHUNK_SIZE)
//                .reader(reader)
//                .processor(processor())
//                .writer(writer)
//                .taskExecutor(threadPoolTaskExecutor)
//                .build();
//    }

//******************* ASYNC

    @Bean
    @Qualifier("asyncReadWriteStep")
    public Step asyncReadWriteStep() {
        return this.stepBuilderFactory.get("asyncReadWriteStep")
                .<Sale, Sale>chunk(CHUNK_SIZE)
                .reader(reader(null))
                .processor((ItemProcessor) asyncItemProcessor())
                .writer(asyncItemWriter())
                .build();
    }

    @Bean
    public AsyncItemProcessor<Sale, Sale> asyncItemProcessor() {
        AsyncItemProcessor<Sale, Sale> processor = new AsyncItemProcessor<>();
        processor.setDelegate(processor());
        processor.setTaskExecutor(new SimpleAsyncTaskExecutor());
        return processor;
    }

    @Bean
    public AsyncItemWriter<Sale> asyncItemWriter() {
        AsyncItemWriter<Sale> writer = new AsyncItemWriter<>();
        writer.setDelegate(writer(null));
        return writer;
    }

//*************************************************************
    @Bean
    @StepScope
    public FlatFileItemReader<Sale> reader(@Value("#{jobParameters['inputFile']}") Resource resource) {
        return new FlatFileItemReaderBuilder<Sale>()
                .name("personItemReader")
                .resource(new ClassPathResource("/data/test1M.csv"))
                .delimited()
                .names("Region","Country","Item Type","Sales Channel","Order Priority","Order Date","Order ID","Ship Date","Units Sold","Unit Price","Unit Cost","Total Revenue","Total Cost","Total Profit")
                .fieldSetMapper(new BeanWrapperFieldSetMapper<Sale>() {{
                    setTargetType(Sale.class);
                    setConversionService(conversionService());
                }})
                .linesToSkip(1)
                .build();
    }

    @Bean
    public ItemProcessor<Sale, Sale>  processor() {
        return new DataProcessor();
    }

    @Bean
    @StepScope
    public ItemWriter<Sale> writer(DataSource datasource) {
        JdbcBatchItemWriter<Sale> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(datasource);
        writer.setSql("insert into SALES(REGION,COUNTRY,ITEM_TYPE,SALES_CHANNEL,ORDER_PRIORITY,ORDER_DATE,ORDER_ID,SHIP_DATE,UNITS_SOLD,UNIT_PRICE,UNIT_COST,TOTAL_REVENUE,TOTAL_COST,TOTAL_PROFIT) " +
                "values ( :region, :country, :itemType, :salesChannel, :orderPriority, :orderDate, :orderId, :shipDate, :unitsSold, :unitPrice, :unitCost, :totalRevenue, :totalCost, :totalProfit )");
        writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>());
        return writer;
    }

    private ConversionService conversionService() {
        DefaultConversionService conversionService = new DefaultConversionService();
        DefaultConversionService.addDefaultConverters(conversionService);
        conversionService.addConverter(new Converter<String, LocalDate>() {
            @Override
            public LocalDate convert(String text) {
                return LocalDate.parse(text,DateTimeFormatter.ofPattern("M/d/yyyy"));
            }
        });

        return conversionService;
    }

}
