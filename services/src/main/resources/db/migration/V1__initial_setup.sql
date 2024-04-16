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

create table osmway (
    id bigint not null,
    osmid bigint not null,

    primary key(id)
);

create sequence osmnode_seq start with 1;

create table meshnode (
    id bigint not null,
    lat double precision not null,
    lon double precision not null,
    -- the OSM 'parent'
    osmnode_id bigint references osmnode,

    primary key(id)
);

create sequence meshnode_seq start with 1;

-- There is not database model that keeps the mesh consistent per se. Thats just impossible.
-- So represent area just by left/right areas of lines for now. That way isn't better/worse than a list of lines
-- mapped to area. At least is makes sure a line cannot belong to more than two areas.
-- And for now we define that sea and background are no areas
create table mesharea (
    id bigint not null,
    -- material is bit encoded
    material int not null,
    -- the OSM way 'parent' when it was a way, null otherwise
    osmway_id bigint references osmway,

    primary key(id)
);

create sequence mesharea_seq start with 1;

create table meshline (
    id bigint not null,
    -- 1=Boundary(coast line), 2=BG triangulation (left/right are null)
    type int not null,
    from_node bigint not null references meshnode,
    to_node bigint not null references meshnode,
    -- left/right from 'from_node' viewpoint
    left_area bigint references mesharea,
    right_area bigint references mesharea,

    primary key(id)
);

create sequence meshline_seq start with 1;

