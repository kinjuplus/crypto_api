package com.crypto.api.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CandleStickResult {

  private LocalDateTime endTime;

  private BigDecimal open;

  private BigDecimal close;

  private BigDecimal high;

  private BigDecimal low;

  private BigDecimal volume;
}
