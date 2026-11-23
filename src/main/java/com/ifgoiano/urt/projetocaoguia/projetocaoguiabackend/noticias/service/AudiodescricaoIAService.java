package com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.noticias.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class AudiodescricaoIAService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @SuppressWarnings("unchecked")
    public String gerarAudiodescricao(String imagemUrl, String contextoTexto) {
        if (imagemUrl == null || imagemUrl.isBlank()) {
            return gerarFallbackAcessivel(contextoTexto);
        }

        try {
            byte[] imageBytes = baixarImagem(imagemUrl);
            if (imageBytes == null || imageBytes.length == 0) {
                return gerarFallbackAcessivel(contextoTexto);
            }

            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            String prompt = "Você é um especialista em audiodescrição profissional para pessoas cegas ou com baixa visão (norma ABNT NBR 16452). "
                    + "Analise EXCLUSIVAMENTE o que está visível nesta imagem, com rigor e precisão factual. "
                    + "Regras obrigatórias: "
                    + "1) Conte exatamente quantas pessoas aparecem na imagem (não estime nem reduza o número). "
                    + "2) Descreva o cenário, as posições das pessoas, expressões, roupas principais, cães-guia (se houver) e qualquer texto ou faixa visível na imagem. "
                    + "3) Nunca invente elementos que não estejam claramente visíveis. "
                    + "4) Escreva em português do Brasil, em um parágrafo objetivo e claro de 3 a 5 frases. "
                    + "Contexto apenas de apoio (não substitui o que está na foto): " + (contextoTexto != null ? contextoTexto : "");

            var inlineData = Map.of("mime_type", "image/jpeg", "data", base64Image);
            var partTexto = Map.of("text", prompt);
            var partImagem = Map.of("inline_data", inlineData);
            var contentObj = Map.of("parts", List.of(partTexto, partImagem));
            var body = Map.of("contents", List.of(contentObj));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            var entity = new HttpEntity<>(body, headers);

            for (String modelo : List.of("gemini-3-flash-preview", "gemini-2.5-pro")) {
                for (int tentativa = 1; tentativa <= 3; tentativa++) {
                    try {
                        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                                + modelo + ":generateContent?key=" + apiKey;

                        var response = restTemplate.postForEntity(url, entity, Map.class);
                        Map responseBody = response.getBody();

                        if (responseBody != null && responseBody.containsKey("candidates")) {
                            List candidates = (List) responseBody.get("candidates");
                            if (!candidates.isEmpty()) {
                                Map firstCandidate = (Map) candidates.get(0);
                                Map content = (Map) firstCandidate.get("content");
                                List parts = (List) content.get("parts");
                                if (!parts.isEmpty()) {
                                    Map firstPart = (Map) parts.get(0);
                                    if (firstPart.containsKey("text")) {
                                        String textoGerado = firstPart.get("text").toString().trim();
                                        if (!textoGerado.isBlank()) {
                                            Thread.sleep(4500);
                                            return textoGerado;
                                        }
                                    }
                                }
                            }
                        }
                        break;
                    } catch (HttpClientErrorException.TooManyRequests e) {
                        System.err.println("[Audiodescrição IA] Limite por minuto (429) no modelo " + modelo + ", aguardando 15s...");
                        Thread.sleep(15000);
                    } catch (Exception e) {
                        System.err.println("[Audiodescrição IA] Erro no modelo " + modelo + ": " + e.getMessage());
                        break;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return gerarFallbackAcessivel(contextoTexto);
    }

    private String gerarFallbackAcessivel(String contextoTexto) {
        if (contextoTexto != null && !contextoTexto.isBlank()) {
            return "Registro visual institucional referente à publicação do Programa Cão-Guia do IF Goiano – Campus Urutaí: "
                    + contextoTexto.trim() + ".";
        }
        return "Registro visual institucional do Programa Cão-Guia do Instituto Federal Goiano – Campus Urutaí.";
    }

    private byte[] baixarImagem(String imagemUrl) {
        try {
            TrustManager[] trustAll = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    }
            };
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAll, new SecureRandom());

            HttpClient client = HttpClient.newBuilder()
                    .sslContext(sc)
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(imagemUrl))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Referer", "https://www.instagram.com/")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            var response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 200) {
                return response.body();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}