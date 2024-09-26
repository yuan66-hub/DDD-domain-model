package com.smartrm.smartrmcommodity.commodity.adapter.convertor.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.smartrm.smartrmcommodity.commodity.adapter.convertor.CommodityPropertyParser;
import com.smartrm.smartrmcommodity.commodity.infrastructure.CommodityError;
import com.smartrm.infracore.exception.DomainException;
import java.math.BigDecimal;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * @author: yoda
 * @description:
 */
public class CurrencyPropertyJsonParser implements
    CommodityPropertyParser<BigDecimal, JsonNode> {

  @Override
  public BigDecimal parse(JsonNode content) {
    if (content == null || content.isNull()) {
      return null;
    }

    if (!content.isValueNode()) {
      throw new DomainException(CommodityError.CommodityParseError);
    }

    String str = content.asText();
    if (StringUtils.isBlank(str)) {
      return null;
    }

    try {
      return NumberUtils.createBigDecimal(str);
    } catch (NumberFormatException e) {
      throw new DomainException(CommodityError.CommodityParseError);
    }
  }
}
