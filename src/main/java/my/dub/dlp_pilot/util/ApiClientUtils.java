package my.dub.dlp_pilot.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import my.dub.dlp_pilot.exception.client.UnexpectedResponseStatusCodeException;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;

public final class ApiClientUtils {

    private static HttpTransport transport = new NetHttpTransport();
    private static HttpRequestFactory requestFactory = transport.createRequestFactory();

    private ApiClientUtils() {
    }

    public static String executeRequest(String url, String exchangeName) throws IOException {
        HttpRequest req = requestFactory.buildGetRequest(new GenericUrl(url));
        HttpResponse response = req.execute();
        int statusCode = response.getStatusCode();
        if (!HttpStatusCodes.isSuccess(statusCode)) {
            throw new UnexpectedResponseStatusCodeException(exchangeName, statusCode, url);
        }
        return response.parseAsString();
    }

    public static String executeRequest(String baseUrl, String endpointUrl, String exchangeName) throws IOException {
        String fullUrl = baseUrl + endpointUrl;
        return executeRequest(fullUrl, exchangeName);
    }

    @SneakyThrows(URISyntaxException.class)
    public static JsonNode executeRequestParseResponse(String baseUrl, String endpointUrl,
            Map<String, String> queryParams, String exchangeName) throws IOException {
        String fullUrl = new URIBuilder(baseUrl + endpointUrl).addParameters(
                queryParams.entrySet().stream().map(entry -> new BasicNameValuePair(entry.getKey(), entry.getValue()))
                        .collect(Collectors.toList())).build().toString();
        return executeRequestParseResponse(fullUrl, exchangeName);
    }

    @SneakyThrows(URISyntaxException.class)
    public static JsonNode executeRequestParseResponse(String baseUrl, String endpointUrl, String queryParamKey,
            String queryParamValue, String exchangeName) throws IOException {
        String fullUrl =
                new URIBuilder(baseUrl + endpointUrl).addParameter(queryParamKey, queryParamValue).build().toString();
        return executeRequestParseResponse(fullUrl, exchangeName);
    }

    public static JsonNode executeRequestParseResponse(String url, String exchangeName) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(executeRequest(url, exchangeName));
    }

    public static JsonNode executeRequestParseResponse(String baseUrl, String endpointUrl, String exchangeName)
            throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(executeRequest(baseUrl, endpointUrl, exchangeName));
    }
}
