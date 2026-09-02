/**
 * keep small, put to history/log created_at timestamp with time zone not null,
       created_by varchar(30) not null,
       modified_at timestamp with time zone not null,
       modified_by varchar(30) not null,
 */

/**
 * Appears to be a OSM clone. But we need a location for meta data like
 * - major/minor in road junctions
 */
create table osmnode (
    id bigint not null,
    osm_id bigint not null,
    lat double precision not null,
    lon double precision not null,

    primary key(id),
    unique (osm_id)
);

create sequence osmnode_seq start with 1;

create table osmway (
    id bigint not null,
    osm_id bigint not null,
    category varchar,

    primary key(id),
    check (category in ('ROAD','RIVER','RAILWAY')),
    unique (osm_id)
);

create sequence osmway_seq start with 1;

/**
 * Mapping
 */
create table osmwaynode (
    osmway_id bigint not null references osmway,
    osmnode_id bigint not null references osmnode,
    index int not null,

    primary key(osmway_id, osmnode_id, index)
);

create table mesh (
    id bigint not null,
    name varchar not null,
    created_at timestamp with time zone not null,
    modified_at timestamp with time zone not null,

    primary key(id),
    unique(name)
);

create sequence mesh_seq start with 1;

create table meshnode (
    id bigint not null,
    lat double precision not null,
    lon double precision not null,
    -- the OSM 'parent'
    osmnode_id bigint references osmnode,
    mesh_id bigint references mesh not null,
    created_at timestamp with time zone not null,
    modified_at timestamp with time zone not null,

    primary key(id)
);

create sequence meshnode_seq start with 1;

-- There is not database model that keeps the mesh consistent per se. Thats just impossible.
-- So represent area just by left/right areas of lines for now. That way isn't better/worse than a list of lines
-- mapped to area. At least is makes sure a line cannot belong to more than two areas.
-- And for now we define that sea and background are no areas
create table mesharea (
    id bigint not null,
    -- material is bit encoded. Do we really need material? Isn't it a matter of model building?
    material int,
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

create table meshpolygon (
    id bigint not null,
    type int not null,
    -- for now no reference. And not unique, who knows what happens
    osmid bigint,
    mesh_id bigint not null,
    created_at timestamp with time zone not null,
    modified_at timestamp with time zone not null,

    primary key(id)
);

create sequence meshpolygon_seq start with 1;

create index meshpolygon_mesh_id_idx on meshpolygon(mesh_id);

/**
 * closed (like JTS polygons?)
 */
create table meshpolygonnode (
    id bigint not null,
    index int not null,
    meshpolygon_id bigint not null references meshpolygon,
    meshnode_id bigint not null references meshnode,
    created_at timestamp with time zone not null,
    modified_at timestamp with time zone not null,

    primary key(id),
    unique(meshpolygon_id, meshnode_id, index)
);

create sequence meshpolygonnode_seq start with 1;

create index meshpolygonnode_meshpolygon_id_idx on meshpolygonnode(meshpolygon_id);
create index meshpolygonnode_meshnode_id_idx on meshpolygonnode(meshnode_id);

/**
 *
 */
create table meshnodepair (
    id bigint not null,
    leftnode int not null,-- references meshnode,
    rightnode int not null,-- references meshnode,
    -- for now no reference
    osmid bigint not null,
    meshpolygon_id bigint not null references meshpolygon,
    --opposite_node_osm_id bigint not null,
    heading double precision not null,
    created_at timestamp with time zone not null,
    modified_at timestamp with time zone not null,

    primary key(id)
);

create sequence meshnodepair_seq start with 1;

create index meshnodepair_meshpolygon_id_idx on meshnodepair(meshpolygon_id);

create table meshfailure (
    id bigint not null,
    sourceref varchar,
    polygon varchar,
    message varchar,
    svg varchar,
    mesh_id bigint not null references mesh,
    created_at timestamp with time zone not null,
    modified_at timestamp with time zone not null,

    primary key(id)
);

create sequence meshfailure_seq start with 1;

