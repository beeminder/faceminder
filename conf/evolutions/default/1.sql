# --- Created by Slick DDL
# To stop Slick DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table "Convo" ("id" VARCHAR(254) PRIMARY KEY NOT NULL,"goal" INTEGER NOT NULL,"expiry" BIGINT NOT NULL);
create table "Goal" ("id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"plugin" VARCHAR(254) NOT NULL,"owner" INTEGER NOT NULL,"slug" VARCHAR(254) NOT NULL,"title" VARCHAR(254) NOT NULL,"lastUpdated" BIGINT NOT NULL,"options" VARCHAR(254) NOT NULL);
create table "Service" ("id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"provider" VARCHAR(254) NOT NULL,"username" VARCHAR(254) NOT NULL,"token" VARCHAR(254) NOT NULL,"expiry" BIGINT);
create table "User" ("id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"username" VARCHAR(254) NOT NULL,"goals" VARCHAR(254) NOT NULL,"bee_service" INTEGER NOT NULL,"fb_service" INTEGER);

# --- !Downs

drop table "Convo";
drop table "Goal";
drop table "Service";
drop table "User";

