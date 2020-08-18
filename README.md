# DnD Combat Tracker
Generic combat tracker for AP based DnD systems.

## Features
- Yaml character templates
- Per character AP counts
- Track ability cooldowns in a round based timeline
- Supports round based and interleaved (every turn) cooldowns
- Abilities know when they can be used (enough AP, character alive, not on cooldown)
- Copy pre-calculated accuracy and damage rolls to clipboard in roll20 syntax
- Per character manual AP overrides
- Track character health and deaths
- Faction Colors

## Todo
- [ ] Specify custom template directory
- [ ] Track turn order in combat tracker + advance turn button
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
