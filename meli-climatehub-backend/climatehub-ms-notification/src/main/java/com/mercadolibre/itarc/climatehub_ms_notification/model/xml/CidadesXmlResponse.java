package com.mercadolibre.itarc.climatehub_ms_notification.model.xml;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "cidades")
@Setter
public class CidadesXmlResponse {
    private List<CidadeXml> cidades = new ArrayList<>();

    @XmlElement(name = "cidade")
    public List<CidadeXml> getCidades() {
        return cidades;
    }
}
