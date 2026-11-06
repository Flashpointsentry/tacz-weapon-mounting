# Changelog

## 1.12
- **Move to dismount:** pressing a movement key or sprint stands you up, like Call of Duty. Keys already held when you mount are ignored until released. Prone players keep crawling. Client setting `moveToDismount`
- **Corner lean:** corner mounts tilt your first-person view out toward the open side (8° by default, client setting `cornerLeanDegrees`, 0 = off). Visual only
- **Smooth mounting:** if you mount while looking outside the allowed range, the view now glides into place instead of snapping
- Network version bumped: server and clients must both run 1.12


## 1.11
- **Any TACZ gun can now mount on corners.** Mounting on top of cover and prone still needs a bipod (gun list or bipod attachment)
- New server setting `corner.anyGun` (on); turn it off to limit corners to bipod guns too
- Removing a bipod attachment while mounted on cover or prone now knocks you off; corner mounts stay
- `/bipod` messages and `/bipod list` now talk about "having a bipod"
- Network version bumped: server and clients must both run 1.11


## 1.10.1
- Author set to Flashpointsentry (shown in the Forge mods list)
- Licensed under GPL-3.0 (LICENSE file included)
- Build now deletes the leftover `-slim` jar, so `build/libs` only has the jar to install
- Server reuses its brace rules instead of rebuilding them every tick for every braced player
- Removed a leftover unused parameter

## 1.10
- **Braced hip-fire accuracy:** once settled, hip-firing while braced on top uses the gun's lying-down spread instead of crouching spread (TACZ default 2.5 vs 3.5). Aiming down sights is unchanged
- Corners keep crouching spread by default (`corner.hipfireAccuracy` to turn it on); prone is already lying-down spread
- New server settings `wall.hipfireAccuracy` (on) and `corner.hipfireAccuracy` (off)

## 1.9
- Prompts now fade out after 5 seconds (over 1.5 seconds) so they're not in the way; they show again whenever the prompt changes, e.g. stepping into a new spot, settling into a brace, or standing up
- New client settings `hintVisibleSeconds` and `hintFadeSeconds` (0 = never fade)

## 1.8
- **Toggle Brace key** (B by default, in Controls under "TACZ Bipod"): press to brace and stay braced whether or not you aim down sights, press again to stand. Hold-aim bracing still works; unbind the key to use only hold-aim
- **Hits knock you off:** a single hit of 4+ damage after armor (2 hearts) ends your brace (`general.knockOffDamage`, 0 = off). Prone players stay crawling; only the bipod folds
- Prompt now shows both options, e.g. "Hold [Right Button] or press [B] to brace", and how to stand up based on how you braced

## 1.7
- New tested defaults: corner `yawTowardWall` 10 → 12, corner `edgeReach` 0.5 → 0.35 (all other defaults unchanged)

## 1.6
- Walking into a brace spot while already aiming no longer braces you automatically; the prompt shows and you press aim again to brace
- After being knocked off a brace (jump, block broken, pushed), the prompt now shows normally instead of hiding until you let go of aim

## 1.5
- Fixed: standing very close to a fence or wall (partly inside its block space) hid the prompt
- Fixed: you could brace with a big gap between you and the block, or while looking well off to the side
- Bracing now uses a short ray along where you're looking, so you must face the block you brace on (within 35°, `general.maxFacingAngle`)
- Allowed gap between your body and the block is now 0.2 blocks (`wall.maxGap`, replaces `maxReach`)
- Corners now need you closer to the edge (`corner.edgeReach` 0.5, replaces `cornerReach`) and facing roughly straight ahead
- Network version bumped: server and clients must both run 1.5

## 1.4
- **Deploy delay:** bracing takes a moment to settle (0.5s by default, `general.deployTicks`) with a clunk sound nearby players can hear; the recoil bonus starts once settled
- **Prone bipod:** crawl (TACZ crawl) with a braceable gun and hold aim to deploy on the ground; stacks an extra recoil multiplier on top of TACZ's crawl recoil (0.7 by default) and limits turning to ±30°
- **Fences, walls and closed fence gates** now count as brace surfaces (uses the vanilla tags, so most modded ones work; `general.allowFencesAndWalls`)
- **Corner bracing:** lean the gun on the edge of a wall end, pillar or door frame at head height; gives half the normal recoil reduction by default (`corner.recoilStrength`) and limits turning toward the wall to 10°
- Prompt no longer mentions jumping (jumping still stands you up); it now shows "Bracing..." while settling and which kind of brace you're in
- Server config is now split into `general`, `wall`, `corner` and `prone` sections. Values from older versions reset to their defaults, so re-apply any you had changed
- Editing the server config while the server runs now syncs to players automatically
- Network version bumped: server and clients must both run 1.4

## 1.3
- Attachment allowlist: mark an attachment (like a bipod grip) and any gun with it installed or built in can brace
- New commands: `/bipod attachment addheld | removeheld | add <id> | remove <id>`
- `/bipod list` now shows both lists; `allowed_guns.json` gains an `attachments` array (old files still load)
- Network version bumped: server and clients must both run 1.3

## 1.2
- Bracing now uses TACZ's aim button (right click by default) instead of the B key: hold aim while in position to brace, release to stand up
- Follows TACZ's own aim setting, so a rebound aim key or toggle-aim works too
- Jump still stands you up; after being knocked off while aiming, release and aim again to re-brace
- Removed the separate "Brace Weapon" keybind

## 1.1
- Brace prompt moved from under the crosshair to just above the hotbar (adjustable with `hintHeight` in `config/taczbipod-client.toml`)
- Braced players can now only look 30° up / 20° down from level (`pitchUp` / `pitchDown` in the server config)
- Network version bumped: server and clients must both run 1.1

## 1.0
- First version: brace allowlisted TACZ guns on a full block, forced crouch, ±45° turn limit, crawl recoil multiplier, `/bipod` allowlist commands
