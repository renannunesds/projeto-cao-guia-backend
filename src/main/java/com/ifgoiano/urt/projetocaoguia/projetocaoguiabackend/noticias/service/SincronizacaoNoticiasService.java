package com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.noticias.service;

import com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.noticias.model.*;
import com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.noticias.repository.NoticiaRepository;
import com.ifgoiano.urt.projetocaoguia.projetocaoguiabackend.usuarios.repository.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SincronizacaoNoticiasService {

    private final NoticiaRepository noticiaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AudiodescricaoIAService audiodescricaoIAService;
    private final TransactionTemplate transactionTemplate;

    private final AtomicBoolean sincronizandoPortal = new AtomicBoolean(false);

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36";

    private static final List<String> FONTES_IF = List.of(
            "https://www.ifgoiano.edu.br/home/index.php/busca.html?searchword=c%C3%A3o-guia&searchphrase=all&ordering=newest",
            "https://www.ifgoiano.edu.br/home/index.php/busca.html?searchword=c%C3%A3o-guia&searchphrase=all&ordering=newest&start=10",
            "https://www.ifgoiano.edu.br/home/index.php/busca.html?searchword=c%C3%A3o+guia&searchphrase=all&ordering=newest",
            "https://www.ifgoiano.edu.br/home/index.php/component/finder/search?q=c%C3%A3o-guia",
            "https://www.ifgoiano.edu.br/home/index.php/ultimas-noticias-urutai"
    );

    private static final List<String> FONTES_RSS_OFICIAL = List.of(
            "https://news.google.com/rss/search?q=site:ifgoiano.edu.br+(%22c%C3%A3o-guia%22+OR+%22c%C3%A3o+guia%22+OR+%22c%C3%A3es-guia%22+OR+%22c%C3%A3es+guia%22)+when:2y&hl=pt-BR&gl=BR&ceid=BR:pt-419",
            "https://www.bing.com/news/search?q=site%3Aifgoiano.edu.br+%22c%C3%A3o-guia%22&format=rss",
            "https://www.bing.com/news/search?q=site%3Aifgoiano.edu.br+%22c%C3%A3o+guia%22&format=rss"
    );

    private static final Map<String, Integer> MESES_PT = Map.ofEntries(
            Map.entry("jan", 1), Map.entry("janeiro", 1),
            Map.entry("fev", 2), Map.entry("fevereiro", 2),
            Map.entry("mar", 3), Map.entry("março", 3), Map.entry("marco", 3),
            Map.entry("abr", 4), Map.entry("abril", 4),
            Map.entry("mai", 5), Map.entry("maio", 5),
            Map.entry("jun", 6), Map.entry("junho", 6),
            Map.entry("jul", 7), Map.entry("julho", 7),
            Map.entry("ago", 8), Map.entry("agosto", 8),
            Map.entry("set", 9), Map.entry("setembro", 9),
            Map.entry("out", 10), Map.entry("outubro", 10),
            Map.entry("nov", 11), Map.entry("novembro", 11),
            Map.entry("dez", 12), Map.entry("dezembro", 12)
    );

    @Value("${noticias.expiracao.dias:365}")
    private int diasExpiracao;

    @Value("${noticias.expiracao.minimo-ativo:6}")
    private int minimoNoticiasAtivas;

    @Value("${noticias.limite-maximo-banco:24}")
    private int limiteMaximoBanco;

    private static class ItemColetadoIF {
        String titulo;
        String link;
        String resumo;
        String imagemUrl;
        LocalDate dataPublicacao;
    }

    @PostConstruct
    @Transactional
    public void limparRegistrosInvalidosAoIniciar() {
        List<Noticia> todas = noticiaRepository.findAll();
        for (Noticia n : todas) {
            String titulo = n.getTitulo() != null ? n.getTitulo().toLowerCase() : "";
            String conteudo = n.getConteudo() != null ? n.getConteudo().toLowerCase() : "";
            boolean isAgendaOuAntiga = titulo.startsWith("atendimento no campus")
                    || titulo.startsWith("participação em evento de entrega")
                    || titulo.startsWith("photo by ")
                    || titulo.contains("likes,")
                    || conteudo.contains("de 2019") || conteudo.contains("de 2021")
                    || conteudo.contains("de 2023");
            if (isAgendaOuAntiga) {
                noticiaRepository.delete(n);
            } else {
                boolean alterou = false;
                if (n.getStatus() == null) {
                    n.setStatus(StatusNoticia.PUBLICADO);
                    alterou = true;
                }
                String tituloLimpo = limparTexto(n.getTitulo());
                String conteudoLimpo = limparTexto(n.getConteudo());
                if (!tituloLimpo.equals(n.getTitulo()) || !conteudoLimpo.equals(n.getConteudo())) {
                    n.setTitulo(tituloLimpo);
                    n.setConteudo(conteudoLimpo);
                    n.setResumo(gerarResumo(conteudoLimpo));
                    alterou = true;
                }
                if (alterou) {
                    noticiaRepository.save(n);
                }
            }
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void sincronizarPortalAoSubirAplicacao() {
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(2000);
                varrerPortalIfGoiano();
            } catch (Exception e) {
                System.err.println("[IF Goiano] Erro na sincronização inicial: " + e.getMessage());
            }
        });
    }

    @Scheduled(cron = "0 0 */6 * * *")
    public void varrerPortalIfGoiano() {
        if (!sincronizandoPortal.compareAndSet(false, true)) {
            return;
        }

        try {
            LocalDate limite24Meses = LocalDate.now().minusMonths(24);
            Map<String, ItemColetadoIF> coletadas = new LinkedHashMap<>();

            coletarDoPortalIF(coletadas);
            coletarDoRssOficial(coletadas);

            List<ItemColetadoIF> ordenadas = new ArrayList<>(coletadas.values());
            ordenadas.sort(Comparator.comparing(
                    (ItemColetadoIF i) -> i.dataPublicacao,
                    Comparator.nullsLast(Comparator.reverseOrder())
            ));

            transactionTemplate.executeWithoutResult(status -> limparRegistrosInvalidosAoIniciar());

            Set<String> titulosVistos = new HashSet<>();
            int salvasCount = 0;

            for (ItemColetadoIF item : ordenadas) {
                if (item.dataPublicacao != null && item.dataPublicacao.isBefore(limite24Meses)) {
                    continue;
                }

                String tituloLimpo = limparTexto(limparTituloIF(item.titulo));
                if (tituloLimpo.length() > 240) {
                    tituloLimpo = tituloLimpo.substring(0, 237) + "...";
                }
                String chaveTitulo = tituloLimpo.toLowerCase().replaceAll("[^a-z0-9áéíóúâêôãõç]", "");
                if (chaveTitulo.isBlank() || titulosVistos.contains(chaveTitulo)) {
                    continue;
                }
                titulosVistos.add(chaveTitulo);

                String conteudoFinal = (item.resumo == null || item.resumo.isBlank()
                        || limparTituloIF(item.resumo).equalsIgnoreCase(tituloLimpo))
                        ? "Matéria publicada no portal oficial do Instituto Federal Goiano sobre o Programa Cão-Guia."
                        : limparTexto(item.resumo);

                String linkSeguro = item.link != null && item.link.length() > 250
                        ? item.link.substring(0, 250)
                        : item.link;

                String imgSegura = item.imagemUrl != null && item.imagemUrl.length() > 250
                        ? null
                        : item.imagemUrl;

                LocalDateTime dataPublicacao = (item.dataPublicacao != null ? item.dataPublicacao : LocalDate.now()).atTime(12, 0);
                final String tituloFinal = tituloLimpo;

                try {
                    Noticia salva = transactionTemplate.execute(status ->
                            sincronizarItem("IF_GOIANO", linkSeguro, tituloFinal, conteudoFinal, imgSegura, dataPublicacao, null)
                    );
                    if (salva != null) {
                        System.out.println("[IF Goiano] Commitada no SQL Server (ID=" + salva.getId() + "): " + salva.getPublicadoEm() + " | " + salva.getTitulo());
                        salvasCount++;
                    }
                } catch (Exception e) {
                    System.err.println("[IF Goiano] Erro ao salvar '" + tituloFinal + "': " + e.getMessage());
                }

                if (salvasCount >= 6) break;
            }

            System.out.println("[IF Goiano] Varredura concluída. Notícias gravadas: " + salvasCount);
            transactionTemplate.executeWithoutResult(status -> aplicarPoliticaRetencaoBanco());
        } finally {
            sincronizandoPortal.set(false);
        }
    }

    private void coletarDoPortalIF(Map<String, ItemColetadoIF> coletadas) {
        for (String urlFonte : FONTES_IF) {
            try {
                Document doc = conectarPortal(urlFonte).timeout(12000).followRedirects(true).get();

                for (Element a : doc.select("a[href]")) {
                    String href = limparUrl(a.attr("abs:href"));
                    String titulo = limparTituloIF(a.text());
                    if (titulo.length() < 10 || !isLinkValidoNoticiaIF(href) || coletadas.containsKey(href)) {
                        continue;
                    }

                    StringBuilder contexto = new StringBuilder(titulo);
                    if (a.parent() != null) {
                        contexto.append(" ").append(a.parent().text());
                        Element irmao = a.parent().nextElementSibling();
                        if (irmao != null) {
                            contexto.append(" ").append(irmao.text());
                        }
                    }
                    Element bloco = a.closest("li, .result-item, article, div.tileItem");
                    if (bloco != null) {
                        contexto.append(" ").append(bloco.text());
                    }

                    String textoContexto = contexto.toString();
                    if (contemTermoProjeto(textoContexto) || contemTermoProjeto(href)) {
                        String resumoPrevio = extrairResumoDoBloco(textoContexto, titulo);
                        ItemColetadoIF model = criarModeloBaseIF(titulo, href, resumoPrevio);
                        enriquecerComPaginaIF(model);
                        coletadas.put(model.link, model);
                    }
                }
            } catch (IOException e) {
                System.err.println("[IF Goiano] Falha ao acessar " + urlFonte + ": " + e.getMessage());
            }
        }
    }

    private void coletarDoRssOficial(Map<String, ItemColetadoIF> coletadas) {
        for (String urlRss : FONTES_RSS_OFICIAL) {
            try {
                Document xml = conectarPortal(urlRss)
                        .timeout(12000)
                        .parser(Parser.xmlParser())
                        .get();

                for (Element item : xml.select("item")) {
                    String titulo = limparTituloIF(textoTag(item, "title"));
                    String rawLink = textoTag(item, "link");
                    String linkReal = extrairUrlRealDoRss(rawLink);
                    String descricao = Jsoup.parse(textoTag(item, "description")).text().trim();

                    if (titulo.isBlank() || linkReal.isBlank()) continue;

                    Element sourceEl = item.selectFirst("source");
                    String sourceUrl = sourceEl != null ? sourceEl.attr("url").toLowerCase() : "";
                    String sourceNome = sourceEl != null ? sourceEl.text().toLowerCase() : "";

                    boolean ehPortalIF = linkReal.toLowerCase().contains("ifgoiano.edu.br")
                            || sourceUrl.contains("ifgoiano.edu.br")
                            || sourceNome.contains("instituto federal goiano")
                            || sourceNome.equals("if goiano");

                    if (!ehPortalIF) continue;
                    if (!contemTermoProjeto(titulo) && !contemTermoProjeto(descricao) && !contemTermoProjeto(linkReal)) {
                        continue;
                    }

                    ItemColetadoIF model = criarModeloBaseIF(titulo, linkReal, descricao);

                    String pubDate = textoTag(item, "pubDate");
                    if (!pubDate.isBlank()) {
                        try {
                            model.dataPublicacao = ZonedDateTime.parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME).toLocalDate();
                        } catch (Exception ignored) {
                        }
                    }

                    Element imgTag = item.selectFirst("News\\:Image, image, thumbnail");
                    if (imgTag != null && !imgTag.text().isBlank()) {
                        model.imagemUrl = imgTag.text().trim();
                    }

                    enriquecerComPaginaIF(model);
                    coletadas.putIfAbsent(model.link, model);
                }
            } catch (IOException e) {
                System.err.println("[IF Goiano] Aviso RSS: " + e.getMessage());
            }
        }
    }

    private void enriquecerComPaginaIF(ItemColetadoIF model) {
        try {
            String urlAlvo = model.link;

            if (!urlAlvo.toLowerCase().contains("ifgoiano.edu.br")) {
                String buscaTitulo = "https://www.ifgoiano.edu.br/home/index.php/component/finder/search?q="
                        + URLEncoder.encode(model.titulo, StandardCharsets.UTF_8);
                Document docBusca = conectarPortal(buscaTitulo).timeout(8000).get();

                for (Element a : docBusca.select("a[href]")) {
                    String cand = limparUrl(a.attr("abs:href"));
                    if (isLinkValidoNoticiaIF(cand) && contemTermoProjeto(a.text())) {
                        urlAlvo = cand;
                        break;
                    }
                }
            }

            if (!urlAlvo.toLowerCase().contains("ifgoiano.edu.br")) {
                return;
            }

            Document pagina = conectarPortal(urlAlvo).timeout(8000).followRedirects(true).get();
            model.link = limparUrl(pagina.location());

            Element corpo = pagina.selectFirst("div[property=rnews:articleBody], div[itemprop=articleBody], div.item-page, article, #content-section");

            if (corpo != null) {
                StringBuilder sb = new StringBuilder();
                for (Element p : corpo.select("p")) {
                    String t = p.text().trim();
                    String low = t.toLowerCase();
                    if (t.length() < 40 || low.startsWith("foto:") || low.startsWith("imagem:") || low.startsWith("publicado")) {
                        continue;
                    }
                    if (!sb.isEmpty()) sb.append(" ");
                    sb.append(t);
                    if (sb.length() >= 450) break;
                }
                if (!sb.isEmpty()) {
                    model.resumo = sb.toString();
                }
            }

            if (model.imagemUrl == null || model.imagemUrl.isBlank()) {
                if (corpo != null) {
                    for (Element img : corpo.select("img[src]")) {
                        String src = img.attr("abs:src");
                        String low = src.toLowerCase();
                        if (!src.isBlank() && !low.contains("icon") && !low.contains("logo") && !low.contains("spacer")
                                && !low.contains("social2s") && !low.contains("qrcode") && !low.contains("brasao")
                                && !low.endsWith(".svg")) {
                            model.imagemUrl = src;
                            break;
                        }
                    }
                }
                if (model.imagemUrl == null) {
                    Element ogImg = pagina.selectFirst("meta[property=og:image]");
                    if (ogImg != null && !ogImg.attr("abs:content").isBlank()) {
                        String ogUrl = ogImg.attr("abs:content");
                        if (!ogUrl.toLowerCase().contains("logo") && !ogUrl.toLowerCase().contains("brasao")) {
                            model.imagemUrl = ogUrl;
                        }
                    }
                }
            }

            LocalDate data = extrairDataPublicacaoIF(pagina, corpo);
            if (data != null) {
                model.dataPublicacao = data;
            }
        } catch (IOException ignored) {
        }
    }

    private ItemColetadoIF criarModeloBaseIF(String titulo, String link, String resumo) {
        ItemColetadoIF model = new ItemColetadoIF();
        model.titulo = limparTituloIF(titulo);
        model.link = link;
        model.resumo = resumo != null && !resumo.isBlank() ? resumo : "";
        model.dataPublicacao = extrairDataPortugues(resumo);
        return model;
    }

    private String limparTituloIF(String titulo) {
        if (titulo == null) return "";
        return titulo
                .replaceAll("(?i)\\s*[-|–]\\s*Instituto Federal Goiano.*$", "")
                .replaceAll("(?i)\\s*[-|–]\\s*IF Goiano.*$", "")
                .trim();
    }

    private String extrairResumoDoBloco(String textoBloco, String titulo) {
        if (textoBloco == null) return "";
        String limpo = textoBloco.replace(titulo, "").trim();
        return limpo.length() > 350 ? limpo.substring(0, 347).trim() + "..." : limpo;
    }

    private String extrairUrlRealDoRss(String rawLink) {
        if (rawLink == null) return "";
        int idx = rawLink.indexOf("url=");
        if (idx != -1) {
            String encoded = rawLink.substring(idx + 4);
            int amp = encoded.indexOf('&');
            if (amp != -1) encoded = encoded.substring(0, amp);
            return limparUrl(URLDecoder.decode(encoded, StandardCharsets.UTF_8));
        }
        return limparUrl(rawLink);
    }

    private String textoTag(Element parent, String tag) {
        Element el = parent.selectFirst(tag);
        return el != null ? el.text().trim() : "";
    }

    private LocalDate extrairDataPublicacaoIF(Document pagina, Element corpo) {
        Element metaDate = pagina.selectFirst("meta[property=article:published_time], meta[name=DC.date.created], time[datetime]");
        if (metaDate != null) {
            String raw = metaDate.hasAttr("content") ? metaDate.attr("content") : metaDate.attr("datetime");
            if (raw != null && raw.length() >= 10) {
                try {
                    return LocalDate.parse(raw.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE);
                } catch (Exception ignored) {
                }
            }
        }

        Elements metaBlocos = pagina.select("span.documentPublished, ul.documentByLine, .article-info, .published, .create, dd.published, dd.create");
        LocalDate encontrada = extrairDataPortugues(metaBlocos.text());
        if (encontrada != null) return encontrada;

        Element area = corpo != null ? corpo : pagina.selectFirst(".item-page, article, #content-section, main");
        if (area != null) {
            String texto = area.text();
            if (texto.length() > 1500) texto = texto.substring(0, 1500);
            return extrairDataPortugues(texto);
        }
        return null;
    }

    private String limparUrl(String url) {
        if (url == null) return "";
        int hashIdx = url.indexOf('#');
        return hashIdx > 0 ? url.substring(0, hashIdx) : url;
    }

    private boolean isLinkValidoNoticiaIF(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        if (!lower.contains("ifgoiano.edu.br/")) return false;
        if (lower.contains("/component/") || lower.contains("/agenda") || lower.endsWith(".pdf")
                || lower.endsWith(".jpg") || lower.endsWith(".png")) {
            return false;
        }
        return !lower.endsWith("ifgoiano.edu.br/") && !lower.endsWith("ifgoiano.edu.br/home/") && !lower.endsWith("index.php");
    }

    private Connection conectarPortal(String url) {
        try {
            TrustManager[] trustAll = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    }
            };
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAll, new SecureRandom());
            return Jsoup.connect(url)
                    .sslSocketFactory(sc.getSocketFactory())
                    .userAgent(USER_AGENT)
                    .timeout(15000);
        } catch (Exception e) {
            return Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(15000);
        }
    }

    @Transactional
    public Noticia sincronizarItem(String fonteOrigem, String linkOrigem, String titulo, String conteudo, String imagemUrl) {
        return sincronizarItem(fonteOrigem, linkOrigem, titulo, conteudo, imagemUrl, LocalDateTime.now(), null);
    }

    @Transactional
    public Noticia sincronizarItem(String fonteOrigem, String linkOrigem, String titulo, String conteudo, String imagemUrl, LocalDateTime publicadoEm) {
        return sincronizarItem(fonteOrigem, linkOrigem, titulo, conteudo, imagemUrl, publicadoEm, null);
    }

    @Transactional
    public Noticia sincronizarItem(String fonteOrigem, String linkOrigem, String titulo, String conteudo, String imagemUrl, LocalDateTime publicadoEm, String audiodescricaoRecebida) {
        String tituloLimpo = limparTexto(titulo);
        String conteudoLimpo = limparTexto(conteudo);
        LocalDateTime dataReal = publicadoEm != null ? publicadoEm : LocalDateTime.now();

        boolean temAudioLocalValida = audiodescricaoRecebida != null
                && !audiodescricaoRecebida.isBlank()
                && !audiodescricaoRecebida.contains("indisponível");

        if (imagemUrl != null && (imagemUrl.contains("data:image") || imagemUrl.length() > 950)) {
            imagemUrl = null;
        }

        Optional<Noticia> existentePorLink = noticiaRepository.findByLinkOrigem(linkOrigem);
        if (existentePorLink.isPresent()) {
            Noticia existente = existentePorLink.get();
            existente.setStatus(StatusNoticia.PUBLICADO);
            existente.setTitulo(tituloLimpo);
            existente.setConteudo(conteudoLimpo);
            existente.setResumo(gerarResumo(conteudoLimpo));
            existente.setPublicadoEm(dataReal);
            if (imagemUrl != null && !imagemUrl.isBlank()) {
                existente.setImagemUrl(imagemUrl);
            } else if ("INSTAGRAM".equalsIgnoreCase(fonteOrigem)) {
                existente.setImagemUrl(null);
            }

            if (temAudioLocalValida) {
                existente.setAudiodescricaoIA(limparTexto(audiodescricaoRecebida));
            } else if (existente.getAudiodescricaoIA() == null
                    || existente.getAudiodescricaoIA().contains("indisponível")
                    || existente.getAudiodescricaoIA().contains("não possui fotografia")
                    || existente.getAudiodescricaoIA().startsWith("Registro visual")
                    || existente.getAudiodescricaoIA().startsWith("Video by")
                    || existente.getAudiodescricaoIA().startsWith("Photo by")) {
                existente.setAudiodescricaoIA(gerarAudiodescricaoComFallback(imagemUrl, fonteOrigem, tituloLimpo));
            }

            Noticia salva = noticiaRepository.saveAndFlush(existente);
            aplicarPoliticaRetencaoBanco();
            return salva;
        }

        List<Noticia> recentes = noticiaRepository.findAll(Sort.by(Sort.Direction.DESC, "publicadoEm"));
        for (Noticia recente : recentes) {
            boolean mesmaFonte = fonteOrigem != null && fonteOrigem.equalsIgnoreCase(recente.getFonteOrigem());
            if (!mesmaFonte) {
                continue;
            }
            boolean mesmoTexto = isMesmaNoticia(
                    recente.getTitulo() + " " + recente.getConteudo(),
                    tituloLimpo + " " + conteudoLimpo,
                    0.70
            );
            boolean mesmoImagem = imagemUrl != null && !imagemUrl.isBlank() && imagemUrl.equals(recente.getImagemUrl());

            if (mesmoTexto || mesmoImagem) {
                return recente;
            }
        }

        var adminUser = usuarioRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Nenhum usuário encontrado no banco para vincular a notícia."));

        String audiodescricao = temAudioLocalValida
                ? limparTexto(audiodescricaoRecebida)
                : gerarAudiodescricaoComFallback(imagemUrl, fonteOrigem, tituloLimpo);

        CategoriaNoticia categoriaPadrao = CategoriaNoticia.values()[0];

        Noticia nova = Noticia.builder()
                .titulo(tituloLimpo)
                .conteudo(conteudoLimpo)
                .resumo(gerarResumo(conteudoLimpo))
                .autor("IF_GOIANO".equalsIgnoreCase(fonteOrigem) ? "Portal IF Goiano" : "@caoguiaurutai")
                .categoria(categoriaPadrao)
                .status(StatusNoticia.PUBLICADO)
                .fonteOrigem(fonteOrigem)
                .linkOrigem(linkOrigem)
                .imagemUrl(imagemUrl)
                .audiodescricaoIA(audiodescricao)
                .criadoPor(adminUser)
                .atualizadoPor(adminUser)
                .publicadoEm(dataReal)
                .build();

        nova.setPublicadoEm(dataReal);
        Noticia salva = noticiaRepository.saveAndFlush(nova);
        if (!dataReal.equals(salva.getPublicadoEm())) {
            salva.setPublicadoEm(dataReal);
            salva = noticiaRepository.saveAndFlush(salva);
        }

        aplicarPoliticaRetencaoBanco();
        return salva;
    }

    public void aplicarPoliticaRetencaoBanco() {
    }

    private String gerarAudiodescricaoComFallback(String imagemUrl, String fonteOrigem, String titulo) {
        if (imagemUrl != null && !imagemUrl.isBlank()) {
            return audiodescricaoIAService.gerarAudiodescricao(imagemUrl, titulo);
        }
        if ("IF_GOIANO".equalsIgnoreCase(fonteOrigem)) {
            return "Comunicado oficial publicado no Portal do IF Goiano (Campus Urutaí) sobre: " + titulo + ".";
        }
        return "Publicação em vídeo do perfil oficial @caoguiaurutai no Instagram sobre: " + titulo + ".";
    }

    private LocalDate extrairDataPortugues(String texto) {
        if (texto == null || texto.isBlank()) return null;

        Matcher mBarra = Pattern.compile("(\\d{2})/(\\d{2})/(\\d{4})").matcher(texto);
        if (mBarra.find()) {
            try {
                return LocalDate.of(Integer.parseInt(mBarra.group(3)), Integer.parseInt(mBarra.group(2)), Integer.parseInt(mBarra.group(1)));
            } catch (Exception ignored) {
            }
        }

        Matcher pattern = Pattern.compile("(\\d{1,2})\\s+(?:de\\s+)?([a-zA-ZçÇ]+)\\.?\\s+(?:de\\s+)?(20\\d{2})", Pattern.CASE_INSENSITIVE).matcher(texto);
        while (pattern.find()) {
            try {
                int dia = Integer.parseInt(pattern.group(1));
                String mesNome = pattern.group(2).toLowerCase();
                int ano = Integer.parseInt(pattern.group(3));
                Integer mes = MESES_PT.get(mesNome);
                if (mes != null) {
                    return LocalDate.of(ano, mes, dia);
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String limparTexto(String texto) {
        if (texto == null) return "";
        return texto.replaceAll("[\\x{10000}-\\x{10FFFF}]", "")
                .replaceAll("[\\u2600-\\u27BF\\uFE0F\\u200D]", "")
                .replaceAll("(?<=[!?.])\\s*\\?+", "")
                .replaceAll("\\s+\\?+(?=\\s|$)", "")
                .replaceAll("\\?{2,}", "")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    private boolean contemTermoProjeto(String texto) {
        if (texto == null) return false;
        String t = texto.toLowerCase();
        return t.contains("cão-guia") || t.contains("cão guia")
                || t.contains("cao-guia") || t.contains("cao guia")
                || t.contains("cães-guia") || t.contains("cães guia")
                || t.contains("caes-guia") || t.contains("caes guia")
                || t.contains("acessibilidade visual");
    }

    private String gerarResumo(String conteudo) {
        if (conteudo == null) return "";
        return conteudo.length() > 180 ? conteudo.substring(0, 177) + "..." : conteudo;
    }

    private boolean isMesmaNoticia(String textoA, String textoB, double limiar) {
        Set<String> palavrasA = extrairPalavrasChave(textoA);
        Set<String> palavrasB = extrairPalavrasChave(textoB);
        if (palavrasA.isEmpty() || palavrasB.isEmpty()) return false;

        Set<String> intersecao = new HashSet<>(palavrasA);
        intersecao.retainAll(palavrasB);

        Set<String> uniao = new HashSet<>(palavrasA);
        uniao.addAll(palavrasB);

        double similaridade = (double) intersecao.size() / uniao.size();
        return similaridade >= limiar;
    }

    private Set<String> extrairPalavrasChave(String texto) {
        if (texto == null) return Collections.emptySet();
        String normalizado = Normalizer.normalize(texto.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("[^a-z0-9\\s]", " ");
        return Arrays.stream(normalizado.split("\\s+"))
                .filter(p -> p.length() > 3)
                .collect(Collectors.toSet());
    }
}