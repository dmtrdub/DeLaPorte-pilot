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
    primary key (id),
    constraint exchange_api_name_uindex
        unique (api_name),
    constraint exchange_id_uindex
        unique (id),
    constraint exchange_name_uindex
        unique (name)
);

create unique index exchange_id_uindex
    on exchange (id);

create table if not exists ticker
(
    id                int auto_increment,
    base              varchar(16)                        not null,
    target            varchar(16)                        not null,
    volume            decimal(19, 3)                     not null,
    volume_btc        decimal(19, 3)                     null,
    volume_usd        decimal(19, 3)                     null,
    price             decimal(20, 7)                     not null,
    price_btc         decimal(20, 7)                     null,
    price_usd         decimal(20, 7)                     null,
    time              datetime default CURRENT_TIMESTAMP not null,
    spread_percentage decimal(6, 3)                      not null,
    anomaly           tinyint  default 0                 not null,
    stale             tinyint  default 0                 not null,
    exchange_id       int                                not null,
    primary key (id),
    constraint ticker_id_uindex
        unique (id),
    constraint ticker_exchange_fk
        foreign key (exchange_id) references local.exchange (id) on update cascade on delete set null
)
    comment 'Basic entity for ticker market data';

create unique index ticker_id_uindex
    on ticker (id);

create table if not exists trade
(
    id                int auto_increment,
    amount            decimal(26, 7)                     not null,
    pnl_usd           decimal(26, 7)                     not null,
    pnl_currency      decimal(26, 7)                     not null,
    pnl_min_currency  decimal(26, 7)                     null,
    pnl_min_usd       decimal(26, 7)                     null,
    expenses_usd      decimal(20, 7)                     not null,
    time_start        datetime default CURRENT_TIMESTAMP not null,
    time_end          datetime default CURRENT_TIMESTAMP not null,
    result_type       tinyint  default 0                 not null,
    total_income      decimal(26, 7)                     not null,
    position_short_id int                                null,
    position_long_id  int                                null,
    primary key (id),
    constraint trade_position_long_fk
        foreign key (position_long_id) references local.position (id)
            on update cascade on delete set null,
    constraint trade_position_short_fk
        foreign key (position_short_id) references local.position (id)
            on update cascade on delete set null
);
create unique index trade_id_uindex
    on trade (id);

create table if not exists transfer
(
    id                   int auto_increment,
    sum_each             decimal(26, 7)                     not null,
    base_1               varchar(16)                        not null,
    base_2               varchar(16)                        not null,
    target_1             varchar(16)                        not null,
    target_2             varchar(16)                        not null,
    time_begin           datetime default CURRENT_TIMESTAMP not null,
    time_end             datetime                           null,
    exchange_sender_1    int                                null,
    exchange_sender_2    int                                null,
    exchange_recipient_1 int                                null,
    exchange_recipient_2 int                                null,
    status               tinyint  default 0                 not null,
    primary key (id),
    constraint transfer_recipient_exchange_1_fk
        foreign key (exchange_recipient_1) references local.exchange (id)
            on update cascade on delete set null,
    constraint transfer_recipient_exchange_2_fk
        foreign key (exchange_recipient_2) references local.exchange (id)
            on update cascade on delete set null,
    constraint transfer_sender_exchange_1_fk
        foreign key (exchange_sender_1) references local.exchange (id)
            on update cascade on delete set null,
    constraint transfer_sender_exchange_2_fk
        foreign key (exchange_sender_2) references local.exchange (id)
            on update cascade on delete set null
)
    comment 'table representing cryptocurrency transfers between exchanges (2 recipients and 2 senders)';

create unique index transfer_id_uindex
    on transfer (id);

create table if not exists position
(
    id               int auto_increment,
    base             varchar(16)              not null,
    target           varchar(16)              not null,
    side             tinyint                  not null,
    pnl_usd          decimal(26, 7) default 0 not null,
    pnl_currency     decimal(26, 7) default 0 not null,
    pnl_min_usd      decimal(26, 7) default 0 not null,
    pnl_min_currency decimal(26, 7) default 0 not null,
    expenses_usd     decimal(20, 7) default 0 not null,
    open_price       decimal(20, 7)           not null,
    close_price_usd  decimal(20, 7)           null,
    close_price      decimal(20, 7)           null,
    open_price_usd   decimal(20, 7)           not null,
    exchange_id      int                      null,
    primary key (id),
    constraint position_exchange_fk
        foreign key (exchange_id) references exchange (id)
            on update cascade on delete set null
)
    comment 'exchange-specific position';

create unique index position_id_uindex
    on position (id);