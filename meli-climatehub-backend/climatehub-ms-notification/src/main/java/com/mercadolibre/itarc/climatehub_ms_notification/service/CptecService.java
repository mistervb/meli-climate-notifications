package com.mercadolibre.itarc.climatehub_ms_notification.service;

import com.mercadolibre.itarc.climatehub_ms_notification.model.redis.CityCache;

public interface CptecService {
    CityCache getCityId(String cityName, String uf);
}
