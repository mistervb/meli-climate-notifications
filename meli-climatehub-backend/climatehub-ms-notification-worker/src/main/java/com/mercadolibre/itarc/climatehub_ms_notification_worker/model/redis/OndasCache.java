package com.mercadolibre.itarc.climatehub_ms_notification_worker.model.redis;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OndasCache implements Serializable {
    private static final long serialVersionUID = 1L;

    private String nome;
    private String uf;
    private String atualizacao;
    private PrevisaoPeriodo manha;
    private PrevisaoPeriodo tarde;
    private PrevisaoPeriodo noite;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PrevisaoPeriodo implements Serializable {
        private static final long serialVersionUID = 1L;

        private String dia;
        private String agitacao;
        private Double altura;
        private String direcao;
        private Double vento;
        private String ventoDir;
    }
} 