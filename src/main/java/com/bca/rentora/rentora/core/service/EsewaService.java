package com.bca.rentora.rentora.core.service;

import org.apache.hc.client5.http.utils.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;


@Component
public class EsewaService {
    @Value("${esewa.secret-key}")
    private String secretKey;

    public String generateEsewaSignature(String data){
        try{
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secretKeySpec);
//            hashing
            return Base64.encodeBase64String(sha256_HMAC.doFinal(data.getBytes()));
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public String createSignature(BigDecimal total_amount, String transaction_uuid , String product_code){
        String total = total_amount.toString();
        String data = String.format("total_amount=%s,transaction_uuid=%s,product_code=%s", total, transaction_uuid, product_code);
        return generateEsewaSignature(data);
    }

    public String createResponseSignature(String transactionCode,String status,String totalAmount,String transactionUuid, String productCode,String signedFieldNames) {
        String data = String.format("transaction_code=%s,status=%s,total_amount=%s,transaction_uuid=%s,product_code=%s,signed_field_names=%s",
                transactionCode, status, totalAmount, transactionUuid, productCode,signedFieldNames);

        return generateEsewaSignature(data);
    }


}
