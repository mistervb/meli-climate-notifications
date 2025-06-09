package com.mercadolibre.itarc.climatehub_ms_notification.service.impl;

import com.mercadolibre.itarc.climatehub_ms_notification.model.redis.CityCache;
import com.mercadolibre.itarc.climatehub_ms_notification.model.xml.CidadesXmlResponse;
import com.mercadolibre.itarc.climatehub_ms_notification.service.CptecService;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;

@Service
public class CptecServiceImpl implements CptecService {
    private final RestTemplate restTemplate;
    private static final String BASE_URL = "http://servicos.cptec.inpe.br/XML";

    public CptecServiceImpl(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(30))
                .setReadTimeout(Duration.ofSeconds(30))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.name())
                .build();
    }

    private String normalizarNome(String nome) {
        if (nome == null) {
            return "";
        }

        // Remove espaços extras e normaliza espaços múltiplos
        String normalizado = nome.trim().replaceAll("\\s+", " ");

        // Converte para minúsculo
        normalizado = normalizado.toLowerCase();

        // Normaliza acentos e caracteres especiais, mantendo hífen e apóstrofo
        normalizado = Normalizer.normalize(normalizado, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "") // Remove acentos
                .replaceAll("[^a-z0-9\\s\\-']", ""); // Mantém apenas letras, números, espaços, hífen e apóstrofo

        // Para busca na API do CPTEC, vamos usar apenas a primeira palavra
        // já que a API tem limitações com nomes compostos
        String[] palavras = normalizado.split("\\s+");

        return palavras[0];
    }

    @Override
    @Cacheable(value = "cityCache", key = "#cityName.toLowerCase() + '-' + #uf.toUpperCase()", unless = "#result == null")
    public CityCache getCityId(String cityName, String uf) {
        String normalizedCityName = normalizarNome(cityName);

        String xml = restTemplate.getForObject(
                BASE_URL + "/listaCidades?city={cityName}",
                String.class,
                normalizedCityName
        );

        try {
            JAXBContext context = JAXBContext.newInstance(CidadesXmlResponse.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            StringReader reader = new StringReader(xml);
            CidadesXmlResponse response = (CidadesXmlResponse) unmarshaller.unmarshal(reader);

            return response.getCidades().stream()
                    .filter(c -> {
                        String normalizedNome = normalizarNome(c.getNome());
                        return normalizedNome.equalsIgnoreCase(normalizedCityName) &&
                                c.getUf().equalsIgnoreCase(uf);
                    })
                    .findFirst()
                    .map(c -> new CityCache(c.getId(), c.getUf()))
                    .orElse(null);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao parsear resposta do CPTEC/INPE", e);
        }
    }
}
