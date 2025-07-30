package com.alura.conversor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

public class ConversorMoedas {

    private static final String API_KEY = "cfac81322d5fecc37b737cfc"; // sua chave
    private static final String BASE_URL = "https://v6.exchangerate-api.com/v6";

    private static final Map<String, String> LABEL = new HashMap<>();
    static {
        LABEL.put("USD", "Dólar");
        LABEL.put("ARS", "Peso argentino");
        LABEL.put("BRL", "Real brasileiro");
        LABEL.put("COP", "Peso colombiano");
    }

    public static void main(String[] args) {
        Locale.setDefault(new Locale("pt", "BR"));

        try (Scanner sc = new Scanner(System.in, StandardCharsets.UTF_8)) {
            System.out.println("************************************************");
            System.out.println(" 1) Dólar >> Peso argentino");
            System.out.println(" 2) Peso argentino >> Dólar");
            System.out.println(" 3) Dólar >> Real brasileiro");
            System.out.println(" 4) Real brasileiro >> Dólar");
            System.out.println(" 5) Dólar >> Peso colombiano");
            System.out.println(" 6) Peso colombiano >> Dólar");
            System.out.println(" 7) Sair");
            System.out.print("Escolha uma opção válida: ");
            int opcao = lerInteiro(sc);

            String from, to;
            switch (opcao) {
                case 1: from = "USD"; to = "ARS"; break;
                case 2: from = "ARS"; to = "USD"; break;
                case 3: from = "USD"; to = "BRL"; break;
                case 4: from = "BRL"; to = "USD"; break;
                case 5: from = "USD"; to = "COP"; break;
                case 6: from = "COP"; to = "USD"; break;
                case 7:
                    System.out.println("Programa encerrado.");
                    return;
                default:
                    System.out.println("Opção inválida. Encerrando.");
                    return;
            }

            System.out.print("Digite o valor que deseja converter: ");
            BigDecimal valor = lerDecimal(sc);
            if (valor.compareTo(BigDecimal.ZERO) < 0) {
                System.out.println("Valor não pode ser negativo. Encerrando.");
                return;
            }

            BigDecimal convertido = converter(from, to, valor);

            System.out.println();
            System.out.printf("%s %s equivalem a %s %s%n",
                    formatar(valor), LABEL.get(from), formatar(convertido), LABEL.get(to));
            System.out.println("Programa encerrado.");
        } catch (Exception e) {
            System.out.println("Ocorreu um erro: " + e.getMessage());
            System.out.println("Programa encerrado.");
        }
    }

    private static BigDecimal converter(String from, String to, BigDecimal amount)
            throws IOException, InterruptedException {

        String url = String.format("%s/%s/pair/%s/%s/%s",
                BASE_URL, API_KEY, from, to, amount);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Falha na consulta: HTTP " + response.statusCode());
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

        String result = json.has("conversion_result") ? json.get("conversion_result").getAsString() : null;
        String status = json.has("result") ? json.get("result").getAsString() : "error";

        if (!"success".equalsIgnoreCase(status) || result == null) {
            String errorType = json.has("error-type") ? json.get("error-type").getAsString() : "desconhecido";
            throw new IOException("Erro da API (" + status + "): " + errorType);
        }

        return new BigDecimal(result).setScale(2, RoundingMode.HALF_UP);
    }

    private static int lerInteiro(Scanner sc) {
        while (!sc.hasNextInt()) {
            sc.next();
            System.out.print("Digite um número válido: ");
        }
        return sc.nextInt();
    }

    private static BigDecimal lerDecimal(Scanner sc) {
        String txt = sc.next().trim().replace(".", "").replace(',', '.');
        try {
            return new BigDecimal(txt);
        } catch (NumberFormatException e) {
            System.out.print("Valor inválido. Tente novamente: ");
            return lerDecimal(sc);
        }
    }

    private static String formatar(BigDecimal n) {
        return n.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
