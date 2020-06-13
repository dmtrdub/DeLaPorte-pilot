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
    base                  varchar(16)                             not null,
    target                varchar(16)                             not null,
    fixed_expenses_usd    decimal(25, 12)                         not null,
    time_start            datetime      default CURRENT_TIMESTAMP not null,
    time_end              datetime      default CURRENT_TIMESTAMP not null,
    entry_percentage_diff decimal(8, 3) default 0.000             not null,
    result_type           tinyint       default 0                 not null,
    position_short_id     int                                     null,
    position_long_id      int                                     null,
    test_run_id           int                                     null,
    written_to_file       tinyint       default 0                 not null,
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

create table if not exists test_run
(
    id                           int auto_increment,
    entry_amounts_usd            varchar(50)                        not null,
    entry_min_percentage         decimal(8, 3)                      not null,
    entry_max_percentage         decimal(8, 3)                      not null,
    exit_diff_percentage         decimal(8, 3)                      not null,
    trade_timeout_mins           int                                not null,
    detrimental_percentage_delta decimal(8, 3)                      not null,
    time_start                   datetime default CURRENT_TIMESTAMP not null,
    time_end                     datetime default CURRENT_TIMESTAMP not null,
    path_to_result_file          varchar(276)                       null,
    primary key (id)
);

create unique index test_run_id_uindex
    on test_run (id);
