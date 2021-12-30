package com.crypto.api.service.impl;

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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import com.aayushatharva.brotli4j.Brotli4jLoader;
import com.aayushatharva.brotli4j.decoder.BrotliInputStream;
import com.crypto.api.model.CandleStickResult;
import com.crypto.api.model.TradeResult;
import com.crypto.api.service.ValidateService;

@Service
public class ValidateServiceImpl implements ValidateService {

  @Value("${crpyto.api.url.getTrade}")
  private String GET_TRADE_PREFIX;

  @Value("${crpyto.api.url.getCandleStick}")
  private String GET_CANDLE_STICK_PREFIX;

  @Override
  public Map<String, Object> validateInstrument(String instrument)
      throws JSONException, IOException, InterruptedException {
    Map<String, Object> result = new HashMap<>();
    List<String> msgList = new LinkedList<>();
    result.put("msgList", msgList);
    List<CandleStickResult> candleSticks = getSortedByTimeCandleStickData(instrument);
    List<TradeResult> tradePairs = getSortedByTimeTradePairsData(instrument);
    LocalDateTime startTime = tradePairs.get(0).getTradeTime().truncatedTo(ChronoUnit.MINUTES);
    LocalDateTime endTime =
        tradePairs.get(tradePairs.size() - 1).getTradeTime().truncatedTo(ChronoUnit.MINUTES);
    Optional<CandleStickResult> startCandle =
        candleSticks.stream().filter(a -> a.getEndTime().compareTo(startTime) == 0).findAny();
    if (startCandle.isPresent()) {
      verifyCandle(startCandle.get(), tradePairs, result);
    }

    Optional<CandleStickResult> endCandle =
        candleSticks.stream().filter(a -> a.getEndTime().compareTo(endTime) == 0).findAny();
    if (endCandle.isPresent()) {
      verifyCandle(endCandle.get(), tradePairs, result);
    }
    msgList = (List<String>) result.get("msgList");
    if (!CollectionUtils.isEmpty(msgList)) {
      result.put("errorCode", "01");
      msgList = msgList.stream().distinct().collect(Collectors.toList());
      result.put("msgList", msgList);
    } else {
      result.put("errorCode", "00");
    }
    return result;
  }

  private void verifyCandle(CandleStickResult candle, List<TradeResult> tradePairs,
      Map<String, Object> result) {
    List<String> msgList = (List<String>) result.get("msgList");
    List<TradeResult> tradeInScope = tradePairs.stream().filter(
        t -> t.getTradeTime().truncatedTo(ChronoUnit.MINUTES).compareTo(candle.getEndTime()) == 0)
        .collect(Collectors.toList());
    if (!CollectionUtils.isEmpty(tradeInScope)) {
      tradeInScope.sort((a, b) -> a.getTradeTime().compareTo(b.getTradeTime()));
      BigDecimal maxPrice =
          tradeInScope.stream().map(a -> a.getPrice()).max(BigDecimal::compareTo).get();
      BigDecimal minPrice =
          tradeInScope.stream().map(a -> a.getPrice()).min(BigDecimal::compareTo).get();
      BigDecimal startPrice = tradeInScope.get(0).getPrice();
      BigDecimal endPrice = tradeInScope.get(tradeInScope.size() - 1).getPrice();
      if (candle.getOpen().compareTo(startPrice) != 0) {
        StringBuilder builder = new StringBuilder("open price is different, ");
        builder.append("open price from candle: ").append(candle.getOpen())
            .append(" open price from trade: ").append(startPrice);
        msgList.add(builder.toString());
      }
      if (candle.getClose().compareTo(endPrice) != 0) {
        StringBuilder builder = new StringBuilder("close price is different, ");
        builder.append("close price from candle: ").append(candle.getClose())
            .append(" close price from trade: ").append(endPrice);
        msgList.add(builder.toString());
      }
      if (candle.getHigh().compareTo(maxPrice) != 0) {
        StringBuilder builder = new StringBuilder("high price is different, ");
        builder.append("high price from candle: ").append(candle.getHigh())
            .append(" high price from trade: ").append(maxPrice);
        msgList.add(builder.toString());
      }
      if (candle.getLow().compareTo(minPrice) != 0) {
        StringBuilder builder = new StringBuilder("low price is different, ");
        builder.append("low price from candle: ").append(candle.getLow())
            .append(" low price from trade: ").append(minPrice);
        msgList.add(builder.toString());
      }
    }
  }

  private List<TradeResult> getSortedByTimeTradePairsData(String instrument)
      throws JSONException, IOException, InterruptedException {
    String urlEndPoint = getTradeUrlEndPoint(instrument);
    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(urlEndPoint))
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

  private List<CandleStickResult> getSortedByTimeCandleStickData(String instrument)
      throws JSONException {
    JSONObject jo = getCanldeStickResponseJson(instrument);
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


  private JSONObject getCanldeStickResponseJson(String instrument) {
    String urlEndPoint = getCandleStickUrlEndPoint(instrument);
    HttpGet httpGet = new HttpGet(urlEndPoint);
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

  private String getCandleStickUrlEndPoint(String instrument) {
    StringBuilder builder = new StringBuilder(GET_CANDLE_STICK_PREFIX);
    builder.append("instrument_name=").append(instrument).append("&timeframe=1m");
    return builder.toString();
  }

  private String getTradeUrlEndPoint(String instrument) {
    StringBuilder builder = new StringBuilder(GET_TRADE_PREFIX);
    builder.append("instrument_name=").append(instrument);
    return builder.toString();
  }
}
