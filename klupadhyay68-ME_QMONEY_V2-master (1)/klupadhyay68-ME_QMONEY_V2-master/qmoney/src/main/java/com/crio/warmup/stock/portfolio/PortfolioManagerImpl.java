
package com.crio.warmup.stock.portfolio;


import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {


  static String token = "878bc729398b224dd09fa8117688e3b7674e3bed";
private RestTemplate restTemplate;

  // Caution: Do not delete or modify the constructor, or else your build will break!
  // This is absolutely necessary for backward compatibility 
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }


  public List<Candle> getStockQuote(String symbol, LocalDate startDate, LocalDate endDate)
      throws JsonProcessingException {

        String urlBuilder = buildUrl(symbol, startDate, endDate);
        RestTemplate restTemplate = new RestTemplate();
        // Getting the POJO from THIRD PARTY APP (tingo) through REST TEMPLATE
        return Arrays.asList(restTemplate.getForObject(urlBuilder, TiingoCandle[].class));
  }

  static Double getOpeningPriceOnStartDate(List<Candle> candles) {
    return candles.get(0).getOpen();
 }


 public static Double getClosingPriceOnEndDate(List<Candle> candles) {
    return candles.get(candles.size()-1).getClose();
 }

  protected static String buildUrl(String symbol, LocalDate startDate, LocalDate endDate) {
    return "https://api.tiingo.com/tiingo/daily/" + symbol + "/prices?startDate=" + startDate.toString() +
    "&endDate=" + endDate.toString() + "&token=" + token;
  }


  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades,
      LocalDate endDate) throws JsonProcessingException {
    
        List<AnnualizedReturn> returnFromStocks = new ArrayList<AnnualizedReturn>();

        for(PortfolioTrade t: portfolioTrades) {
          List<Candle> tiingoStocks = getStockQuote(t.getSymbol(), t.getPurchaseDate(), endDate);
          AnnualizedReturn annualizedStock = calculateAnnualizedReturns(endDate, t,
           getOpeningPriceOnStartDate(tiingoStocks), getClosingPriceOnEndDate(tiingoStocks));
           returnFromStocks.add(annualizedStock);
        }
        
        List<AnnualizedReturn> result = returnFromStocks.stream()
                                        .sorted()
                                        .collect(Collectors.toList());

     return result;
  }

  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate,
      PortfolioTrade trade, Double buyPrice, Double sellPrice) {
      
        // 1. Calculate the total return (absolute returns)
        double totalReturns = (sellPrice - buyPrice) / buyPrice;

        // 2. Calculate the number of days between buy and sell dates in order to get the total years
        LocalDate purchaseDate = trade.getPurchaseDate();
        LocalDate sellDate = endDate;

        double daysBetweenBuyAndSell = ChronoUnit.DAYS.between(purchaseDate, sellDate);
      
        // 3 . Calculate the annualized return
        double total_num_years = (daysBetweenBuyAndSell/365.24);

        double annualizedReturns = Math.pow((1 + totalReturns),(1 / total_num_years)) - 1;

        // 4. Return the annualized object
      return new AnnualizedReturn(trade.getSymbol(), annualizedReturns, totalReturns);
    }
}
