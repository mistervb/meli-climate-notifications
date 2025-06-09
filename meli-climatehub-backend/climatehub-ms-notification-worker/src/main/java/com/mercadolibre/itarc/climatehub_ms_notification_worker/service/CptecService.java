package com.mercadolibre.itarc.climatehub_ms_notification_worker.service;

import com.mercadolibre.itarc.climatehub_ms_notification_worker.model.redis.CityCache;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.model.redis.OndasCache;
import com.mercadolibre.itarc.climatehub_ms_notification_worker.model.redis.PrevisaoCache;

public interface CptecService {
    /**
     * Busca a previsão do tempo para uma cidade específica
     * @param cityId ID da cidade no CPTEC
     * @return Previsão do tempo para os próximos dias
     */
    PrevisaoCache getPrevisao(Integer cityId);

    /**
     * Busca a previsão de ondas para uma cidade litorânea específica
     * @param cityId ID da cidade no CPTEC
     * @param dia Dia da previsão (0 = hoje, 1 = amanhã, 2 = depois de amanhã)
     * @return Previsão de ondas para o dia especificado
     * @throws IllegalArgumentException se o dia não estiver entre 0 e 2
     */
    OndasCache getPrevisaoOndas(Integer cityId, Integer dia);
}
