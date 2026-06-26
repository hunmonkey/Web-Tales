# Tales on Unraid — Install, Backup, Upgrade and Rollback (SV-702)

This guide covers running the Tales / Web-Tales container on Unraid. It targets
the image produced by SV-701: server port `25600`, a `/config` volume, non-root
runtime (UID/GID `1000`), an `/actuator/health` health check, and
`org.opencontainers.image.version` / `.revision` labels.

> The original media library can be mounted **read-write** when admins need to
> delete duplicate source files from Media > Duplicate Files. Mount it
> read-only only if you prefer to protect source media and will not use that
> delete action.

## 1. Volumes and permissions

| Container path | Purpose | Mode | Suggested Unraid host path |
| --- | --- | --- | --- |
| `/config` | App data: SQLite DB, thumbnails, settings | read-write | `/mnt/user/appdata/Web-Tales` |
| `/data` (or your own) | Book library (CBZ/ZIP/PDF/TXT/image folders) | **read-write for duplicate deletion** | `/mnt/user/media/books` |

Permissions:

- The container runs as **UID/GID 1000** (non-root). The `/config` host directory
  must be writable by `1000`. On Unraid the default owner is `nobody:users`
  (`99:100`), so either:
  1. `chown -R 1000:1000 /mnt/user/appdata/Web-Tales`, or
  2. set the share/dir permissions so `1000` can read/write.
- Mount the media share **read-write** when you want duplicate file deletion to
  remove source files. The container user must have write permission on that
  share.
- Mount the media share **read-only** (`:ro`) only when source files must never
  be modified. In that mode, duplicate file deletion will fail with a writable
  volume error.

## 2. Container settings (Unraid template / docker run)

- **Repository**: your published image, pinned by version or SHA tag
  (see §5 for why pinning matters).
- **Network**: bridge.
- **Port**: host `25600` → container `25600` (WebUI URL `http://<unraid-ip>:25600`).
- **Restart policy**: `unless-stopped`.
- **Health check**: built into the image (`/actuator/health`); Unraid shows the
  container health once Docker reports it.

`docker run` equivalent:

```bash
docker run -d \
  --name Web-Tales \
  -p 25600:25600 \
  -v /mnt/user/appdata/Web-Tales:/config \
  -v /mnt/user/media/books:/data \
  --restart unless-stopped \
  <your-image>:<version-or-sha>
```

## 3. appdata backup

All persistent state lives under `/config`, so backing up the appdata share is
sufficient.

1. **Stop the container** (clean SQLite state) — recommended before a backup.
2. Back up `/mnt/user/appdata/Web-Tales` (the **CA Appdata Backup/Restore** plugin
   automates stop → archive → start on a schedule).
3. Restore by stopping the container, replacing `/config` contents, and starting
   it again.

Keep at least one backup taken **before** each upgrade so you can roll back the DB
together with the image (see §5).

## 4. Reverse proxy

Tales already sets `server.forward-headers-strategy: framework`, so it honours
`X-Forwarded-*` headers from a proxy.

- **Nginx Proxy Manager / SWAG**: proxy `https://reader.example.com` →
  `http://<unraid-ip>:25600`.
- Forward the standard proxy headers (`Host`, `X-Forwarded-For`,
  `X-Forwarded-Proto`).
- Allow streaming responses (do not buffer) and a generous read timeout, since the
  reader streams page images and text.
- Increase `client_max_body_size` if you use any upload-capable admin features.

Minimal Nginx `location` example:

```nginx
location / {
    proxy_pass http://<unraid-ip>:25600;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_buffering off;
    proxy_read_timeout 300s;
}
```

## 5. Upgrade and rollback

The image is labeled with `org.opencontainers.image.version` and `.revision`
(Git SHA), so you can always identify and pin an exact build:

```bash
docker image inspect <your-image>:<tag> \
  --format '{{json .Config.Labels}}'
```

**Upgrade**

1. Take an appdata backup (§3) — this captures the current DB state.
2. Note the currently running tag/SHA (for rollback).
3. Pull the new tag and recreate the container (Unraid: *Force update* or apply the
   new tag in the template).
4. Watch the logs (§6) until health is reported and the WebUI loads.

**Rollback**

1. Stop the container.
2. **Restore the appdata backup taken before the upgrade.** This is required: the
   DB uses Flyway **forward-only** migrations, so a newer DB schema may not work
   with an older image. Rolling back the image without restoring the matching
   appdata can fail to start.
3. Recreate the container using the previously noted older tag/SHA.

> Always pin by version or SHA (not `latest`) so the running build is reproducible
> and a rollback target is unambiguous.

## 6. Log collection

- **Live logs**: `docker logs -f Web-Tales`, or the Unraid Docker tab → container
  → *Logs*.
- Tales writes application logs to stdout (captured by Docker) and, for
  deeper diagnostics, under `/config` (e.g. `logback`/Spring log output if enabled).
- Do not enable verbose logging of request bodies or credentials; per project
  security rules, tokens, raw content and full media paths must not be logged.
- For persistent/rotated logs, ship the container's stdout to your log stack
  (e.g. the Unraid syslog server or a Loki/Promtail sidecar).
