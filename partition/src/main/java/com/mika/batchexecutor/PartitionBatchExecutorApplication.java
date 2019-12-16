package com.mika.batchexecutor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PartitionBatchExecutorApplication {

    public static void main(String[] args) {
        String [] customArgs = new String[] {"inputFile=/data/test1M.csv"};
        SpringApplication.run(PartitionBatchExecutorApplication.class, customArgs);
    }

}