-- V1: Création des tables de projection simplifiée du module profile

create table if not exists profile_entry (
	profile_id varchar(255) primary key,
	total_experience integer not null,
	wins integer not null,
	losses integer not null,
	draws integer not null,
	win_streak integer not null,
	loss_streak integer not null,
	draw_streak integer not null,
	created_at timestamptz not null,
	updated_at timestamptz not null
);

create index if not exists idx_profile_total_xp on profile_entry(total_experience);
create index if not exists idx_profile_wins on profile_entry(wins);

create table if not exists profile_topic_statistics_entry (
	profile_id varchar(255) not null,
	topic_id varchar(255) not null,
	total_experience integer not null,
	wins integer not null,
	losses integer not null,
	draws integer not null,
	primary key (profile_id, topic_id),
	constraint fk_profile_topic_stats_profile
		foreign key (profile_id) references profile_entry(profile_id)
);

create table if not exists profile_game_entry (
	profile_id varchar(255) not null,
	position_idx integer not null,
	game_id varchar(255) not null,
	topic_id varchar(255) not null,
	opponent_id varchar(255) not null,
	opponent_name varchar(255),
	player_score integer not null,
	opponent_score integer not null,
	result_type varchar(20) not null,
	played_at timestamptz not null,
	primary key (profile_id, position_idx),
	constraint fk_profile_game_profile
		foreign key (profile_id) references profile_entry(profile_id)
);

create index if not exists idx_profile_game_profile_played_at
	on profile_game_entry(profile_id, played_at desc);
