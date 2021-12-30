package com.crypto.api;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.CollectionUtils;
import com.aayushatharva.brotli4j.Brotli4jLoader;
import com.aayushatharva.brotli4j.decoder.BrotliInputStream;
import com.crypto.api.model.CandleStickResult;
import com.crypto.api.model.TradeResult;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class OneMinuteVerifyTest {

  @Test
  public void verifyOneMinute() throws JSONException, IOException, InterruptedException {
    List<TradeResult> tradePairs = getSortedByTimeTradePairsData();
    List<CandleStickResult> candleSticks = getSortedByTimeCandleStickData();
    LocalDateTime startTime = tradePairs.get(0).getTradeTime().truncatedTo(ChronoUnit.MINUTES);
    LocalDateTime endTime =
        tradePairs.get(tradePairs.size() - 1).getTradeTime().truncatedTo(ChronoUnit.MINUTES);
    Optional<CandleStickResult> startCandle =
        candleSticks.stream().filter(a -> a.getEndTime().compareTo(startTime) == 0).findAny();
    if (startCandle.isPresent()) {
      verifyCandle(startCandle.get(), tradePairs);
    }

    Optional<CandleStickResult> endCandle =
        candleSticks.stream().filter(a -> a.getEndTime().compareTo(endTime) == 0).findAny();
    if (endCandle.isPresent()) {
      verifyCandle(endCandle.get(), tradePairs);
    }

  }

  private void verifyCandle(CandleStickResult candle, List<TradeResult> tradePairs) {
    List<TradeResult> tradeInScope = tradePairs.stream().filter(
        t -> t.getTradeTime().truncatedTo(ChronoUnit.MINUTES).compareTo(candle.getEndTime()) == 0)
        .collect(Collectors.toList());
    if (!CollectionUtils.isEmpty(tradeInScope)) {
      tradeInScope.sort((a, b) -> a.getTradeTime().compareTo(b.getTradeTime()));
      BigDecimal maxPrice =
          tradeInScope.stream().map(a -> a.getPrice()).max(BigDecimal::compareTo).get();
      BigDecimal minPrice =
          tradeInScope.stream().map(a -> a.getPrice()).min(BigDecimal::compareTo).get();
      System.out.println(
          "open price " + candle.getOpen() + " first price " + tradeInScope.get(0).getPrice());
      assertTrue(candle.getOpen().compareTo(tradeInScope.get(0).getPrice()) == 0);
      assertTrue(
          candle.getClose().compareTo(tradeInScope.get(tradeInScope.size() - 1).getPrice()) == 0);
      assertTrue(maxPrice.compareTo(candle.getHigh()) == 0);
      assertTrue(minPrice.compareTo(candle.getLow()) == 0);
    }
  }

  private List<TradeResult> getSortedByTimeTradePairsData()
      throws JSONException, IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("https://api.crypto.com/v2/public/get-trades?instrument_name=BTC_USDT"))
        .header("Content-Type", "application/json").GET().build();
    HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(Duration.ofSeconds(20)).build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    JSONObject jo = new JSONObject(response.body());
    JSONObject result = jo.getJSONObject("result");
    JSONArray resultSet = result.getJSONArray("data");
    List<TradeResult> tradeList = new LinkedList<>();
    for (int i = 0, size = resultSet.length(); i < size; i++) {
      JSONObject objectInArray = resultSet.getJSONObject(i);
      TradeResult trade = new TradeResult();
      LocalDateTime tradeTime = LocalDateTime.ofInstant(
          Instant.ofEpochMilli(objectInArray.getLong("t")), TimeZone.getDefault().toZoneId());
      trade.setTradeTime(tradeTime);
      trade.setPrice(objectInArray.getBigDecimal("p"));
      trade.setQuantity(objectInArray.getBigDecimal("q"));
      trade.setSide(objectInArray.getString("s"));
      trade.setInstrument(objectInArray.getString("i"));
      tradeList.add(trade);
    }
    tradeList.sort((a, b) -> a.getTradeTime().compareTo(b.getTradeTime()));
    return tradeList;
  }

  private List<CandleStickResult> getSortedByTimeCandleStickData() throws JSONException {
    JSONObject jo = getCanldeStickResponseJson();
    JSONObject result = jo.getJSONObject("result");
    JSONArray resultSet = result.getJSONArray("data");
    List<CandleStickResult> candleList = new LinkedList<>();
    for (int i = 0, size = resultSet.length(); i < size; i++) {
      JSONObject objectInArray = resultSet.getJSONObject(i);
      CandleStickResult candle = new CandleStickResult();
      LocalDateTime endTime = LocalDateTime.ofInstant(
          Instant.ofEpochMilli(objectInArray.getLong("t")), TimeZone.getDefault().toZoneId());
      candle.setEndTime(endTime);
      candle.setClose(objectInArray.getBigDecimal("c"));
      candle.setHigh(objectInArray.getBigDecimal("h"));
      candle.setLow(objectInArray.getBigDecimal("l"));
      candle.setOpen(objectInArray.getBigDecimal("o"));
      candleList.add(candle);
    }
    candleList.sort((a, b) -> a.getEndTime().compareTo(b.getEndTime()));
    return candleList;
  }

  private JSONObject getCanldeStickResponseJson() {
    HttpGet httpGet = new HttpGet(
        "https://api.crypto.com/v2/public/get-candlestick?instrument_name=BTC_USDT&timeframe=1m");
    httpGet.addHeader("Content-Type", "application/json");
    httpGet.addHeader(HttpHeaders.ACCEPT_ENCODING, "br");
    httpGet.addHeader(HttpHeaders.ACCEPT, "application/json");
    JSONObject result = null;
    try (
        CloseableHttpClient httpClient =
            HttpClientBuilder.create().disableRedirectHandling().build();
        CloseableHttpResponse response = httpClient.execute(httpGet)) {
      String rawJson = decodeResponse(response.getEntity().getContent());
      result = new JSONObject(rawJson.substring(1));
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return result;
  }

  private String decodeResponse(InputStream in) throws IOException {
    Brotli4jLoader.ensureAvailability();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    BrotliInputStream brotliInputStream = new BrotliInputStream(in);
    int read = brotliInputStream.read();
    byte[] BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    baos.write(BOM);
    while (read > -1) {
      baos.write(read);
      read = brotliInputStream.read();
    }
    brotliInputStream.close();
    String rawJson = new String(baos.toByteArray(), StandardCharsets.UTF_8);
    baos.close();
    return rawJson;
  }
}
