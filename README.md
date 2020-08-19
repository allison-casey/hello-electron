# DnD Combat Tracker
Generic combat tracker for AP based DnD systems.

## Features
- [x] Yaml character templates
- [x] Per character AP counts
- [x] Track ability cooldowns in a round based timeline
- [x] Supports round based and interleaved (every turn) cooldowns
- [x] Abilities know when they can be used (enough AP, character alive, not on cooldown)
- [x] Copy pre-calculated accuracy and damage rolls to clipboard in roll20 syntax
- [x] Per character manual AP overrides
- [x] Track character health and deaths
- [x] Faction Colors
- [x] Specify custom template directory
- [ ] Track turn order in combat tracker + advance turn button
- [ ] Reusable abilities/passives
- [ ] Documentation: how to use, template syntax, etc
- [ ] Release binary
- [ ] Host room for players to view tracker with DM
- [ ] Spiffy animations ¬‿¬

## Development
ClojureScript + Shadow-cljs + Electron + Reagent

### How to Run
```
npm install electron-prebuilt -g
npm install shadow-cljs -g
npm install

npm run dev
electron .
```

### Release
```
npm run build
electron-packager . HelloWorld --platform=darwin --arch=x64 --version=1.4.13
```
