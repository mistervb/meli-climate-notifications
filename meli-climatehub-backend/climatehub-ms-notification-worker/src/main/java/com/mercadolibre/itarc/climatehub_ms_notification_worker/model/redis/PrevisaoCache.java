package com.mercadolibre.itarc.climatehub_ms_notification_worker.model.redis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PrevisaoCache implements Serializable {
    private static final long serialVersionUID = 1L;

    private String nome;
    private String uf;
    private String atualizacao;
    private List<PrevisaoDia> previsoes;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PrevisaoDia implements Serializable {
        private static final long serialVersionUID = 1L;

        private String dia;
        private String tempo;
        private Integer maxima;
        private Integer minima;
        private Double iuv;
    }
}