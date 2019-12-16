package com.mika.batchexecutor.processors;


import com.mika.batchexecutor.model.Sale;
import org.springframework.batch.item.ItemProcessor;


public class DataProcessor implements ItemProcessor<Sale, Sale> {
    @Override
    public Sale process(Sale data) throws Exception {

        return data;
    }
}
