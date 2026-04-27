# TAGLINE

rename an existing container

# TLDR

**Rename a container**

```docker container rename [old_name] [new_name]```

# SYNOPSIS

**docker** **container** **rename** _container_ _new_name_

# DESCRIPTION

**docker container rename** renames a container to a new name without affecting its configuration, state, or data. This operation works on both running and stopped containers, allowing you to correct naming mistakes or reorganize your container naming scheme.

Container names must be unique on the Docker host. Renaming does not modify the container's ID or any other attributes beyond the name itself. The shorthand alias **docker rename** is equivalent.

# CAVEATS

Other containers that referenced the old name via Docker DNS or the legacy `--link` flag will continue to use the old name until reconnected or restarted. Compose-managed containers should be renamed through Compose configuration to keep state consistent.

# SEE ALSO

[docker-rename](/man/docker-rename)(1), [docker-container](/man/docker-container)(1)
