# BetterTPA4Fabric
Yet another TPA mod for Fabric.

# Commands
- [x] tpa {to}
- [x] tpahere {target}
- [x] tpaback
- [x] tpaaccept [from]
- [x] tpadeny [from]
- [ ] tpacancel [to]
- [ ] tpaallow [true|false]
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
- [ ] `/tpacancel`
- [ ] `/tpaallow`
- [ ] `/tpaconfig` / runtime config modification
- [ ] Formatted messages
- [ ] Teleport effects
