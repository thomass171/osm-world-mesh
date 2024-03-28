/**
 * keep small, put to history/log created_at timestamp with time zone not null,
       created_by varchar(30) not null,
       modified_at timestamp with time zone not null,
       modified_by varchar(30) not null,
 */

create table osmnode (
    id bigint not null,
    osmid bigint not null,
    lat double precision not null,
    lon double precision not null,

    primary key(id)
);

create sequence osmnode_seq start with 1;

create table meshpoint (
    id bigint not null,
    lat double precision not null,
    lon double precision not null,
    osmnode_id bigint references osmnode,

    primary key(id)
);

create sequence meshpoint_seq start with 1;

create table meshline (
    id bigint not null,
    frompoint bigint not null,
    topoint bigint not null,

    foreign key (frompoint) references meshpoint,
    foreign key (topoint) references meshpoint,
    primary key(id)
);

create sequence meshline_seq start with 1;

