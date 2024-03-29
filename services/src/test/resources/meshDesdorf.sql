

delete from worldmesh.meshline;
delete from worldmesh.meshnode;
delete from worldmesh.osmnode;

--SouthEast. Near osm node 1829065191(50.9479214, 6.5936041)?
--insert into worldmesh.meshnode values(nextval('worldmesh.meshnode_seq'), 50.9481, 6.5934, null);
insert into worldmesh.meshnode values(1, 50.9481, 6.5934, null);

--North near 1829058473
insert into worldmesh.meshnode values(2, 50.9502612, 6.5913940, null);

--West 2377084113
insert into worldmesh.meshnode values(3, 50.9475747, 6.5874427, null);

alter sequence worldmesh.meshnode_seq restart WITH 4;

insert into worldmesh.meshline values(1, 1, 2);
insert into worldmesh.meshline values(2, 2, 3);
insert into worldmesh.meshline values(3, 3, 1);

alter sequence worldmesh.meshline_seq restart WITH 4;
