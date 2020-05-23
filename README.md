# DeLaPorte-pilot
Project dedicated to test cryptocurrency exchange arbitrage possibilities.
A dedicated exchange API is used to fetch market data.

## Workflow:

1. Fetch tickers from all subscribed exchanges, save them to the database, and to local container.
2. Find equivalent tickers from other exchanges, with price difference > set %, and time difference <= set parameter (trade_ticker_max_difference_seconds).
3. Create and save trade.
4. Update P&L for each active trade.
5. When price difference <= set %, close trade, record profit.


### UPD:
- (_09.05.2020_) Currently, CoinGecko API does not provide relevant real-time market data. Tickers are fetched with variable delay, which significantly reduces possibilities to find arbitrage opportunities.
- (_23.05.2020_) Using Google HTTP client + Jackson library allows to easily parse API response. This will allow to fetch up-to-date market data.