package com.crypto.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONObject;
import com.crypto.api.model.TradeResult;

public class Test2 {

  public static void main(String[] args) throws IOException, InterruptedException {

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("https://api.crypto.com/v2/public/get-trades?instrument_name=BTC_USDT"))
        .header("Content-Type", "application/json").GET().build();
    HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(Duration.ofSeconds(20)).build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
    System.out.println(tradeList.size());
    System.out.println(tradeList.get(0).getTradeTime().format(formatter));
    System.out.println(tradeList.get(tradeList.size() - 1).getTradeTime().format(formatter));
    tradeList.forEach(t -> {
      System.out.println(t.getTradeTime());
    });
  }

}
