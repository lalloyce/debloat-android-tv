# Security

The launcher holds two permissions (`INTERNET`, `READ_TV_LISTINGS`) and sends nothing off the TV except YouTube API requests and thumbnail downloads. The scripts run commands on a TV over adb, so a flaw in them (for example a way for a package list to run a command) is a security issue.

If you find one, please report it privately: use the repository's "Report a vulnerability" option on the Security tab if it is enabled. If it is not, open an issue that says you have a security report and gives no details, and a maintainer will arrange a private channel.

Never post an API key, a signing key or a TV's address in an issue. If you committed a key by mistake, revoke it first.
