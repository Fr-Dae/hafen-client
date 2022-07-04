# Zeecow hafen-client

Client focusing on small helper tasks, many using mouse middle button, and other stuff:

 - search actions globally
 - sort actions by most used craft items
 - craft window buttons for ingredients and history
 - bigger close buttons
 - button for auto-organizing duplicate windows
 - simple window and buttons
 - compact equip window
 - auto toggle equip window
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
 - rightclicking doors, cave, gate, wheelbarrow, stockpiles, horse, cart, will interact with these objects whenever possible
 - midclick Barter Stand "Buy" button to auto-buy  
 - midclick ground 
   - while holding item will move to location (simulates ctrl+click)
   - while inspecting icon active, will msg tile name
 - long midclick ground
   - inspect water tile using inventory woodencup  
   - while mounting horse will try to dismount
   - while carrying wheelbarrow will try to(dismount and) unload stockpile at ground
 - midclick object 
   - inspect object quality and show text
   - harvest 2 dreams from dreamcatch obj
   - Giddyup! if obj is horse
   - lightup torch on firesource obj
   - open "Cargo" on Knarr/Snekkja obj
   - toggle barrels labels 
   - toggle mine support radius 
   - toggle aggressive animal obj radius
   - while holding item will try to ctrl+click obj(containers,stockpiles)
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
 - midclick item 
   - sort-transfer item ascending(?) 
   - pick-up all
   - quick-equip from belt
   - seed-planting cursor
   - while holding cheese-curd will fill up cheese-tray item
 - long midclick item
   - sort-transfer item descending(?)
   - show context menu for various items (details below)
   - sort-transfer inverse
   - equip fishing hook/lure and recast
   - equip two sack items from belt
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