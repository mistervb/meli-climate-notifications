package com.mercadolibre.itarc.climatehub_ms_notification_worker.model.xml;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

@XmlRootElement(name = "cidade")
@AllArgsConstructor
@NoArgsConstructor
@Setter
public class CidadeXml {
    private String nome = "";
    private String uf = "";
    private Integer id = 0;

    @XmlElement(name = "nome")
    public String getNome() {
        return nome;
    }

    @XmlElement(name = "uf")
    public String getUf() {
        return uf;
    }

    @XmlElement(name = "id")
    public Integer getId() {
        return id;
    }
}
