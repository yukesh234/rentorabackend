package com.bca.rentora.rentora.dtos.esewa;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EsewaResponse {
    private Double amount;
    private String transactionId;
    private String productCode;
    private Double totalAmount;
    private String successUrl;
    private String failureUrl;
    private String signature;
}
