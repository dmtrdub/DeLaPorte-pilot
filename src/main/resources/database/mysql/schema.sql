create table if not exists exchange
(
    id                   int auto_increment,
    deposit_fee          decimal(11, 6)     not null,
    withdraw_fee         decimal(11, 6)     not null,
    taker_fee_percentage decimal(6, 3)      not null,
    name                 varchar(200)       not null,
    api_name             varchar(200)       not null,
    pairs_count          smallint default 0 not null,
    trust_score          smallint default 1 not null,
    constraint exchange_api_name_uindex
        unique (api_name),
    constraint exchange_id_uindex
        unique (id),
    constraint exchange_name_uindex
        unique (name)
);

alter table exchange
    add primary key (id);

create table if not exists ticker
(
    id                int auto_increment,
    base              varchar(16)                        not null,
    target            varchar(16)                        not null,
    volume            decimal(19, 3)                     not null,
    volume_btc        decimal(19, 3)                     null,
    volume_usd        decimal(19, 3)                     null,
    price             decimal(16, 8)                     not null,
    price_btc         decimal(16, 8)                     null,
    price_usd         decimal(16, 8)                     null,
    spread_percentage decimal(6, 3)                      not null,
    time              datetime default CURRENT_TIMESTAMP not null,
    anomaly           tinyint  default 0                 not null,
    stale             tinyint  default 0                 not null,
    exchange_id       int                                not null,
    constraint ticker_id_uindex
        unique (id),
    constraint ticker_exchange_fk
        foreign key (exchange_id) references local.exchange (id)
)
    comment 'Basic entity for ticker market data';

alter table local.ticker
    add primary key (id);