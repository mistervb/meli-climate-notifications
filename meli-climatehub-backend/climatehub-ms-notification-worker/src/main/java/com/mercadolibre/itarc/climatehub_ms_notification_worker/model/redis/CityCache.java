package com.mercadolibre.itarc.climatehub_ms_notification_worker.model.redis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CityCache implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Integer cityId;
    private String uf;
}
