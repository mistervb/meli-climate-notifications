package com.mercadolibre.itarc.climatehub_ms_notification_worker.model.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@XmlRootElement(name = "cidade")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class OndasXmlResponse {
    
    @XmlElement(name = "nome")
    private String nome;
    
    @XmlElement(name = "uf")
    private String uf;
    
    @XmlElement(name = "atualizacao")
    private String atualizacao;
    
    @XmlElement(name = "manha")
    private PrevisaoOnda manha;
    
    @XmlElement(name = "tarde")
    private PrevisaoOnda tarde;
    
    @XmlElement(name = "noite")
    private PrevisaoOnda noite;

    @XmlAccessorType(XmlAccessType.FIELD)
    @Data
    public static class PrevisaoOnda {
        @XmlElement(name = "dia")
        private String dia;
        
        @XmlElement(name = "agitacao")
        private String agitacao;
        
        @XmlElement(name = "altura")
        private Double altura;
        
        @XmlElement(name = "direcao")
        private String direcao;
        
        @XmlElement(name = "vento")
        private Double vento;
        
        @XmlElement(name = "vento_dir")
        private String ventoDir;
    }
} 