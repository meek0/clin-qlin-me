package bio.ferlab.clin.qlinme.cients;

import bio.ferlab.clin.qlinme.App;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.HttpResponseException;
import io.javalin.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;

@Slf4j
public class KeycloakClient {

  private final String charset = "UTF-8";
  private final ObjectMapper mapper = new ObjectMapper();
  private final HttpClient httpClient;
  private final String authUrl;
  private final String client;
  private final String audience;

  public KeycloakClient(String url, String client, String audience, int timeoutMs) {
    var config = RequestConfig.custom()
      .setConnectTimeout(timeoutMs)
      .setConnectionRequestTimeout(timeoutMs)
      .setSocketTimeout(timeoutMs).build();
    httpClient = HttpClientBuilder.create()
      .setDefaultRequestConfig(config).build();
    this.authUrl = StringUtils.appendIfMissing(url, "/") + "protocol/openid-connect/token";
    this.client = client;
    this.audience = audience;
  }

  public String getAccessToken(String username, String password) throws IOException {
    var request = new HttpPost(authUrl);

    var form = new ArrayList<BasicNameValuePair>();
    form.add(new BasicNameValuePair("client_id", client));
    form.add(new BasicNameValuePair("grant_type", "password"));
    form.add(new BasicNameValuePair("username", username));
    form.add(new BasicNameValuePair("password", password));

    request.setEntity(new UrlEncodedFormEntity(form, charset));
    return extractAccessToken(execute(request));
  }

  public String getRpt(String accessToken) throws IOException {
    var request = new HttpPost(authUrl);
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

    var form = new ArrayList<BasicNameValuePair>();
    form.add(new BasicNameValuePair("audience", audience));
    form.add(new BasicNameValuePair("grant_type", "urn:ietf:params:oauth:grant-type:uma-ticket"));

    request.setEntity(new UrlEncodedFormEntity(form, charset));
    return extractAccessToken(execute(request));
  }

  private String extractAccessToken(String body) throws JsonProcessingException {
    return mapper.readTree(body).get("access_token").asText();
  }

  private String execute(HttpUriRequest request) throws IOException {
    var response = httpClient.execute(request);
    var statusCode = response.getStatusLine().getStatusCode();
    if (statusCode == HttpStatus.OK.getCode()) {
      return EntityUtils.toString(response.getEntity(), charset);
    } else {
      EntityUtils.consumeQuietly(response.getEntity());
      throw new HttpResponseException(statusCode);
    }
  }
}
