# BetterTPA4Fabric
Yet another TPA mod for Fabric.

# Commands
- [x] tpa {target}
- [x] tpahere {target}
- [x] tpaback
- [x] tpaaccept [sender]
- [x] tpadeny [sender]
- [x] tpaallow [true|false]
- [x] tpaabout
- [x] tpacancel {target}
- [ ] tpaconfig {key} {value}

> { } is required, [ ] is optional

# Config
| Key | Default | Description |
|-----|---------|-------------|
| tpaExpireTime | 120s | How long before a request expires |
| tpaTeleportTime | 5s | Countdown before teleporting |
| tpaCooldown | 5s | Cooldown between TPA requests (**deprecated**) |
| tpaRequestLimit | 99 | Max pending requests per player (**deprecated**) |
| oneTimeTPABack | false | Whether /tpaback can only be used once per teleport |
| resetTimerOnMove | false | Whether moving resets the teleport countdown |

# To-do
- [x] `/tpacancel`
- [x] `/tpaallow`
- [ ] `/tpaconfig` / runtime config modification
- [ ] Formatted messages
- [ ] Teleport effects
