insert into local.exchange (name, deposit_fee_usd, withdraw_fee_usd, taker_fee_percentage,
                            trust_score, base_endpoint, bars_per_request, api_request_rate_min, api_request_rate_min_preload, asc_preload)
values ('BIGONE', 0, 0.0005, 0.1, 7, 'https://big.one/api/v3/', 500, 100, 2700, 0),
       ('BINANCE', 0, 0.0005, 0.1, 10, 'https://api.binance.com/api/v3/', 1000, 100, 1100, 1),
       ('BITFINEX', 0, 0, 0.2, 10, 'https://api-pub.bitfinex.com/v2/', 10000, 30, 60, 1),
       ('BITMAX', 0, 0.0005, 0.1, 8, 'https://bitmax.io/api/pro/v1/', 500, 100, 580, 1),
       ('GATE', 0, 0.0005, 0.2, 9, 'https://api.gateio.ws/api/v4/', 1000 , 100, 10000, 1),

       /*to be added*/
       ('BITBAY', 0, 0.0002, 0.1, 7, 'https://api.bitbay.net/rest/', 55),
       ('BITMART', 0, 0.00005, 0.2, 6, 'https://openapi.bitmart.com/v2/', 60),
       ('BITTREX', 0, 0.0005, 0.2, 9, 'https://api.bittrex.com/v3/', 60),

       ('HUOBI', 0, 0.4, 0.2, 10, 'https://api.huobi.pro/', 100),

       ('kraken', 'Kraken', 0, 0.0001, 0.26, 135, 10),
       ('kucoin', 'KuCoin', 0, 0.0005, 0.1, 445, 10),
       ('kuna', 'Kuna Exchange', 0, 0.00005, 0.25, 19, 6),
       ('latoken', 'LATOKEN', 0, 0.27, 0.1, 252, 6),
       ('livecoin', 'Livecoin', 0, 0.00005, 0.18, 593, 8),
       ('okex', 'OKEx', 0, 0.0005, 0.15, 366, 7),
       ('poloniex', 'Poloniex', 0, 0.000001, 0.09, 117, 10),
       ('probit', 'Probit', 0, 0.027, 0.2, 366, 9) /*unknown actual withdrawal fee*/,
       ('upbit', 'Upbit', 0, 0.0005, 0.25, 246, 8),
       ('whitebit', 'WhiteBIT', 0, 0.027, 0.1, 100, 8),
       ('xt', 'XT', 0, 0.0054, 0.2, 141, 8),
       ('zb', 'ZB', 0, 0.0054, 0.2, 184, 7);
