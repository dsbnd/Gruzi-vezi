package com.rzd.dispatcher.model.dto.request;

import lombok.Data;

@Data
public class CreateAccountRequest {
    private String inn;
    private String companyName;
    private String bik;
    private String bankName;
    private Boolean isMain = false;
}