
package com.crio.warmup.stock;


import com.crio.warmup.stock.dto.*;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.crio.warmup.stock.portfolio.PortfolioManager;
import com.crio.warmup.stock.portfolio.PortfolioManagerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;


public class PortfolioManagerApplication {


  // TODO: CRIO_TASK_MODULE_CALCULATIONS
  //  Now that you have the list of PortfolioTrade and their data, calculate annualized returns
  //  for the stocks provided in the Json.
  //  Use the function you just wrote #calculateAnnualizedReturns.
  //  Return the list of AnnualizedReturns sorted by annualizedReturns in descending order.

  // Note:
  // 1. You may need to copy relevant code from #mainReadQuotes to parse the Json.
  // 2. Remember to get the latest quotes from Tiingo API.  

  public static List<String> mainReadFile(String[] args) throws IOException, URISyntaxException {
    
    ObjectMapper om = getObjectMapper();
    File inputFile = resolveFileFromResources(args[0]);

    PortfolioTrade[] trades = om.readValue(inputFile, PortfolioTrade[].class);

    List<String> stocks = new ArrayList<String>(); 
    for(PortfolioTrade t: trades) {
      stocks.add(t.getSymbol());
    }

    return stocks;
  }


  public static List<TotalReturnsDto> helper(String[] args, List<PortfolioTrade> trades) throws IOException, URISyntaxException {

    LocalDate endDate = LocalDate.parse(args[1]);
    List<TotalReturnsDto> stocksToBeSort = new ArrayList<TotalReturnsDto>();
    
    for(PortfolioTrade t: trades) {

      LocalDate date1 = LocalDate.parse(t.getPurchaseDate().toString());
      LocalDate date2 = LocalDate.parse(endDate.toString());

      if(date1.isAfter(date2)) {
        throw new RuntimeException();
      }

      List<Candle> tiingoTrades = fetchCandles(t, endDate, getToken());

      if(!ObjectUtils.isEmpty(tiingoTrades)) {
        stocksToBeSort.add(new TotalReturnsDto(t.getSymbol(), getClosingPriceOnEndDate(tiingoTrades)));
      }
    }
    return stocksToBeSort;
  } 

// Note:
  // Remember to confirm that you are getting same results for annualized returns as in Module 3.
  public static List<String> mainReadQuotes(String[] args) throws IOException, URISyntaxException {

    // Getting the POJO from trades.json which ish passed as args[0]
    List<PortfolioTrade> trades = readTradesFromJson(args[0]);

    List<TotalReturnsDto> sortByValue = helper(args, trades);

    stockComparator sortTheStock = new stockComparator();
    Collections.sort(sortByValue, sortTheStock);

    List<String> stocks = new ArrayList<String>();
    for(TotalReturnsDto t: sortByValue) {
      stocks.add(t.getSymbol());
    }

    return stocks;
 }

 static class stockComparator implements Comparator <TotalReturnsDto> {

  @Override
  public int compare(TotalReturnsDto t1, TotalReturnsDto t2) {
    return (int) (t1.getClosingPrice().compareTo(t2.getClosingPrice()));
  }
}


  public static List<PortfolioTrade> readTradesFromJson(String filename) throws IOException, URISyntaxException {
    
    ObjectMapper om = getObjectMapper();
    File tradeFile = resolveFileFromResources(filename);
    List<PortfolioTrade> trades = new ArrayList<PortfolioTrade>();
    trades = Arrays.asList(om.readValue(tradeFile, PortfolioTrade[].class));
    return trades;
 }


 //  Build the Url using given parameters and use this function in your code to cann the API.
 public static String prepareUrl(PortfolioTrade trade, LocalDate endDate, String token) {

    String companyName = trade.getSymbol();
    String purchaseDate = trade.getPurchaseDate().toString();

    return "https://api.tiingo.com/tiingo/daily/" + companyName + "/prices?startDate=" + purchaseDate +
    "&endDate=" + endDate.toString() + "&token=" + token;
 }

 

  private static void printJsonObject(Object object) throws IOException {
    Logger logger = Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
    ObjectMapper mapper = new ObjectMapper();
    logger.info(mapper.writeValueAsString(object));
  }

  private static File resolveFileFromResources(String filename) throws URISyntaxException {
    return Paths.get(
        Thread.currentThread().getContextClassLoader().getResource(filename).toURI()).toFile();
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

  public static String readFileAsString(String file)throws Exception
    {
        return new String(Files.readAllBytes(Paths.get(file)));
    }


  // TODO: CRIO_TASK_MODULE_JSON_PARSING
  //  Follow the instructions provided in the task documentation and fill up the correct values for
  //  the variables provided. First value is provided for your reference.
  //  A. Put a breakpoint on the first line inside mainReadFile() which says
  //    return Collections.emptyList();
  //  B. Then Debug the test #mainReadFile provided in PortfoliomanagerApplicationTest.java
  //  following the instructions to run the test.
  //  Once you are able to run the test, perform following tasks and record the output as a
  //  String in the function below.
  //  Use this link to see how to evaluate expressions -
  //  https://code.visualstudio.com/docs/editor/debugging#_data-inspection
  //  1. evaluate the value of "args[0]" and set the value
  //     to the variable named valueOfArgument0 (This is implemented for your reference.)
  //  2. In the same window, evaluate the value of expression below and set it
  //  to resultOfResolveFilePathArgs0
  //     expression ==> resolveFileFromResources(args[0])
  //  3. In the same window, evaluate the value of expression below and set it
  //  to toStringOfObjectMapper.
  //  You might see some garbage numbers in the output. Dont worry, its expected.
  //    expression ==> getObjectMapper().toString()
  //  4. Now Go to the debug window and open stack trace. Put the name of the function you see at
  //  second place from top to variable functionNameFromTestFileInStackTrace
  //  5. In the same window, you will see the line number of the function in the stack trace window.
  //  assign the same to lineNumberFromTestFileInStackTrace
  //  Once you are done with above, just run the corresponding test and
  //  make sure its working as expected. use below command to do the same.
  //  ./gradlew test --tests PortfolioManagerApplicationTest.testDebugValues

  public static List<String> debugOutputs() {

     String valueOfArgument0 = "trades.json";
     String resultOfResolveFilePathArgs0 = "trades.json";
     String toStringOfObjectMapper = "ObjectMapper";
     String functionNameFromTestFileInStackTrace = "mainReadFile";
     String lineNumberFromTestFileInStackTrace = "29";


    return Arrays.asList(new String[]{valueOfArgument0, resultOfResolveFilePathArgs0,
        toStringOfObjectMapper, functionNameFromTestFileInStackTrace,
        lineNumberFromTestFileInStackTrace});
  }


  static Double getOpeningPriceOnStartDate(List<Candle> candles) {
     return candles.get(0).getOpen();
  }


  public static Double getClosingPriceOnEndDate(List<Candle> candles) {
     return candles.get(candles.size()-1).getClose();
  }


  public static List<Candle> fetchCandles(PortfolioTrade trade, LocalDate endDate, String token) {
    String urlBuilder = prepareUrl(trade, endDate, token);
    RestTemplate restTemplate = new RestTemplate();
    // Getting the POJO from THIRD PARTY APP (tingo) through REST TEMPLATE
    return Arrays.asList(restTemplate.getForObject(urlBuilder, TiingoCandle[].class));
  }

  public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args)
      throws IOException, URISyntaxException {
      
        List<PortfolioTrade> trades = readTradesFromJson(args[0]);
        LocalDate endDate = LocalDate.parse(args[1]);

        List<AnnualizedReturn> returnFromStocks = new ArrayList<AnnualizedReturn>();

        for(PortfolioTrade t: trades) {
          List<Candle> tiingoStocks = fetchCandles(t, endDate, getToken());
          AnnualizedReturn annualizedStock = calculateAnnualizedReturns(endDate, t,
           getOpeningPriceOnStartDate(tiingoStocks), getClosingPriceOnEndDate(tiingoStocks));
           returnFromStocks.add(annualizedStock);
        }
        
        List<AnnualizedReturn> result = returnFromStocks.stream()
                                        .sorted()
                                        .collect(Collectors.toList());

     return result;
  }

  // TODO: CRIO_TASK_MODULE_CALCULATIONS
  //  Return the populated list of AnnualizedReturn for all stocks.
  //  Annualized returns should be calculated in two steps:
  //   1. Calculate totalReturn = (sell_value - buy_value) / buy_value.
  //      1.1 Store the same as totalReturns
  //   2. Calculate extrapolated annualized returns by scaling the same in years span.
  //      The formula is:
  //      annualized_returns = (1 + total_returns) ^ (1 / total_num_years) - 1
  //      2.1 Store the same as annualized_returns
  //  Test the same using below specified command. The build should be successful.
  //     ./gradlew test --tests PortfolioManagerApplicationTest.testCalculateAnnualizedReturn

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




  // TODO: CRIO_TASK_MODULE_REFACTOR
  //  Once you are done with the implementation inside PortfolioManagerImpl and
  //  PortfolioManagerFactory, create PortfolioManager using PortfolioManagerFactory.
  //  Refer to the code from previous modules to get the List<PortfolioTrades> and endDate, and
  //  call the newly implemented method in PortfolioManager to calculate the annualized returns.

  // Note:
  // Remember to confirm that you are getting same results for annualized returns as in Module 3.

  public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args)
      throws Exception {
       String file = args[0];
       LocalDate endDate = LocalDate.parse(args[1]);
       RestTemplate restTemplate = new RestTemplate();
       PortfolioManager portfolioManager = PortfolioManagerFactory.getPortfolioManager(restTemplate);
       String contents = readFileAsString(file);
       ObjectMapper objectMapper = getObjectMapper();
       PortfolioTrade[] portfolioTrades = objectMapper.readValue(contents, PortfolioTrade[].class);
       return portfolioManager.calculateAnnualizedReturn(Arrays.asList(portfolioTrades), endDate);
  }


  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());

    printJsonObject(mainCalculateReturnsAfterRefactor(args));
    // printJsonObject(mainCalculateSingleReturn(args));
    // printJsonObject(mainReadQuotes(args));
  }

  public static String getToken() {
    String token = "878bc729398b224dd09fa8117688e3b7674e3bed";
    return token;
  }
}

