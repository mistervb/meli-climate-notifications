package com.mercadolibre.itarc.climatehub_api_gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.mercadolibre.itarc.climatehub_api_gateway.util.JwtUtil;

import reactor.core.publisher.Mono;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

    private final JwtUtil jwtUtil;

    public AuthenticationFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            
            logger.debug("Verificando autenticação para o path: {}", path);

            // Rotas liberadas - não precisam de token
            if (path.startsWith("/user/register") || path.startsWith("/user/login") || path.startsWith("/actuator")) {
                logger.debug("Rota pública acessada: {}", path);
                return chain.filter(exchange);
            }

            // Validação do token para as demais rotas
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                logger.error("Header de autorização ausente para o path: {}", path);
                return onError(exchange, "Missing Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.error("Header de autorização inválido: {}", authHeader);
                return onError(exchange, "Invalid Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);
            logger.debug("Validando token para o path: {}", path);

            try {
                if (!jwtUtil.isTokenValid(token)) {
                    logger.error("Token inválido para o path: {}", path);
                    return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
                }

                String username = jwtUtil.extractUsername(token);
                logger.debug("Token válido para o usuário: {} no path: {}", username, path);

                // Adiciona o email do usuário no header para o microserviço
                ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Email", username)
                    .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            } catch (Exception e) {
                logger.error("Erro ao validar token para o path: {}", path, e);
                return onError(exchange, "Error validating token: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        return response.writeWith(Mono.just(response.bufferFactory()
            .wrap(err.getBytes())));
    }

    public static class Config {
        // empty class as configuration holder
    }
}

