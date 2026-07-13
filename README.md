# AuthorisedKeysMC

### Work in progress!!!

Minecraft authentication mod and plugin using asymmetric key authentication.

Pretty much eliminates the need for punching in passwords when joining, which makes it quite convenient to use.

The downside is that **the mod is required on both server and client**.

Currently, the mod supports Minecraft 26.2 on the following platforms, in order of priority:

- Fabric
- Neoforge
- Paper
- ~~Forge~~ (currently broken right now)

## Features

- Passwordless authentication
- Players do not spawn until the full login process is complete.
- Cross-loader play (i.e., Neoforge client can join Fabric server, provided there are no other conflicts).
- Create and use multiple key pairs. (optional)
- Password encryption for private keys (optional)
- When using encrypted private keys, passwords are neither stored on disk nor sent over the network.
- Host key verification (via trust-on-first-use)
- Bind multiple keys to a given username on the server. (configurable max limit)
- ID aliasing per username, i.e. UUID spoofing. *Use with caution.*

## Compatibility Warning

This mod/plugin is a bit invasive, as the Fabric, Neoforge, and Forge versions make use of several Mixins, particularly in `ServerLoginPacketListenerImpl`.

In the Paper version, a custom handler is inserted into the netty channel pipeline for the duration of the login, and the plugin itself makes use of reflection.

That said, I have found that ViaVersion, ViaFabric, and ViaBackwards somehow work.

Expect things to break if there are also other mods/plugins that touch these areas.

## Server Configuration

The following config options are available:

- `enforcing`: `true` (default) or `false`

  Whether AKMC will run the authentication flow or not.

- `registration_required`: `true` or `false` (default)

  Whether players joining must have at least one key bound in order to log-in.

- `allow_registration`: `true` (default) or `false`
  
  Whether players joining can bind a key themselves.
  If `false`, a server administrator will have to bind the players' keys on their behalves.

  *If, at the same time, `registration_required` is `true`, then players without any keys cannot join at all,*
  *until a server administrator binds their key for them.*

- `match_player_list_by_name`: `true` (default) or `false`

  **Only in Fabric/Neoforge/Forge servers. Currently does not work on Paper.**

  Whether to use the username instead of the UUID when comparing profiles against the whitelist, ban list, or operator list.

- `max_key_count`: any unsigned integer, `100` by default

  The maximum number of public keys that can be bound to a particular username.

- `login_timeout_ticks`: any unsigned integer, `1200` by default

  Amount of ticks that the authentication process may last, at maximum. The client will be disconnected after this amount of ticks passes.

  By default, the timeout is 1200 ticks (60 seconds).

- `kick_message`: string, left blank by default

  A custom kick message, if any, to show to clients connecting without the AKMC mod installed on their side.

## Building (TODO)

1. Clone the repository and enter the project directory.
2. Run `./gradlew build` on Linux/Mac and `.\gradlew.bat build` on Windows.
3. Hopefully it should build just fine.
4. The compiled JARs can be found in:

   - Fabric: `fabric/build/libs/AuthorisedKeysMC-fabric-$VERSION-shadow.jar`
   - Neoforge: `neoforge/build/libs/AuthorisedKeysMC-neoforge-$VERSION.jar`
   - ~~Forge: TODO~~
   - Paper: `paper/build/libs/AuthorisedKeysMC-PAPER-$VERSION.jar`

TODO

## Licence

[LGPL-3.0-only](LICENSE)
