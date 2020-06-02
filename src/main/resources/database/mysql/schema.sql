create table if not exists exchange
(
    id                   int auto_increment,
    deposit_fee_usd      decimal(11, 6)     not null,
    withdraw_fee_usd     decimal(11, 6)     not null,
    taker_fee_percentage decimal(6, 3)      not null,
    name                 varchar(200)       not null,
    base_endpoint        varchar(400)       not null,
    trust_score          smallint default 1 not null,
    api_request_rate_min int      default 1 not null,
    primary key (id),
    constraint exchange_base_endpoint_uindex
        unique (base_endpoint),
    constraint exchange_id_uindex
        unique (id),
    constraint exchange_name_uindex
        unique (name)
)
    comment 'General info about exchange';

create unique index exchange_id_uindex
    on exchange (id);

create table if not exists trade
(
    id                    int auto_increment,
    amount                decimal(26, 7)                          not null,
    pnl_currency          decimal(31, 12)                         not null,
    pnl_usd               decimal(31, 12)                         not null,
    expenses_usd          decimal(25, 12)                         not null,
    time_start            datetime      default CURRENT_TIMESTAMP not null,
    time_end              datetime      default CURRENT_TIMESTAMP not null,
    entry_percentage_diff decimal(8, 3) default 0.000             not null,
    result_type           tinyint       default 0                 not null,
    total_income          decimal(31, 12)                         not null,
    position_short_id     int                                     null,
    position_long_id      int                                     null,
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

create table if not exists position
(
    id              int auto_increment,
    base            varchar(16)     not null,
    target          varchar(16)     not null,
    side            tinyint         not null,
    open_price      decimal(25, 12) not null,
    open_price_usd  decimal(25, 12) not null,
    close_price     decimal(25, 12) null,
    close_price_usd decimal(25, 12) null,
    exchange_id     int             null,
    primary key (id),
    constraint position_exchange_fk
        foreign key (exchange_id) references exchange (id)
            on update cascade on delete set null
)
    comment 'exchange-specific position';

create unique index position_id_uindex
    on position (id);