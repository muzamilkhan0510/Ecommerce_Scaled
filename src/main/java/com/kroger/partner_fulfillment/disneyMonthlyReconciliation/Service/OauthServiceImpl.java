package com.kroger.partner_fulfillment.disneyMonthlyReconciliation.Service;

import com.google.gson.Gson;
import com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO.DataDogLoggingObject;
import com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO.OauthObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OauthServiceImpl implements OauthService {

    @Value("${oauth.client_id}")
    private String clientId;

    @Value("${oauth.client_secret}")
    private String clientSecret;

    @Value("${oauth.token_url}")
    private String tokenUrl;

    @Value("${oauth.timeout}")
    private String timeout;

    @Value("${oauth.claims}")
    private String claims;

    @Value("${oauth.retry}")
    private String oauthRetryAttempts;

    @Override
    public String getOauthToken() throws IOException, InterruptedException {

        String tokenURL = tokenUrl;
        String formatted = clientId + ":" + clientSecret;
        String encoded = Base64.getEncoder().encodeToString((formatted).getBytes());

        Map<String, String> parameters = new HashMap<>();
        parameters.put("grant_type", "client_credentials");
        parameters.put("scope", claims);
        String form = parameters.keySet().stream()
                .map(key -> key + "=" + URLEncoder.encode(parameters.get(key), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = null;
        long startTime = System.currentTimeMillis();
        int attempts = Integer.parseInt(oauthRetryAttempts);
        OauthObject oauthResponse = null;

        while (request == null && attempts > 0) {
            try {
                request = HttpRequest.newBuilder().uri(URI.create(tokenURL))
                        .headers("Content-Type", "application/x-www-form-urlencoded")
                        .headers("Authorization", "Basic " + encoded)
                        .POST(HttpRequest.BodyPublishers.ofString(form)).build();

                HttpResponse<?> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                Gson g = new Gson();
                oauthResponse = g.fromJson(response.body().toString(), OauthObject.class);

            } catch (Exception e) {
                long elapsed = System.currentTimeMillis() - startTime;
                DataDogLoggingObject dataDogLoggingObject = new DataDogLoggingObject("Error During Oauth Token Generation: Attempt " + ((Integer.parseInt(oauthRetryAttempts)+1)-attempts), String.valueOf(elapsed), "500", e.getCause() !=null ? getStackTraceAsString(e.getCause()) : null, null);
                Gson objectMapper = new Gson();
                System.out.println(objectMapper.toJson(dataDogLoggingObject));
                attempts--;
            }
        }

        if(oauthResponse == null) {
            throw new RuntimeException("Error during Oauth Token Generation");
        }


        return oauthResponse.getAccess_token();
    }

    private static String getStackTraceAsString(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        return stringWriter.toString();
    }
}
