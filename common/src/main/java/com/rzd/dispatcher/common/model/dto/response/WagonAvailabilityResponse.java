package com.rzd.dispatcher.common.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class WagonAvailabilityResponse {

    private UUID wagonId;                 
    private String wagonNumber;            
    private String wagonType;
    
    private Integer maxWeightKg;           
    private Integer maxVolumeM3;           
    private String currentStation;
    
    private Boolean isAvailable;           
    private String availabilityStatus;      

    private Integer distanceToStation;      
    private Integer estimatedArrivalHours;
    
    private Integer matchPercentage;        
    private String recommendation;
    
    private BigDecimal estimatedPrice;      
    private String priceUnit;
    
    private String ownerInfo;               
    private String lastMaintenanceDate;     
    private String maintenanceStatus;       
}