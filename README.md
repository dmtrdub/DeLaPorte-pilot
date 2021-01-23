# DeLaPorte-pilot

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/3ba6f582f05f4059aaf5888c032b27fd)](https://app.codacy.com/manual/dmytro.dbnsky/DeLaPorte-pilot?utm_source=github.com&utm_medium=referral&utm_content=dmtrdub/DeLaPorte-pilot&utm_campaign=Badge_Grade_Dashboard)

Project dedicated to test cryptocurrency arbitrage trading possibilities. Named after Mathieu de la Porte, who was the first to define 'Arbitrage'. 

## Workflow:

1. Preload bars from all subscribed exchanges for a specified period. Save bars to DB.
2. Calculate average close price difference for each equivalent pairs on two different exchanges (e.g. _Binance-Bitfinex: BTC/USDT_).
3. Constantly fetch tickers to obtain relevant price in real time.
4. Open new trades when current price difference > average price difference, and when parameter-based conditions for trade open are satisfied.
5. Handle opened trades: update P&L, and close in case of profit, loss or trade duration exceeding the appropriate parameters.
6. Record closed trades to DB, and to result file.

NOTE: application support '_force exit_' option - when a file containing exit code is detected on a specified path, the test run will end prematurely.


### UPD:
- (_09.05.2020_) Currently, CoinGecko API does not provide relevant real-time market data. Tickers are fetched with variable delay, which significantly reduces possibilities to find arbitrage opportunities.
- (_23.05.2020_) Google HTTP client + Jackson library allows to easily parse API response. This will allow to fetch up-to-date market data.
- (_30.10.2020_) Added bars preload functionality.
- (_23.01.2021_) Current trade entry model did not show long-term benefits. Therefore, a new mathematically-justified model is considered to be implemented as a separate project.