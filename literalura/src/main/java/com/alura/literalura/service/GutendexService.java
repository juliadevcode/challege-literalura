package com.alura.literalura.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
public class GutendexService {

    private final RestTemplate rest;
    private final ObjectMapper mapper;

    public GutendexService() {
        this.rest = new RestTemplate();
        this.mapper = new ObjectMapper();
    }

    /**
     * Busca o primeiro livro retornado pela API Gutendex para o título fornecido.
     * Retorna um JsonNode contendo os campos mais relevantes (title, authors[], languages[], download_count).
     */
    public Optional<JsonNode> buscarLivroPorTitulo(String titulo) {
        try {
            String url = "https://gutendex.com/books?search=" + java.net.URLEncoder.encode(titulo, "UTF-8");
            String json = rest.getForObject(url, String.class);
            JsonNode root = mapper.readTree(json);
            JsonNode results = root.path("results");
            if (results.isArray() && results.size() > 0) {
                JsonNode primeiro = results.get(0);
                return Optional.of(primeiro);
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
