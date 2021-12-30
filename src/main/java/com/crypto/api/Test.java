package com.crypto.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import com.aayushatharva.brotli4j.Brotli4jLoader;
import com.aayushatharva.brotli4j.decoder.BrotliInputStream;
import com.crypto.api.model.CandleStickResult;



public class Test {

  public static void main(String[] args) throws IOException, InterruptedException {

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    HttpGet httpGet = new HttpGet(
        "https://api.crypto.com/v2/public/get-candlestick?instrument_name=BTC_USDT&timeframe=1m");
    httpGet.addHeader("Content-Type", "application/json");
    httpGet.addHeader(HttpHeaders.ACCEPT_ENCODING, "br");
    httpGet.addHeader(HttpHeaders.ACCEPT, "application/json");

    try (
        CloseableHttpClient httpClient =
            HttpClientBuilder.create().disableRedirectHandling().build();
        CloseableHttpResponse response = httpClient.execute(httpGet)) {

      Brotli4jLoader.ensureAvailability();
      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      BrotliInputStream brotliInputStream =
          new BrotliInputStream(response.getEntity().getContent());

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

      JSONObject jo = new JSONObject(rawJson.substring(1));
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
      System.out.println(candleList.size());
      System.out.println(candleList.get(0).getEndTime().format(formatter));
      System.out.println(candleList.get(candleList.size() - 1).getEndTime().format(formatter));

    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

}
