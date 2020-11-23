create table if not exists exchange
(
    id                           int auto_increment,
    deposit_fee_usd              decimal(11, 6)       not null,
    withdraw_fee_usd             decimal(11, 6)       not null,
    taker_fee_percentage         decimal(6, 3)        not null,
    name                         varchar(200)         not null,
    base_endpoint                varchar(400)         not null,
    trust_score                  smallint   default 1 not null,
    bars_per_request             int        default 1 not null,
    api_request_rate_min         int        default 1 not null,
    api_request_rate_min_preload int        default 1 not null,
    asc_preload                  tinyint(1) default 1 not null,
    primary key (id),
    constraint exchange_base_endpoint_uindex
        unique (base_endpoint),
    constraint exchange_name_uindex
        unique (name)
)
    comment 'General info about exchange';

create unique index exchange_id_uindex
    on exchange (id);

create table if not exists test_run
(
    id                  int auto_increment,
    config_params       varchar(4000)                        not null,
    time_start          datetime   default CURRENT_TIMESTAMP not null,
    preload_time_start  datetime   default CURRENT_TIMESTAMP not null,
    trades_time_start   datetime,
    time_end            datetime,
    path_to_result_file varchar(276)                         null,
    forced_exit         tinyint(1) default 0                 null,
    primary key (id)
);

create unique index test_run_id_uindex
    on test_run (id);

create table if not exists position
(
    id          int auto_increment,
    side        tinyint         not null,
    open_price  decimal(25, 12) not null,
    close_price decimal(25, 12) null,
    exchange_id int             null,
    primary key (id),
    constraint position_exchange_fk
        foreign key (exchange_id) references exchange (id)
            on update cascade on delete set null
)
    comment 'exchange-specific position';

create unique index position_id_uindex
    on position (id);

create table if not exists trade
(
    id                    int auto_increment,
    base                  varchar(16)                             not null,
    target                varchar(16)                             not null,
    fixed_expenses_usd    decimal(25, 12)                         not null,
    time_start            datetime      default CURRENT_TIMESTAMP not null,
    time_end              datetime      default CURRENT_TIMESTAMP not null,
    entry_percentage_diff decimal(8, 3) default 0.000             not null,
    average_price_diff    decimal(25, 12)                         not null,
    open_price_diff       decimal(25, 12)                         not null,
    close_price_diff      decimal(25, 12)                         not null,
    result_type           tinyint       default 0                 not null,
    position_short_id     int                                     null,
    position_long_id      int                                     null,
    test_run_id           int                                     null,
    written_to_file       tinyint(1)    default 0                 not null,
    primary key (id),
    constraint trade_position_long_fk
        foreign key (position_long_id) references position (id)
            on update cascade on delete set null,
    constraint trade_position_short_fk
        foreign key (position_short_id) references position (id)
            on update cascade on delete set null,
    constraint trade_test_run_fk
        foreign key (test_run_id) references test_run (id)
            on update cascade on delete set null
);
create unique index trade_id_uindex
    on trade (id);

create table if not exists trade_dynamic_result_data
(
    id            int auto_increment,
    amount_usd    decimal(26, 7)  not null,
    expenses_usd  decimal(25, 12) not null,
    pnl_usd_short decimal(31, 12) not null,
    pnl_usd_long  decimal(31, 12) not null,
    income_usd    decimal(31, 12) not null,
    trade_id      int             not null,
    primary key (id),
    constraint trade_dynamic_result_data_trade_fk
        foreign key (trade_id) references trade (id)
            on update cascade on delete cascade
);

create unique index trade_dynamic_result_data_id_uindex
    on trade_dynamic_result_data (id);

create table if not exists bar
(
    id            int auto_increment,
    base          varchar(16)                        not null,
    target        varchar(16)                        not null,
    open_price    decimal(25, 12)                    not null,
    high_price    decimal(25, 12)                    not null,
    low_price     decimal(25, 12)                    not null,
    close_price   decimal(25, 12)                    not null,
    volume        decimal(19, 6)                     not null,
    exchange_name varchar(200)                       not null,
    time_open     datetime default CURRENT_TIMESTAMP not null,
    time_close    datetime default CURRENT_TIMESTAMP not null,
    test_run_id   int                                null,
    primary key (id),
    constraint bar_test_run_fk
        foreign key (test_run_id) references test_run (id)
            on update cascade on delete set null
)
    comment 'preload bars';

create unique index bar_id_uindex
    on bar (id);