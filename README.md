# TACZ Bipod (Forge 1.20.1, TACZ 1.1.8-hotfix)

Lets players brace allowlisted TACZ guns on a full block in front of them.

## How it plays
**Any TACZ gun** can lean on corners. Mounting **on top of cover** and **prone** needs a bipod: the gun
is on the gun list, or has a listed attachment installed. Get in position and A prompt appears
above the hotbar. Two ways to brace, and players can use either:
- **Hold aim** (right click, or whatever TACZ's aim key is bound to) to brace; let go to stand up.
- **Press B** (the "Toggle Brace" key, rebindable or unbindable in Controls) to brace and stay braced
  whether you aim or not; press it again to stand up.

 Bracing takes a short moment to settle (with a clunk sound) before the recoil bonus starts.

| Brace | How to get it | Recoil | Turning |
|---|---|---|---|
| **On top** | Face and stand right against (within 0.2 blocks) a full block, fence, wall or closed fence gate with open space above it | Gun's `crawl_recoil_multiplier` (TACZ default 0.5) | ±45°, 30° up, 20° down |
| **Corner** | Stand at the end of a wall, or just behind a pillar or door frame, with clear space straight ahead | Half the normal reduction (0.75x with a 0.5 gun) | 45° away from the wall, 12° toward it |
| **Prone** | Crawl (TACZ crawl key) on the ground | Crawl recoil × 0.7 (0.35x with a 0.5 gun) | ±30°, 15° up, 15° down |

While braced on top or on a corner you're held crouched in place. Hip-firing while braced on top
uses the gun's lying-down spread (`wall.hipfireAccuracy`); corners keep crouching spread unless you
turn on `corner.hipfireAccuracy`. Aiming down sights is unchanged. You
can reload while braced. Pressing a movement key or sprint also stands you up (keys you were already holding when you mounted
are ignored until released). Corner mounts tilt your view out toward the open side. You're knocked off if you take a hit of 4+ damage (2 hearts, after armor; `general.knockOffDamage`), jump, switch items, the block breaks or gets
covered, you get pushed away, stop crawling (prone), or change dimension.

## Managing which guns can brace (ops, permission level 2)
| Command | What it does |
|---|---|
| `/bipod addheld` | Allow the gun in your hand |
| `/bipod removeheld` | Disallow the gun in your hand |
| `/bipod add <gun id>` | Allow by id (tab-complete lists every loaded TACZ gun) |
| `/bipod remove <gun id>` | Disallow by id (tab-complete lists the current list) |
| `/bipod attachment addheld` | Any gun with the attachment in your hand installed can brace |
| `/bipod attachment removeheld` | Undo that for the attachment in your hand |
| `/bipod attachment add <id>` | Same, by attachment id (tab-complete lists every loaded attachment) |
| `/bipod attachment remove <id>` | Remove an attachment from the list |
| `/bipod list` | Show both lists (flags ids from packs that aren't installed) |
| `/bipod reload` | Re-read the file after editing it by hand |

These lists decide which guns have a bipod: a gun counts if it's on the gun list **or** has a listed
attachment installed (built-in attachments count too). Every gun can use corners regardless
(`corner.anyGun` to change that). Both lists live in `config/taczbipod/allowed_guns.json`; the gun list starts
with every TACZ default-pack gun that has a bipod on its model (M249, RPK, FN Evolys, PKM, HCAR,
Mk 14, SCAR Mk20, SPR15HB, AI AWP, MRAD, M95, M107, HM50B) and the attachment list starts empty. Changes sync to everyone online immediately.

Per-world settings live in `serverconfig/taczbipod-server.toml`, split into `general`
(deploy time, fences/walls on/off), `wall`, `corner` and `prone` sections. Every number in the table
above can be changed there, and each type can be turned off. Changes sync to players automatically.

Per-player setting (`config/taczbipod-client.toml`): `hintHeight`, how many pixels above the
bottom of the screen the brace prompt sits (default 82, just above the hearts and action bar);
`hintVisibleSeconds` (default 5) and `hintFadeSeconds` (default 1.5) control how long each prompt
stays before fading out (0 = never fade). A prompt shows again whenever it changes.
`moveToDismount` (default on) and `cornerLeanDegrees` (default 8, 0 = off) are per-player too.

## Building
Everything needed is included. You need Java 17 or newer installed (Java 21 is fine; the build fetches Java 17 for compiling by itself).

- **Windows:** open a terminal in this folder and run `gradlew.bat build`
- **Mac/Linux:** run `./gradlew build`

The first build takes several minutes because it downloads Gradle, Minecraft, Forge and TACZ.
The finished mod is `build/libs/taczbipod-1.13.jar`. Install it on **both** the server and every client.

Or open the folder in IntelliJ IDEA, let it import, and run the `build` task from the Gradle panel.
