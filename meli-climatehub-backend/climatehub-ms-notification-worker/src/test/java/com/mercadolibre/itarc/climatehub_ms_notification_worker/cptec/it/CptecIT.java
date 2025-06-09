package com.mercadolibre.itarc.climatehub_ms_notification_worker.cptec.it;

import com.mercadolibre.itarc.climatehub_ms_notification_worker.cptec.config.TestRedisConfiguration;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.model.redis.CityCache;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.model.redis.OndasCache;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.model.redis.PrevisaoCache;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.service.CptecService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                CptecIT.TestConfig.class,
                TestRedisConfiguration.class,
                com.mercadolibre.itarc.climatehub_ms_notification_worker.service.impl.CptecServiceImpl.class
        }
)
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
@ActiveProfiles("test")
*/
public class CptecIT {
/*
    private static final String BASE_URL = "http://servicos.cptec.inpe.br/XML";
    private static final Logger logger = LoggerFactory.getLogger(CptecIT.class);

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames()
                .forEach(cacheName -> cacheManager.getCache(cacheName).clear());

        // Verifica se a API está respondendo
        try {
            String response = restTemplate.getForObject(
                    BASE_URL + "/listaCidades?city={cityName}",
                    String.class,
                    "Santos"
            );

            logger.info("API do CPTEC está respondendo. Resposta para 'Santos': {}", response);

            if (response == null || response.isBlank()) {
                logger.error("API do CPTEC retornou resposta vazia");
                throw new RuntimeException("API do CPTEC não está respondendo corretamente");
            }
        } catch (Exception e) {
            logger.error("Erro ao verificar API do CPTEC", e);
            throw new RuntimeException("API do CPTEC não está acessível", e);
        }
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
            return restTemplateBuilder
                    .setConnectTimeout(Duration.ofSeconds(10))
                    .setReadTimeout(Duration.ofSeconds(10))
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
                    .defaultHeader(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.name())
                    .build();
        }
    }

    @Autowired
    private CptecService cptecService;

    @Test
    @DisplayName("Deve retornar cidade quando encontrada na API do CPTEC")
    void deveRetornarCidadeQuandoEncontrada() {
        // Arrange
        String cityName = "Santos";
        String uf = "SP";

        // Act
        CityCache result = cptecService.getCityId(cityName, uf);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getCityId());
        assertEquals("SP", result.getUf());
    }

    @Test
    @DisplayName("Deve retornar null quando cidade não existe na API do CPTEC")
    void deveRetornarNullQuandoCidadeNaoExiste() {
        // Arrange
        String cityName = "Cidade Que Não Existe";
        String uf = "XX";

        // Act
        CityCache result = cptecService.getCityId(cityName, uf);

        // Assert
        assertNull(result);
    }

    @Test
    @DisplayName("Deve retornar cidade correta quando existem múltiplas cidades com mesmo nome")
    void deveRetornarCidadeCorretaQuandoExistemMultiplasCidades() {
        // Arrange
        String cityName = "Santos";
        String uf = "SP";

        // Act
        CityCache result = cptecService.getCityId(cityName, uf);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getCityId());
        assertEquals("SP", result.getUf());
    }

    @Test
    @DisplayName("Deve retornar cidade quando o nome tem acentuação")
    void deveRetornarCidadeQuandoNomeTemAcentuacao() {
        // Arrange
        String cityName = "São José dos Campos";  // Cidade com mais acentuação
        String uf = "SP";

        logger.info("Testando busca de cidade com acentuação: {} - {}", cityName, uf);

        // Act
        CityCache result = cptecService.getCityId(cityName, uf);

        // Assert
        assertNotNull(result, "O resultado não deveria ser null");
        assertNotNull(result.getCityId(), "O ID da cidade não deveria ser null");
        assertEquals("SP", result.getUf(), "O UF deveria ser SP");
    }

    @Test
    @DisplayName("Deve retornar a mesma cidade independente de maiúsculas/minúsculas")
    void deveRetornarCidadeIndependenteDeCaseSensitive() {
        // Arrange
        String cityNameMinusculo = "santos";
        String cityNameMaiusculo = "SANTOS";
        String uf = "SP";

        // Act
        CityCache resultMinusculo = cptecService.getCityId(cityNameMinusculo, uf);
        CityCache resultMaiusculo = cptecService.getCityId(cityNameMaiusculo, uf);

        // Assert
        assertNotNull(resultMinusculo);
        assertNotNull(resultMaiusculo);
        assertEquals(resultMinusculo.getCityId(), resultMaiusculo.getCityId(),
                "Os IDs das cidades deveriam ser iguais independente de maiúsculas/minúsculas");
    }

    @Test
    @DisplayName("Deve retornar cidade mesmo com espaços em branco extras")
    void deveRetornarCidadeComEspacosEmBranco() {
        // Arrange
        String cityName = "  São Paulo  ";
        String uf = "SP";

        // Act
        CityCache result = cptecService.getCityId(cityName, uf);

        // Assert
        assertNotNull(result, "Deveria encontrar a cidade mesmo com espaços em branco extras");
        assertEquals("SP", result.getUf());
    }

    @Test
    @DisplayName("Deve usar cache na segunda chamada")
    void deveUsarCacheNaSegundaChamada() {
        // Arrange
        String cityName = "Campinas";
        String uf = "SP";

        // Act - Primeira chamada
        CityCache primeiroResultado = cptecService.getCityId(cityName, uf);

        // Limpa o cache para garantir um cache limpo
        cacheManager.getCache("cityCache").clear();

        // Act - Segunda chamada
        CityCache segundoResultado = cptecService.getCityId(cityName, uf);

        // Assert
        assertNotNull(primeiroResultado);
        assertNotNull(segundoResultado);
        assertEquals(primeiroResultado.getCityId(), segundoResultado.getCityId(),
                "Os resultados deveriam ser iguais mesmo após limpar o cache");
    }

    @Test
    @DisplayName("Deve retornar cidade com hífen no nome")
    void deveRetornarCidadeComHifen() {
        // Arrange
        String cityName = "Boa Vista do Norte";  // Exemplo de cidade com hífen
        String uf = "RR";

        // Act
        CityCache result = cptecService.getCityId(cityName, uf);

        // Assert
        assertNotNull(result, "Deveria encontrar a cidade com hífen");
        assertEquals("RR", result.getUf());
    }

    @Test
    @DisplayName("Deve retornar cidade com nome composto")
    void deveRetornarCidadeComNomeComposto() {
        // Arrange
        String cityName = "Boa Vista";  // Capital de Roraima
        String uf = "RR";

        // Act
        CityCache result = cptecService.getCityId(cityName, uf);

        // Assert
        assertNotNull(result, "Deveria encontrar a cidade com nome composto");
        assertEquals("RR", result.getUf());
    }

    @Test
    @DisplayName("Deve retornar cidade com caracteres especiais")
    void deveRetornarCidadeComCaracteresEspeciais() {
        // Arrange
        String cityName = "São Paulo";  // Cidade com caractere especial
        String uf = "SP";

        logger.info("Testando busca de cidade com caracteres especiais: {} - {}", cityName, uf);

        // Act
        CityCache result = cptecService.getCityId(cityName, uf);

        // Assert
        assertNotNull(result, "Deveria encontrar a cidade com caracteres especiais");
        assertEquals("SP", result.getUf());
    }

    @Test
    @DisplayName("Deve retornar previsão do tempo para cidade válida")
    void deveRetornarPrevisaoParaCidadeValida() {
        // Arrange
        Integer cityId = 244; // São Paulo

        // Act
        PrevisaoCache result = cptecService.getPrevisao(cityId);

        // Assert
        assertNotNull(result, "A previsão não deveria ser null");
        assertNotNull(result.getPrevisoes(), "A lista de previsões não deveria ser null");
        assertFalse(result.getPrevisoes().isEmpty(), "A lista de previsões não deveria estar vazia");

        // Verifica cada previsão
        result.getPrevisoes().forEach(previsao -> {
            assertNotNull(previsao.getDia(), "O dia não deveria ser null");
            assertNotNull(previsao.getTempo(), "O tempo não deveria ser null");
            assertNotNull(previsao.getMaxima(), "A temperatura máxima não deveria ser null");
            assertNotNull(previsao.getMinima(), "A temperatura mínima não deveria ser null");
            assertNotNull(previsao.getIuv(), "O IUV não deveria ser null");
        });
    }

    @Test
    @DisplayName("Deve usar cache na segunda chamada de previsão")
    void deveUsarCacheNaSegundaChamadaDePrevisao() {
        // Arrange
        Integer cityId = 244; // São Paulo

        // Act - Primeira chamada
        PrevisaoCache primeiroResultado = cptecService.getPrevisao(cityId);

        // Limpa o cache para garantir um cache limpo
        cacheManager.getCache("previsaoCache").clear();

        // Act - Segunda chamada
        PrevisaoCache segundoResultado = cptecService.getPrevisao(cityId);

        // Assert
        assertNotNull(primeiroResultado, "O primeiro resultado não deveria ser null");
        assertNotNull(segundoResultado, "O segundo resultado não deveria ser null");
        assertEquals(
                primeiroResultado.getPrevisoes().size(),
                segundoResultado.getPrevisoes().size(),
                "O número de previsões deveria ser igual"
        );
    }

    @Test
    @DisplayName("Deve retornar previsão com temperaturas válidas")
    void deveRetornarPrevisaoComTemperaturasValidas() {
        // Arrange
        Integer cityId = 244; // São Paulo

        // Act
        PrevisaoCache result = cptecService.getPrevisao(cityId);

        // Assert
        assertNotNull(result, "A previsão não deveria ser null");
        result.getPrevisoes().forEach(previsao -> {
            assertTrue(previsao.getMaxima() >= -50 && previsao.getMaxima() <= 50,
                    "A temperatura máxima deveria estar entre -50 e 50°C");
            assertTrue(previsao.getMinima() >= -50 && previsao.getMinima() <= 50,
                    "A temperatura mínima deveria estar entre -50 e 50°C");
            assertTrue(previsao.getMaxima() >= previsao.getMinima(),
                    "A temperatura máxima deveria ser maior ou igual à mínima");
        });
    }

    @Test
    @DisplayName("Deve retornar previsão com datas válidas")
    void deveRetornarPrevisaoComDatasValidas() {
        // Arrange
        Integer cityId = 244; // São Paulo

        // Act
        PrevisaoCache result = cptecService.getPrevisao(cityId);

        // Assert
        assertNotNull(result, "A previsão não deveria ser null");
        assertNotNull(result.getAtualizacao(), "A data de atualização não deveria ser null");
        assertTrue(result.getAtualizacao().matches("\\d{4}-\\d{2}-\\d{2}"),
                "A data de atualização deveria estar no formato YYYY-MM-DD");

        result.getPrevisoes().forEach(previsao -> {
            assertNotNull(previsao.getDia(), "O dia da previsão não deveria ser null");
            assertTrue(previsao.getDia().matches("\\d{4}-\\d{2}-\\d{2}"),
                    "A data da previsão deveria estar no formato YYYY-MM-DD");
        });
    }

    @Test
    @DisplayName("Deve retornar previsão de ondas para cidade litorânea")
    void deveRetornarPrevisaoOndasParaCidadeLitoranea() {
        // Arrange
        Integer cityId = 241; // Rio de Janeiro
        Integer dia = 0; // Hoje

        // Act
        OndasCache result = cptecService.getPrevisaoOndas(cityId, dia);

        // Assert
        assertNotNull(result, "A previsão de ondas não deveria ser null");
        assertNotNull(result.getUf(), "O UF não deveria ser null");
        assertEquals("RJ", result.getUf(), "O UF deveria ser RJ");
        assertNotNull(result.getAtualizacao(), "A data de atualização não deveria ser null");

        // Verifica períodos do dia
        assertNotNull(result.getManha(), "A previsão da manhã não deveria ser null");
        assertNotNull(result.getTarde(), "A previsão da tarde não deveria ser null");
        assertNotNull(result.getNoite(), "A previsão da noite não deveria ser null");

        // Verifica dados da manhã
        OndasCache.PrevisaoPeriodo manha = result.getManha();
        assertNotNull(manha.getDia(), "O dia da manhã não deveria ser null");
        assertNotNull(manha.getAgitacao(), "A agitação da manhã não deveria ser null");
        assertTrue(manha.getAltura() > 0, "A altura das ondas deveria ser maior que 0");
        assertNotNull(manha.getDirecao(), "A direção das ondas não deveria ser null");
        assertNotNull(manha.getVentoDir(), "A direção do vento não deveria ser null");
    }

    @Test
    @DisplayName("Deve usar cache na segunda chamada de previsão de ondas")
    void deveUsarCacheNaSegundaChamadaDeOndas() {
        // Arrange
        Integer cityId = 241; // Rio de Janeiro
        Integer dia = 0; // Hoje

        // Act - Primeira chamada
        OndasCache primeiroResultado = cptecService.getPrevisaoOndas(cityId, dia);

        // Limpa o cache para garantir um cache limpo
        cacheManager.getCache("ondasCache").clear();

        // Act - Segunda chamada
        OndasCache segundoResultado = cptecService.getPrevisaoOndas(cityId, dia);

        // Assert
        assertNotNull(primeiroResultado, "O primeiro resultado não deveria ser null");
        assertNotNull(segundoResultado, "O segundo resultado não deveria ser null");
        assertEquals(
                primeiroResultado.getManha().getAltura(),
                segundoResultado.getManha().getAltura(),
                "A altura das ondas deveria ser igual nas duas chamadas"
        );
    }

    @Test
    @DisplayName("Deve validar parâmetro dia na previsão de ondas")
    void deveValidarParametroDiaNaPrevisaoOndas() {
        // Arrange
        Integer cityId = 241; // Rio de Janeiro
        Integer diaInvalido = 3; // Dia inválido

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> cptecService.getPrevisaoOndas(cityId, diaInvalido),
                "Deveria lançar IllegalArgumentException para dia inválido"
        );

        assertTrue(
                exception.getMessage().contains("deve estar entre 0 e 2"),
                "A mensagem de erro deveria indicar o intervalo válido"
        );
    }

    @Test
    @DisplayName("Deve retornar previsão de ondas com valores válidos")
    void deveRetornarPrevisaoOndasComValoresValidos() {
        // Arrange
        Integer cityId = 241; // Rio de Janeiro
        Integer dia = 0; // Hoje

        // Act
        OndasCache result = cptecService.getPrevisaoOndas(cityId, dia);

        // Assert
        assertNotNull(result, "A previsão de ondas não deveria ser null");

        // Verifica formato da data de atualização
        assertTrue(result.getAtualizacao().matches("\\d{2}-\\d{2}-\\d{4}"),
                "A data de atualização deveria estar no formato DD-MM-YYYY");

        // Verifica valores da manhã
        OndasCache.PrevisaoPeriodo manha = result.getManha();
        assertTrue(manha.getDia().matches("\\d{2}-\\d{2}-\\d{4} \\d{2}h Z"),
                "O dia/hora deveria estar no formato DD-MM-YYYY HHh Z");
        assertTrue(List.of("Fraco", "Moderado", "Forte").contains(manha.getAgitacao()),
                "A agitação deveria ser Fraco, Moderado ou Forte");
        assertTrue(manha.getAltura() >= 0 && manha.getAltura() <= 20,
                "A altura das ondas deveria estar entre 0 e 20 metros");
        assertTrue(manha.getVento() >= 0,
                "A velocidade do vento não deveria ser negativa");
    }
    */
}
