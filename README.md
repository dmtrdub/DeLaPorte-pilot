# DeLaPorte-pilot
Project dedicated to test cryptocurrency exchange arbitrage possibilities.
CoinGecko API is used to fetch market data.

## Workflow:

1. Fetch tickers from all subscribed exchanges, save them to the database, and to local container.
2. Find equivalent tickers from other exchanges, with price difference > set %, and time difference <= set parameter (trade_ticker_max_difference_seconds). Transfers, simulating movement of money from one exchange to another with set delay (transfer_delay_seconds), are created as a result.
3. If transfers are ready to be processed, check if trade is profitable with current prices. If true, create and save trade.
4. Update P&L for each active trade.
5. When price difference <= set %, close trade, record profit.

### UPD:
Currently, CoinGecko does not provide relevant real-time market data. Tickers are fetched with variable delay, which significantly reduces possibilities to find arbitrage opportunities.