# --- Created by Slick DDL
# To stop Slick DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table "Dad" ("id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"name" VARCHAR(254) NOT NULL);
create table "Goal" ("id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"owner" INTEGER NOT NULL);
create table "Service" ("id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"owner" INTEGER NOT NULL,"provider" VARCHAR(254) NOT NULL,"token" VARCHAR(254) NOT NULL,"secret" VARCHAR(254) NOT NULL);
create table "User" ("id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"username" VARCHAR(254) NOT NULL,"email" VARCHAR(254) NOT NULL,"encrypted_password" VARCHAR(254) NOT NULL,"goals" VARCHAR(254) NOT NULL,"bee_service" INTEGER NOT NULL,"fb_service" INTEGER NOT NULL);

# --- !Downs

drop table "Dad";
drop table "Goal";
drop table "Service";
drop table "User";

