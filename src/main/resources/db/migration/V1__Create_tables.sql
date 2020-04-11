create table NEWS (
  id bigint primary key auto_increment,
  title text,
  content text,
  url varchar(1000),
  created_at timestamp default now(),
  modified_at timestamp default now()
) DEFAULT CHARSET=utf8mb4;

create table VISITED_LINKS (link varchar(1000));

create table UNVISITED_LINKS (link varchar(1000));