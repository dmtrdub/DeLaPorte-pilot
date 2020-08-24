# DeLaPorte-pilot

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/3ba6f582f05f4059aaf5888c032b27fd)](https://app.codacy.com/manual/dmytro.dbnsky/DeLaPorte-pilot?utm_source=github.com&utm_medium=referral&utm_content=dmtrdub/DeLaPorte-pilot&utm_campaign=Badge_Grade_Dashboard)

Project dedicated to test cryptocurrency arbitrage trading possibilities. Named after Mathieu de la Porte, who was the first to define 'Arbitrage'. 

## Workflow:

1. Fetch tickers from all subscribed exchanges, save them to the local container.
2. Find equivalent tickers from other exchanges, with price difference > average.
3. Create and save trade.
4. Update P&L for each active trade.
5. When price difference <= set %, close trade, record profit.


### UPD:
- (_09.05.2020_) Currently, CoinGecko API does not provide relevant real-time market data. Tickers are fetched with variable delay, which significantly reduces possibilities to find arbitrage opportunities.
- (_23.05.2020_) Google HTTP client + Jackson library allows to easily parse API response. This will allow to fetch up-to-date market data.