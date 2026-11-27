-- Firebase Auth migration: convert Supabase-auth UUID user IDs to text IDs.
-- Firebase JWT subject (sub) is a string, not a UUID. Existing data is preserved
-- via ::text casts. RLS policies move from auth.uid() to auth.jwt()->>'sub'.
-- Applied: 2026-11-27 via Supabase MCP execute_sql.

begin;

-- Drop RLS policies first (they reference user_id/id types).
drop policy if exists "profiles own row" on public.profiles;
drop policy if exists "devices own rows" on public.devices;
drop policy if exists "memories own rows" on public.memories;
drop policy if exists "vault own rows" on public.vault_records;
drop policy if exists "settings own row" on public.settings_snapshots;
drop policy if exists "usage own rows" on public.usage_events;

-- Drop foreign keys to auth.users.
alter table public.profiles drop constraint if exists profiles_id_fkey;
alter table public.devices drop constraint if exists devices_user_id_fkey;
alter table public.memories drop constraint if exists memories_user_id_fkey;
alter table public.vault_records drop constraint if exists vault_records_user_id_fkey;
alter table public.settings_snapshots drop constraint if exists settings_snapshots_user_id_fkey;
alter table public.usage_events drop constraint if exists usage_events_user_id_fkey;

-- Convert user identity columns to text for Firebase sub compatibility.
alter table public.profiles alter column id type text using id::text;
alter table public.devices alter column user_id type text using user_id::text;
alter table public.memories alter column user_id type text using user_id::text;
alter table public.vault_records alter column user_id type text using user_id::text;
alter table public.settings_snapshots alter column user_id type text using user_id::text;
alter table public.usage_events alter column user_id type text using user_id::text;

-- Recreate RLS policies using JWT subject claim (Firebase sub / Supabase sub).
create policy "profiles own row" on public.profiles
  for all to authenticated
  using ((select auth.jwt()->>'sub') = id)
  with check ((select auth.jwt()->>'sub') = id);

create policy "devices own rows" on public.devices
  for all to authenticated
  using ((select auth.jwt()->>'sub') = user_id)
  with check ((select auth.jwt()->>'sub') = user_id);

create policy "memories own rows" on public.memories
  for all to authenticated
  using ((select auth.jwt()->>'sub') = user_id)
  with check ((select auth.jwt()->>'sub') = user_id);

create policy "vault own rows" on public.vault_records
  for all to authenticated
  using ((select auth.jwt()->>'sub') = user_id)
  with check ((select auth.jwt()->>'sub') = user_id);

create policy "settings own row" on public.settings_snapshots
  for all to authenticated
  using ((select auth.jwt()->>'sub') = user_id)
  with check ((select auth.jwt()->>'sub') = user_id);

create policy "usage own rows" on public.usage_events
  for all to authenticated
  using ((select auth.jwt()->>'sub') = user_id)
  with check ((select auth.jwt()->>'sub') = user_id);

-- Harden memory matching: restrict to the authenticated JWT subject.
create or replace function public.match_user_memories(
  p_query_embedding vector,
  p_match_threshold double precision default 0.5,
  p_match_count integer default 5
)
returns table(
  id uuid,
  key text,
  category text,
  content_ciphertext text,
  content_iv text,
  similarity double precision,
  source text,
  hit_count integer,
  updated_at bigint
)
language sql
stable
set search_path to 'public'
as $function$
  select m.id,
         m.key,
         m.category,
         m.content_ciphertext,
         m.content_iv,
         1 - (m.embedding <=> p_query_embedding) as similarity,
         m.source,
         m.hit_count,
         m.updated_at
  from public.memories m
  where m.user_id = (select auth.jwt()->>'sub')
    and m.embedding is not null
    and 1 - (m.embedding <=> p_query_embedding) >= p_match_threshold
  order by m.embedding <=> p_query_embedding
  limit p_match_count;
$function$;

-- Harden legacy match_memories search_path without rewriting body (table may be absent).
do $do$
begin
  if exists (
    select 1 from pg_proc p
    join pg_namespace n on n.oid = p.pronamespace
    where n.nspname = 'public' and p.proname = 'match_memories'
  ) then
    execute 'alter function public.match_memories(vector, double precision, integer) set search_path = ''public''';
  end if;
end $do$;

commit;
