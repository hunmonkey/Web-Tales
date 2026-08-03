-- SV-1076 / ADR-0017: remove Kobo Sync and KOReader Sync completely.
-- Forward-only. Restoring this data requires a database backup.

-- Kobo sync bookkeeping. Children first, then the parent table.
DROP TABLE IF EXISTS SYNC_POINT_READLIST_REMOVED_SYNCED;
DROP TABLE IF EXISTS SYNC_POINT_READLIST_BOOK;
DROP TABLE IF EXISTS SYNC_POINT_READLIST;
DROP TABLE IF EXISTS SYNC_POINT_BOOK_REMOVED_SYNCED;
DROP TABLE IF EXISTS SYNC_POINT_BOOK;
DROP TABLE IF EXISTS SYNC_POINT;

-- Roles that no longer exist in the UserRoles enum.
DELETE
FROM USER_ROLE
WHERE ROLE IN ('KOBO_SYNC', 'KOREADER_SYNC');

-- Server settings that no longer have a backing property.
DELETE
FROM SERVER_SETTINGS
WHERE KEY IN ('KOBO_PROXY', 'KOBO_PORT', 'KEPUBIFY_PATH');

-- KOReader partial-MD5 hashing.
ALTER TABLE LIBRARY
    DROP COLUMN HASH_KOREADER;

ALTER TABLE BOOK
    DROP COLUMN FILE_HASH_KOREADER;

-- KEPUB detection, which only existed to serve the Kobo download format.
ALTER TABLE MEDIA
    DROP COLUMN EPUB_IS_KEPUB;
