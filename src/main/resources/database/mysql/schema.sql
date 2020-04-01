create table if not exists local.TICKER
(
    id                int                                not null auto_increment,
    base              varchar(6)                         not null,
    target            varchar(6)                         not null,
    exchange_name     varchar(200) charset utf8          not null,
    volume            decimal(20, 10)                    not null,
    volume_btc        decimal(20, 10)                    null,
    volume_usd        decimal(20, 10)                    null,
    price             decimal(16, 10)                    not null,
    price_btc         decimal(16, 10)                    null,
    price_usd         decimal(16, 10)                    null,
    spread_percentage decimal(6, 3)                      not null,
    time              datetime default CURRENT_TIMESTAMP not null,
    anomaly           tinyint  default 0                 not null,
    stale             tinyint  default 0                 not null,
    constraint ticker_id_uindex
        unique (id)
)
    auto_increment = 100,
    comment 'Basic entity for ticker market data';

alter table local.TICKER
    add primary key (id);