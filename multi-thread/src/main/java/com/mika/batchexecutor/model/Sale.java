package com.mika.batchexecutor.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class Sale {
    String region;
    String country;
    String itemType;
    String salesChannel;
    String orderPriority;
    LocalDate orderDate;
    String orderId;
    LocalDate shipDate;
    BigDecimal unitsSold;
    BigDecimal unitPrice;
    BigDecimal unitCost;
    BigDecimal totalRevenue;
    BigDecimal totalCost;
    BigDecimal totalProfit;
}


