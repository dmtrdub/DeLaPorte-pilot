create table if not exists exchange
(
    id                   int auto_increment,
    deposit_fee          decimal(11, 6) not null,
    withdraw_fee         decimal(11, 6) not null,
    taker_fee_percentage decimal(6, 3)  not null,
    name                 varchar(200)   not null,
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
    base              varchar(6)                         not null,
    target            varchar(6)                         not null,
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
    exchange_id       int                                not null,
    constraint ticker_id_uindex
        unique (id),
    constraint ticker_exchange_fk
        foreign key (exchange_id) references exchange (id)
)
    comment 'Basic entity for ticker market data';

alter table ticker
    add primary key (id);