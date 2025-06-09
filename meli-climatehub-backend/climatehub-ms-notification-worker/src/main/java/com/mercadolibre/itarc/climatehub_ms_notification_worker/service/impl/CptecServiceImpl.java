package com.mercadolibre.itarc.climatehub_ms_notification_worker.service.impl;

import com.mercadolibre.itarc.climatehub_ms_notification_worker.model.redis.CityCache;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.model.redis.OndasCache;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.model.redis.PrevisaoCache;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.model.xml.CidadesXmlResponse;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.model.xml.OndasXmlResponse;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.model.xml.PrevisaoXmlResponse;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.service.CptecService;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CptecServiceImpl implements CptecService {
    private final RestTemplate restTemplate;
    private static final String BASE_URL = "http://servicos.cptec.inpe.br/XML";

    public CptecServiceImpl(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(60))
                .setReadTimeout(Duration.ofSeconds(60))
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
    @Cacheable(value = "previsaoCache", key = "#cityId", unless = "#result == null")
    public PrevisaoCache getPrevisao(Integer cityId) {
        String xml = restTemplate.getForObject(
                BASE_URL + "/cidade/{cityId}/previsao.xml",
                String.class,
                cityId
        );

        try {
            JAXBContext context = JAXBContext.newInstance(PrevisaoXmlResponse.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            StringReader reader = new StringReader(xml);
            PrevisaoXmlResponse response = (PrevisaoXmlResponse) unmarshaller.unmarshal(reader);

            PrevisaoCache previsaoCache = new PrevisaoCache();
            previsaoCache.setNome(response.getNome());
            previsaoCache.setUf(response.getUf());
            previsaoCache.setAtualizacao(response.getAtualizacao());

            List<PrevisaoCache.PrevisaoDia> previsoes = response.getPrevisoes().stream()
                    .map(p -> {
                        PrevisaoCache.PrevisaoDia previsaoDia = new PrevisaoCache.PrevisaoDia();
                        previsaoDia.setDia(p.getDia());
                        previsaoDia.setTempo(p.getTempo());
                        previsaoDia.setMaxima(p.getMaxima());
                        previsaoDia.setMinima(p.getMinima());
                        previsaoDia.setIuv(p.getIuv());
                        return previsaoDia;
                    })
                    .collect(Collectors.toList());

            previsaoCache.setPrevisoes(previsoes);

            return previsaoCache;

        } catch (Exception e) {
            throw new RuntimeException("Erro ao parsear resposta de previsão do CPTEC/INPE", e);
        }
    }

    @Override
    @Cacheable(value = "ondasCache", key = "#cityId + '-' + #dia", unless = "#result == null")
    public OndasCache getPrevisaoOndas(Integer cityId, Integer dia) {
        if (dia < 0 || dia > 2) {
            throw new IllegalArgumentException("O parâmetro 'dia' deve estar entre 0 e 2");
        }

        String xml = restTemplate.getForObject(
                BASE_URL + "/cidade/{cityId}/dia/{dia}/ondas.xml",
                String.class,
                cityId,
                dia
        );

        try {
            JAXBContext context = JAXBContext.newInstance(OndasXmlResponse.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            StringReader reader = new StringReader(xml);
            OndasXmlResponse response = (OndasXmlResponse) unmarshaller.unmarshal(reader);

            OndasCache ondasCache = new OndasCache();
            ondasCache.setNome(response.getNome());
            ondasCache.setUf(response.getUf());
            ondasCache.setAtualizacao(response.getAtualizacao());

            // Mapeia previsão da manhã
            if (response.getManha() != null) {
                OndasCache.PrevisaoPeriodo manha = new OndasCache.PrevisaoPeriodo();
                manha.setDia(response.getManha().getDia());
                manha.setAgitacao(response.getManha().getAgitacao());
                manha.setAltura(response.getManha().getAltura());
                manha.setDirecao(response.getManha().getDirecao());
                manha.setVento(response.getManha().getVento());
                manha.setVentoDir(response.getManha().getVentoDir());
                ondasCache.setManha(manha);
            }

            // Mapeia previsão da tarde
            if (response.getTarde() != null) {
                OndasCache.PrevisaoPeriodo tarde = new OndasCache.PrevisaoPeriodo();
                tarde.setDia(response.getTarde().getDia());
                tarde.setAgitacao(response.getTarde().getAgitacao());
                tarde.setAltura(response.getTarde().getAltura());
                tarde.setDirecao(response.getTarde().getDirecao());
                tarde.setVento(response.getTarde().getVento());
                tarde.setVentoDir(response.getTarde().getVentoDir());
                ondasCache.setTarde(tarde);
            }

            // Mapeia previsão da noite
            if (response.getNoite() != null) {
                OndasCache.PrevisaoPeriodo noite = new OndasCache.PrevisaoPeriodo();
                noite.setDia(response.getNoite().getDia());
                noite.setAgitacao(response.getNoite().getAgitacao());
                noite.setAltura(response.getNoite().getAltura());
                noite.setDirecao(response.getNoite().getDirecao());
                noite.setVento(response.getNoite().getVento());
                noite.setVentoDir(response.getNoite().getVentoDir());
                ondasCache.setNoite(noite);
            }

            return ondasCache;

        } catch (Exception e) {
            throw new RuntimeException("Erro ao parsear resposta de ondas do CPTEC/INPE", e);
        }
    }


}
