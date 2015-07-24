These are the various binaries (cross-compiled for arm) pulled from SSHelper which we need for sshd and rsync.
busybox was symlinked to ash, which is apparently the shell sshelper uses.

mksh was copied from /system/bin, and was symlinked to sh.