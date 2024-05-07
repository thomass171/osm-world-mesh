
-- 20.4.24: Was too small for K41 to fit once, but now its slightly greater. So K41 fits.
delete from worldmesh_test.meshline;
delete from worldmesh_test.mesharea;
delete from worldmesh_test.meshnode;
delete from worldmesh_test.osmwaynode;
delete from worldmesh_test.osmnode;
delete from worldmesh_test.osmway;

--SouthEast. Near osm node 1829065191(50.9479214, 6.5936041)?
--insert into worldmesh.meshnode values(nextval('worldmesh.meshnode_seq'), 50.9481, 6.5934, null);
insert into worldmesh_test.meshnode values(1, 50.945, 6.6, null);

--North near 1829058473
insert into worldmesh_test.meshnode values(2, 50.963, 6.595, null);

--West 2377084113
insert into worldmesh_test.meshnode values(3, 50.9475747, 6.5874427, null);

alter sequence worldmesh_test.meshnode_seq restart WITH 4;

insert into worldmesh_test.meshline values(1, 1, 1, 2, null, null);
insert into worldmesh_test.meshline values(2, 1, 2, 3, null, null);
insert into worldmesh_test.meshline values(3, 1, 3, 1, null, null);

alter sequence worldmesh_test.meshline_seq restart WITH 4;
