package com.mercadolibre.itarc.climatehub_ms_notification_worker.model.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

import java.util.List;

@XmlRootElement(name = "cidade")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class PrevisaoXmlResponse {
    
    @XmlElement(name = "nome")
    private String nome;
    
    @XmlElement(name = "uf")
    private String uf;
    
    @XmlElement(name = "atualizacao")
    private String atualizacao;
    
    @XmlElement(name = "previsao")
    private List<Previsao> previsoes;

    @XmlAccessorType(XmlAccessType.FIELD)
    @Data
    public static class Previsao {
        @XmlElement(name = "dia")
        private String dia;
        
        @XmlElement(name = "tempo")
        private String tempo;
        
        @XmlElement(name = "maxima")
        private Integer maxima;
        
        @XmlElement(name = "minima")
        private Integer minima;
        
        @XmlElement(name = "iuv")
        private Double iuv;
    }
} 