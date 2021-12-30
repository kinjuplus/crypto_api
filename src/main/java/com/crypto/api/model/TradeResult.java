package com.crypto.api.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TradeResult {

  private LocalDateTime tradeTime;

  private BigDecimal price;

  private BigDecimal quantity;

  private String side;

  private String instrument;

}
