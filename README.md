# Zeecow hafen-client

Client focusing on small helper tasks, many using mouse middle button, and other stuff:

- flowermenu works onmouseup 
- search actions globally
- sort actions by most used craft items
- craft window buttons for ingredients and history
- window buttons auto-organize duplicates, auto-hide  
- simple window and buttons
- compact equip window, auto toggle equip window
- shorter cattle roster window
- reposition map window when compacted/expanded
- basic inventory item counter
- tamed animal name generator, using animal's stats 
- auto-pile helper for mulberry leaves, woodblocks, boards, coal, ground stones
- seed-farmer manager: no pathfinding, buggy (ctrlLongMidClick crop)
- cook manager: auto cook pepper and refill cauldron until barrel is empty(no pathfinding)
- icon list categs
- shaped icons options
- label trasfering barrels
- Window fuel buttons for Oven, Kiln, Smelter
- keys up/down control volume
- scroll transfer items directly (no shift)
- free gob placement
- auto toggle gridlines 
- rightclick interactions involving various vehicles and workstations, entrances, containers
- Quick Options window show 3 latest used options, and auto-click for latest flowermenu
- default icon sound to "Bell 2" if empty
- inspect cursor tooltip containing gob/tile/minimap info
- shift+tab toggles between belt, creel and basket
- option Ctrl+click petal confirmation list (default Empty, Swill)
- Barter Stand "allow mid-click auto-buy" checkbox
- Barter Stand button return branches to closest wooden chest
- Audio msg mute list
- lift vehicle before travel hearth
- midclick mark expands minimap, center mark, highlight mark
- new mark zooms map for precision
- "ctrl+q" show window pickup gobs ("q" pick closest, "shift+q" pick all similar)
- area piler: create piles around selected area (long-midclick item)
- mining tile counter: long midclick mine ground or tiles to show window
- main inventory checkboxes for midclick transfer modes: asc, desc, one, ql
- gob monitor: highlight, play audio, text to speech (cmd festival)
- Feasting log window, count gains
- Option "Fish Moon XP" show text over calendar 
- Scroll text entry fuel for kiln, oven, smelter
- (unfinished) hover menu on search actions button 
- (unfinished) Lag Camera (:cam lag)

- midclick ground 
  - while holding item will move to location (simulates ctrl+click)
  - while inspecting icon active, will msg tile name
  - place stockpile and all items
  - dig multiple ballclay if cursor dig
- long midclick ground (LMC)
  - inspect water tile using inventory woodencup  
  - while mounting horse will try to dismount
  - while carrying wheelbarrow will try to(dismount and) unload stockpile at ground
  - while coracle equipped, LMC water will try to drop and mount
  - while coracle mounted, LMC ground will try to dismount and equip 
  - disembark dugout, rowboat, coracle, kicksled
  - activate snow clear area
  - show mining tile counter when click mine ground/tile
- midclick object 
  - inspect object quality and show text
  - harvest 2 dreams from dreamcatch objects closeby
  - Giddyup! if obj is horse
  - lightup torch on firesource obj
  - open "Cargo" on Knarr/Snekkja obj
  - toggle barrels labels 
  - toggle mine support radius 
  - toggle aggressive animal obj radius
  - while holding item will try to ctrl+click obj(containers,stockpiles)
  - open cauldron
- long midclick object
  - show context menu for various objs (details below)
  - lift up obj 
  - take all from stockpile obj
  - click "Man the helm" on Knarr/Snekkja obj
  - while holding item will try to store all on barrel obj
  - add/remove tree obj to "Remove all trees" ordered queue
  - add/remove treelog obj to "Destroy all" ordered queue
  - while carrying wheelbarrow will try to(dismount and) unload at stockpile/container
  - while driving wheelbarrow will lift it up and open gate obj
  - while driving wheelbarrow will lift it up and place on cart obj
  - while carrying/driving wheelbarrow will mount horse obj and lift wheelbarrow again
  - show window for area piler
  - pile all clay if clicked pile with cursor dig
  - pile inv board/block/stone if clicked pile while working, then try getting more items
  - put out cauldron
- midclick item 
  - sort-transfer item ascending(?) 
  - pick-up all
  - quick-equip from belt
  - seed-planting cursor
  - while holding cheese-curd will fill up cheese-tray item
  - undo stack item (ctrl+shift+rclick) if transfer not available
  - create single stack when hold and midclick same item type
- long midclick item
  - sort-transfer item descending(?)
  - show context menu for various items (details below)
  - equip fishing hook/lure and recast
  - equip two sack items from belt
  - undo multiple stack items if transfer not available
  - create multiple stacks when hold and midclick same item type
- long midclick context menus for objects
  - auto butch dead animal objs
  - start seed farmer for crop objs: harvest, store and replant seeds (no pathfinding)
  - harvest area for crop objs: activate harvest icon (shift+click)
  - add 4 branches to oven obj
  - add 9/12 coal to smelter obj
  - remove tree and stump 
  - remove trellis plant(s)
  - destroy multiple treelogs (if bonesaw is equipped)
- long midclick context menus for items
  - auto butch dead animal items
  - kill all cocoon items
  - feast all food items(requires table)
  - transfer sort items (asc/desc)


other stuff:
 - autodrop mined items, seeds, soil
 - auto click menu options
 - auto drink
 - sort transfer with Alt+Rclick or Alt+Lclick (no gemstone yet)
 - Auto-hearth  
 - replaces arrow cursor with system's default
 - show claims by default
 - optional friend online notification
 - craft window numbers, history, search ingredients
 - highlight damaged gobs more  
 - highlight finished crops, growing trees, gobs and categories
 - zoom extended for ortho cam
 - Shift+c alternates cams ortho/free 
 - mini trees
 - hide smoke and some animations
 - keybelt shortcuts navigation 
 - reposition rightmost windows horiz. when game resize
 - drink key "'"
 - pickup key "q", shift+q pickup all, ctrl+q pickup window
 - Ctrl + right click to confirm remove shortcut 
 