DROP DATABASE IF EXISTS zukschema;
CREATE DATABASE zukschema CHARACTER SET utf8 COLLATE utf8_general_ci;
DROP TABLE IF EXISTS schemainfo;
CREATE TABLE schemainfo (
  schemaid varchar(32) NOT NULL,
  usercode varchar(32) NOT NULL,
  schemaname varchar(128) NOT NULL,
  orginschema text NOT NULL,
  targetschema text NOT NULL,
  databasekind varchar(15) NOT NULL,
  databasename varchar(30) NOT NULL,
  createdtime varchar(20) NOT NULL,
  lastupdate varchar(20) NOT NULL,
  tablemap text,
  PRIMARY KEY ('schemaid')
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

