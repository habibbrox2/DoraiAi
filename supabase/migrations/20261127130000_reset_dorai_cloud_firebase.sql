-- Dorai Cloud schema reset for Firebase Auth (destructive for Dorai cloud tables only).
-- Applied: 2026-11-27
begin;
drop table if exists public.usage_events cascade;
drop table if exists public.settings_snapshots cascade;
drop table if exists public.vault_records cascade;
drop table if exists public.memories cascade;
drop table if exists public.devices cascade;
drop table if exists public.profiles cascade;

create table public.profiles (
  id text primary key,
  display_name text,
  plan text not null default 'free',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.devices (
  id uuid primary key default gen_random_uuid(),
  user_id text not null,
  platform text not null,
  label text not null default '',
  push_token text,
  last_seen_at bigint,
  created_at bigint not null default (extract(epoch from now()) * 1000)::bigint,
  unique (user_id, platform, label)
);

create table public.memories (
  id uuid primary key default gen_random_uuid(),
  user_id text not null,
  key text not null,
  category text not null default 'LEARNINGS',
  content_ciphertext text not null,
  content_iv text not null,
  embedding vector(384),
  hit_count integer not null default 1,
  source text,
  created_at bigint not null default (extract(epoch from now()) * 1000)::bigint,
  updated_at bigint not null default (extract(epoch from now()) * 1000)::bigint,
  unique (user_id, key)
);

create table public.vault_records (
  id uuid primary key default gen_random_uuid(),
  user_id text not null,
  device_id uuid references public.devices(id) on delete set null,
  payload_version integer not null default 1,
  iv text not null,
  ciphertext text not null,
  checksum text not null,
  updated_at bigint not null default (extract(epoch from now()) * 1000)::bigint
);

create table public.settings_snapshots (
  id uuid primary key default gen_random_uuid(),
  user_id text not null unique,
  iv text not null,
  ciphertext text not null,
  checksum text not null,
  updated_at bigint not null default (extract(epoch from now()) * 1000)::bigint
);

create table public.usage_events (
  id bigint generated always as identity primary key,
  user_id text not null,
  kind text not null,
  units integer not null default 1,
  created_at timestamptz not null default now()
);

create index memories_user_idx on public.memories(user_id);
create index memories_embedding_hnsw on public.memories using hnsw (embedding vector_cosine_ops);
create index vault_records_user_idx on public.vault_records(user_id, updated_at desc);
create index usage_events_user_idx on public.usage_events(user_id, created_at desc);

alter table public.profiles enable row level security;
alter table public.devices enable row level security;
alter table public.memories enable row level security;
alter table public.vault_records enable row level security;
alter table public.settings_snapshots enable row level security;
alter table public.usage_events enable row level security;

create policy "profiles own row" on public.profiles for all to authenticated
  using ((select auth.jwt()->>'sub') = id) with check ((select auth.jwt()->>'sub') = id);
create policy "devices own rows" on public.devices for all to authenticated
  using ((select auth.jwt()->>'sub') = user_id) with check ((select auth.jwt()->>'sub') = user_id);
create policy "memories own rows" on public.memories for all to authenticated
  using ((select auth.jwt()->>'sub') = user_id) with check ((select auth.jwt()->>'sub') = user_id);
create policy "vault own rows" on public.vault_records for all to authenticated
  using ((select auth.jwt()->>'sub') = user_id) with check ((select auth.jwt()->>'sub') = user_id);
create policy "settings own row" on public.settings_snapshots for all to authenticated
  using ((select auth.jwt()->>'sub') = user_id) with check ((select auth.jwt()->>'sub') = user_id);
create policy "usage own rows" on public.usage_events for all to authenticated
  using ((select auth.jwt()->>'sub') = user_id) with check ((select auth.jwt()->>'sub') = user_id);

create or replace function public.match_user_memories(
  p_query_embedding vector,
  p_match_threshold double precision default 0.5,
  p_match_count integer default 5
)
returns table(
  id uuid, key text, category text, content_ciphertext text, content_iv text,
  similarity double precision, source text, hit_count integer, updated_at bigint
)
language sql stable set search_path to 'public'
as $function$
  select m.id, m.key, m.category, m.content_ciphertext, m.content_iv,
         1 - (m.embedding <=> p_query_embedding) as similarity,
         m.source, m.hit_count, m.updated_at
  from public.memories m
  where m.user_id = (select auth.jwt()->>'sub')
    and m.embedding is not null
    and 1 - (m.embedding <=> p_query_embedding) >= p_match_threshold
  order by m.embedding <=> p_query_embedding
  limit p_match_count;
$function$;

commit;
