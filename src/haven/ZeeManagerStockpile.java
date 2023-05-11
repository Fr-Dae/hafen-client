package haven;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static haven.OCache.posres;
import static java.util.Map.entry;

public class ZeeManagerStockpile extends ZeeThread{

    static final String TASK_PILE_GROUND_ITEMS = "TASK_PILE_GROUND_ITEMS";
    static final String TASK_PILE_GOB_SOURCE = "TASK_PILE_GOB_SOURCE";
    static final String TASK_PILE_TILE_SOURCE = "TASK_PILE_TILE_SOURCE";
    static final String STOCKPILE_LEAF = "gfx/terobjs/stockpile-leaf";
    static final String STOCKPILE_BLOCK = "gfx/terobjs/stockpile-wblock";
    static final String STOCKPILE_BOARD = "gfx/terobjs/stockpile-board";
    static final String STOCKPILE_COAL = "gfx/terobjs/stockpile-coal";
    static final String STOCKPILE_SAND = "gfx/terobjs/stockpile-sand";
    static final String STOCKPILE_STONE = "gfx/terobjs/stockpile-stone";
    static final String BASENAME_MULB_LEAF = "leaf-mulberrytree";

    static Map<String, String> mapItemPileRegex = Map.ofEntries(
            entry("gfx/(terobjs/items|invobjs)/flaxfibre", "/stockpile-flaxfibre"),
            entry("gfx/(terobjs/items|invobjs)/hempfibre", "/stockpile-hempfibre"),
            entry("gfx/(terobjs/items|invobjs)/turnip", "/stockpile-turnip"),
            entry("gfx/(terobjs/items|invobjs)/carrot", "/stockpile-carrot"),
            entry("gfx/(terobjs/items|invobjs)/beet", "/stockpile-beetroot"),
            entry("gfx/(terobjs/items|invobjs)/nugget-.+", "/stockpile-nugget-metal"),
            entry("gfx/(terobjs/items|invobjs)/bar-.+","/stockpile-metal"),
            entry("gfx/(terobjs/items|invobjs)/cloth", "/stockpile-cloth"),
            entry("gfx/(terobjs/items|invobjs)/rope","/stockpile-rope"),
            entry("gfx/(terobjs/items|invobjs)/(black)?coal","/stockpile-coal"),
            entry("gfx/(terobjs/items|invobjs)/wblock-.+","/stockpile-wblock"),
            entry("gfx/(terobjs/items|invobjs)/board-.+","/stockpile-board"),
            entry("gfx/(terobjs/items|invobjs)/leaf-.+","/stockpile-leaf"),
            entry("gfx/(terobjs/items|invobjs)/bough-.+","/stockpile-bough"),
            entry("gfx/(terobjs/items|invobjs)/sand","/stockpile-sand"),
            entry("gfx/(terobjs/items|invobjs)/clay-.+","/stockpile-clay"),
            entry("gfx/(terobjs/items|invobjs)/cattail","/stockpile-cattailpart"),
            entry("gfx/(terobjs/items|invobjs)/straw","/stockpile-straw"),
            entry("gfx/(terobjs/items|invobjs)/petrifiedshell","/stockpile-petrifiedshell"),
            entry("gfx/(terobjs/items|invobjs)/hopcones","/stockpile-hopcones"),
            entry("gfx/(terobjs/items|invobjs)/cucumber","/stockpile-cucumber"),
            entry("gfx/(terobjs/items|invobjs)/grapes","/stockpile-grapes"),
            entry("gfx/(terobjs/items|invobjs)/pumpkin","/stockpile-pumpkin"),
            entry("gfx/(terobjs/items|invobjs)/lettucehead","/stockpile-lettuce"),
            entry("gfx/(terobjs/items|invobjs)/plum","/stockpile-plum"),
            entry("gfx/(terobjs/items|invobjs)/apple","/stockpile-apple"),
            entry("gfx/(terobjs/items|invobjs)/pear","/stockpile-pear"),
            entry("gfx/(terobjs/items|invobjs)/quince","/stockpile-quince"),
            entry("gfx/(terobjs/items|invobjs)/lemon","/stockpile-lemon"),
            entry("gfx/(terobjs/items|invobjs)/tobacco-(cured|fresh)","/stockpile-pipeleaves"),
            entry("gfx/(terobjs/items|invobjs)/gems/gemstone","/stockpile-gemstone"),
            entry("gfx/(terobjs/items|invobjs)/bone","/stockpile-bone"),
            entry("gfx/(terobjs/items|invobjs)/feather","/stockpile-feather"),
            //ores
            entry("gfx/(terobjs/items|invobjs)/.+ite","/stockpile-ore"),
            // stones, requires extra code for regular ones
            entry("gfx/(terobjs/items|invobjs)/slag","/stockpile-stone"),
            entry("gfx/(terobjs/items|invobjs)/catgold","/stockpile-stone"),
            entry("gfx/(terobjs/items|invobjs)/quarryartz","/stockpile-stone"),
            // flowers, requires extra code for non poppy
            entry("gfx/(terobjs/items|invobjs)/flower-poppy","/stockpile-poppy"),
            //soil
            entry("gfx/(terobjs/items|invobjs)/(mulch|soil|earthworm)","/stockpile-soil"),
            //trash pile
            entry("gfx/(terobjs/items|invobjs)/(entrails|intestines|pumpkinflesh)","/stockpile-trash")
    );

    static ZeeWindow windowManager;
    static boolean busy;
    static Inventory mainInv;
    static String lastPetalName;
    static Gob gobPile, gobSource;
    static MapView.Plob lastPlob;
    static long lastPlobMs;
    static String lastTileStoneSourceTileName;
    static Coord2d lastTileStoneSourceCoordMc;
    static boolean diggingTileSource;
    static Gob lastTreelogSawed, lastTreelogChopped, lastBoulderChipped;
    static String lastBoulderChippedStoneName;
    private final String task;

    public ZeeManagerStockpile(String task) {
        this.task = task;
        busy = true;
        mainInv = ZeeConfig.gameUI.maininv;
    }

    public static void checkPlob(MapView.Plob plob) {
        lastPlobMs = ZeeThread.now();
        lastPlob = plob;
    }

    @Override
    public void run() {
        try{
            if (task.contentEquals(TASK_PILE_GOB_SOURCE)){
                startPilingGobSource();
            }
            else if (task.contentEquals(TASK_PILE_TILE_SOURCE)){
                startPilingTileSource();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        busy = false;
        ZeeConfig.removeGobText(ZeeConfig.getPlayerGob());
    }


    // gfx/terobjs/stockpile-board
    // gfx/terobjs/stockpile-wblock
    private void startPilingGobSource() throws InterruptedException {

        //find pile
        gobPile = findPile();

        if(gobPile==null) {
            println("pile null");
            return;
        }

        //mark gob pile and source
        ZeeConfig.addPlayerText("piling");
        ZeeConfig.addGobText(gobPile,"pile");
        ZeeConfig.addGobText(gobSource,"source");

        //start collection from source
        if( gobSource==null || !ZeeManagerGobClick.clickGobPetal(gobSource, lastPetalName) ){
            println("no more source? gobSource0 = "+gobSource);
            pileAndExit();
            return;
        }

        // adjust timeout if chipping stone without pickaxe
        int timeout = 3000;
        if (lastPetalName.contentEquals("Chip stone")){
            if (ZeeManagerItemClick.isItemEquipped("/pickaxe"))
                timeout = 2000;
            else
                timeout = 5000;
        }else if (lastPetalName.contentEquals("Chop into blocks")){
            timeout = 1500;
        }else if (lastPetalName.contentEquals("Pick leaf")){
            timeout = 1500;
        }

        while(busy) {

            // wait reaching source
            println("wait reach source");
            waitPlayerIdleVelocityMs(1000);

            // wait inv full
            println("wait inv full");
            waitInvFullOrHoldingItem(mainInv, timeout);

            // if not holding item
            if (!ZeeConfig.isPlayerHoldingItem()) {
                List<WItem> invItems;
                if (lastPetalName.equals("Make boards"))
                    invItems = mainInv.getWItemsByNameContains("gfx/invobjs/board-");//avoid spiral curio
                else if (lastPetalName.equals("Chop into blocks"))
                    invItems = mainInv.getWItemsByNameContains("gfx/invobjs/wblock-");//avoid splinter curio
                else
                    invItems = mainInv.getWItemsByNameContains(ZeeConfig.lastInvItemBaseName);//get last inv item without checking
                if(invItems.size()==0) {
                    //no inventory items, try getting more from source
                    if(!busy)
                        continue;
                    if( gobSource==null || !ZeeManagerGobClick.clickGobPetal(gobSource, lastPetalName) ){
                        println("no more source? gobSource1 = "+gobSource);
                        pileAndExit();
                    }
                    continue;
                }
                if(!busy)//thread cancelled
                    continue;
                WItem wItem = invItems.get(0);
                String itemName = wItem.item.getres().name;
                //pickup inv item
                if (ZeeManagerItemClick.pickUpItem(wItem)) {
                    //right click stockpile
                    ZeeManagerGobClick.itemActGob(gobPile, UI.MOD_SHIFT);
                    if (waitNotHoldingItem()) {//piling successfull
                        sleep(1000);//wait inv transfer to stockpile
                        if (mainInv.getWItemsByNameContains(itemName).size() > 0) {
                            // coal pile full, try creating 2nd pile
                            if (lastPetalName.contentEquals("Collect coal")){
                                ZeeManagerGobClick.gobClick(gobSource,1);//move towards tar kiln
                                waitPlayerIdleVelocityMs(800);//wait idle
                                //pickup coal from inv
                                if (!ZeeConfig.isPlayerHoldingItem()){
                                    if (ZeeManagerItemClick.pickUpInvItem(ZeeConfig.getMainInventory(),"/coal")) {
                                        // right click to create virtual pile(lastPlob)
                                        Coord newPileCoord = ZeeConfig.getNextTileTowards(ZeeConfig.getGobTile(gobPile),ZeeConfig.getPlayerTile());
                                        ZeeConfig.gameUI.map.wdgmsg("itemact", newPileCoord, ZeeConfig.tileToCoord(newPileCoord), 0);
                                        sleep(1000);

                                        //try placing new pile
                                        Coord playerTile = ZeeConfig.getPlayerTile();
                                        Coord c = ZeeConfig.tileToCoord(newPileCoord);
                                        ZeeManagerGobClick.gobPlace(lastPlob,c,UI.MOD_SHIFT);
                                        waitPlayerIdleFor(1);

                                        // player didnt move = couldnt place new coal pile?
                                        if (playerTile.compareTo(ZeeConfig.getPlayerTile()) == 0){
                                            exitManager("couldnt create second coal pile");
                                        }
                                        // new coal pile created
                                        else{
                                            ZeeConfig.removeGobText(gobPile);
                                            gobPile = ZeeConfig.getClosestGobByNameContains("/stockpile-coal");
                                            ZeeConfig.addGobText(gobPile,"pile");
                                        }
                                    }
                                }
                            }
                            // pile full, exit
                            else {
                                exitManager("pile full (inv still has items)");
                            }
                        }
                        if(!busy)//thread cancelled
                            continue;
                        //try getting more from source
                        if( gobSource==null || !ZeeManagerGobClick.clickGobPetal(gobSource, lastPetalName) ){
                            println("gob source consumed = "+gobSource);
                            pileAndExit();
                        }
                    } else {
                        exitManager("pile full??");
                    }
                } else {
                    //pileAndExit();
                    exitManager("couldn't pickup source item??");
                }
            }
            //holding item? try stockpiling...
            else {

                if(!busy)
                    continue;
                String itemName = ZeeConfig.gameUI.vhand.item.getres().name;
                ZeeManagerGobClick.itemActGob(gobPile, UI.MOD_SHIFT);//right click stockpile
                if(waitNotHoldingItem()) {
                    sleep(1000);
                    //check if pile full
                    if (mainInv.getWItemsByNameContains(itemName).size() > 0)
                        exitManager("pile full (inv still has items) 2");
                    if (!busy)
                        continue;
                    //check if tree still have leafs
                    if (gobSource == null || !ZeeManagerGobClick.clickGobPetal(gobSource, lastPetalName)) {
                        println("no more source? gobSource3 = " + gobSource);
                        pileAndExit();
                        return;
                    }
                } else {
                    exitManager("pile full?? 2");
                }
            }
        }
        //pileAndExit();
    }

    private static Gob findPile() {
        if (diggingTileSource) {
            if (lastTileStoneSourceTileName.contentEquals(ZeeConfig.TILE_BEACH))
                return ZeeConfig.getClosestGob(ZeeConfig.findGobsByNameContains(STOCKPILE_SAND));
            else
                return ZeeConfig.getClosestGob(ZeeConfig.findGobsByNameContains(STOCKPILE_STONE));
        }
        if (lastPetalName==null)
            return null;
        if(lastPetalName.equals("Pick leaf"))
            return ZeeConfig.getClosestGob(ZeeConfig.findGobsByNameContains("stockpile-leaf"));
        else if (lastPetalName.equals("Chop into blocks"))
            return ZeeConfig.getClosestGob(ZeeConfig.findGobsByNameContains("stockpile-wblock"));
        else if (lastPetalName.equals("Make boards"))
            return ZeeConfig.getClosestGob(ZeeConfig.findGobsByNameContains("stockpile-board"));
        else if (lastPetalName.equals("Chip stone"))
            return ZeeConfig.getClosestGob(ZeeConfig.findGobsByNameContains("stockpile-stone"));
        else if (lastPetalName.equals("Collect coal"))
            return ZeeConfig.getClosestGob(ZeeConfig.findGobsByNameContains("stockpile-coal"));
        return null;
    }


    // add piles to Area until area full (
    static boolean selTileSourcePile = false;
    static Coord2d tileSourceCoord;
    static String tileSourceName;
    static Gob tileSourceGobPile;
    static void tileSourceWaitSelection(Coord2d coordMc){
        try {

            tileSourceName = ZeeConfig.getTileResName(coordMc);

            //check for pickaxe
            if (tileSourceName.contentEquals(ZeeConfig.TILE_MOUNTAIN)){
                if (!ZeeManagerItemClick.isItemEquipped("/pickaxe")){
                    if (ZeeManagerItemClick.getBeltWItem("/pickaxe")==null){
                        ZeeConfig.msgError("pickaxe required");
                        return;
                    }
                }
            }

            selTileSourcePile = true;
            tileSourceCoord = Coord2d.of(coordMc.x, coordMc.y);
            ZeeConfig.addPlayerText("Select area for piles");

            // wait area selection
            ZeeConfig.gameUI.map.uimsg("sel", 1); //MapView.Selector
            ZeeConfig.gameUI.map.showgrid(true);
            ZeeConfig.keepMapViewOverlay = true;

        }catch (Exception e){
            tileSourceExit("wait selection failed: "+e.getMessage());
        }
    }
    static void tileSourceAreaPilerStart(){
        new Thread(){
            public void run() {
                try {
                    ZeeConfig.addPlayerText("areapilan");
                    while(selTileSourcePile) {
                        // dig icon
                        ZeeConfig.cursorChange(ZeeConfig.ACT_DIG);
                        if (!waitCursorName(ZeeConfig.CURSOR_DIG)) {
                            tileSourceExit("no cursor dig");
                            return;
                        }
                        // click tile source
                        ZeeConfig.clickCoord(tileSourceCoord.floor(posres), 1);
                        sleep(PING_MS);
                        // wait idle pose
                        if (!waitPlayerIdlePose()) {
                            tileSourceExit("cancel click?");
                            return;
                        }
                        if (!ZeeConfig.isPlayerHoldingItem()){
                            tileSourceExit("should be holding item");
                            return;
                        }
                        // pile area
                        tileSourcePileLogic();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                tileSourceExit("the end");
            }
        }.start();
    }
    private static void tileSourcePileLogic() throws Exception {

        Inventory inv = ZeeConfig.getMainInventory();
        Coord playerTile = ZeeConfig.getPlayerTile();

        // add to existing pile
        if (tileSourceGobPile!=null){
            ZeeManagerGobClick.itemActGob(tileSourceGobPile, UI.MOD_SHIFT);
            waitPlayerIdlePose();
        }
        // create new pile
        else {
            Area area = ZeeConfig.lastSavedOverlay.a;
            Coord further = ZeeConfig.getTileFurtherFromPlayer(area);

            //create virtual pile
            ZeeConfig.gameUI.map.wdgmsg("itemact", further, ZeeConfig.tileToCoord(further), 0);
            sleep(500);

            //try placing pile
            ZeeManagerGobClick.gobPlace(lastPlob, ZeeConfig.tileToCoord(further), UI.MOD_SHIFT);
            waitPlayerIdleFor(1);
        }

        // player didnt move? tile occupied, terrain not flat, pile was already full?
        if (playerTile.compareTo(ZeeConfig.getPlayerTile()) == 0){
            tileSourceExit("player didnt move, tile unavailable or pile was already full");
        }
        // inv items remaining, pile just filled up? (TODO ignore rare item curios )
        else if(inv.countItemsByNameContains(ZeeConfig.lastInvItemName) > 0) {
            tileSourceExit("inv items remaining, pile just filled up?");
        }
        // pile placed?
        else{
            println("pile placed? back to digging");
            ZeeConfig.removeGobText(tileSourceGobPile);
            // TODO confirm pile coord(treestump removal code)
            tileSourceGobPile = ZeeConfig.getClosestGobByNameContains("/stockpile-");
            ZeeConfig.addGobText(tileSourceGobPile,"pile");
        }
    }
    private static void tileSourceExit(String msg) {
        println(msg);
        selTileSourcePile = false;
        ZeeConfig.removePlayerText();
        ZeeConfig.gameUI.map.uimsg("sel", 0);
        ZeeConfig.resetTileSelection();
        ZeeConfig.gameUI.map.showgrid(false);
        if (tileSourceGobPile!=null)
            ZeeConfig.removeGobText(tileSourceGobPile);
        tileSourceGobPile = null;
    }


    // single pile
    static void startPilingTileSource() {
        try {

            if (lastTileStoneSourceTileName.contentEquals(ZeeConfig.TILE_BEACH))
                gobPile = ZeeConfig.getClosestGob(ZeeConfig.findGobsByNameContains(STOCKPILE_SAND));
            else // TODO check pickaxe?
                gobPile = ZeeConfig.getClosestGob(ZeeConfig.findGobsByNameContains(STOCKPILE_STONE));

            ZeeConfig.addGobText(gobPile,"pile");

            ZeeConfig.addPlayerText("dig");

            pileItems();

            while (busy){

                //dig stone tile
                ZeeConfig.clickCoord(lastTileStoneSourceCoordMc.floor(posres),1);

                //wait inv full
                while(busy && ZeeConfig.getMainInventory().getNumberOfFreeSlots() > 0){
                    sleep(2000);
                }
                if (!busy)
                    break;

                pileItems();
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        exitManager();
    }


    public static void checkWdgmsgPileExists() {
        new ZeeThread() {
            public void run() {
                try {
                    waitNotHoldingItem(5000);
                    Gob closestPile = findPile();
                    if (closestPile!=null) {
                        checkShowWindow(closestPile.getres().name);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public static void checkWdgmsgPilePlacing() {
        if (lastPlob !=null && lastPlob.getres()!=null && lastPlob.getres().name.contains("/stockpile-")) {
            String pileGobName = lastPlob.getres().name;
            checkShowWindow(pileGobName);
        }
    }

    private static void checkShowWindow(String pileGobName) {

        boolean show = false;
        String task = "";

        if ( diggingTileSource && (pileGobName.equals(STOCKPILE_STONE) || pileGobName.equals(STOCKPILE_SAND)) ) {
            show = true;
            task = TASK_PILE_TILE_SOURCE;
        }
        else if ( lastPetalName == null || gobSource == null ){
            show = false;
        }
        else if (now() - ZeeConfig.lastInvItemMs > 3000){ //3s
            show = false; // time limit to avoid late unwanted window popup
        }
        else if (pileGobName.equals(STOCKPILE_LEAF) && ZeeConfig.lastInvItemBaseName.contentEquals(BASENAME_MULB_LEAF)){
            show = true;
            task = TASK_PILE_GOB_SOURCE;
        }
        else if (pileGobName.equals(STOCKPILE_LEAF) && ZeeConfig.lastInvItemBaseName.contentEquals("leaf-laurel")){
            show = true;
            task = TASK_PILE_GOB_SOURCE;
        }
        else if (pileGobName.equals(STOCKPILE_BLOCK) && ZeeConfig.lastInvItemBaseName.startsWith("wblock-")){
            show = true;
            task = TASK_PILE_GOB_SOURCE;
        }
        else if (pileGobName.equals(STOCKPILE_BOARD) && ZeeConfig.lastInvItemBaseName.contains("board-")){
            show = true;
            task = TASK_PILE_GOB_SOURCE;
        }
        else if (pileGobName.equals(STOCKPILE_COAL) && ZeeConfig.lastInvItemBaseName.contentEquals("coal")){
            show = true;
            task = TASK_PILE_GOB_SOURCE;
        }
        else if (pileGobName.equals(STOCKPILE_STONE) && ZeeManagerMiner.isBoulder(gobSource)){
            show = true;
            task = TASK_PILE_GOB_SOURCE;
        }

        if(show)
            showWindow(task);
    }

    public static void checkClickedPetal(String petalName) {
        lastPetalName = petalName;
        if (petalName.contentEquals("Sleep")) {
            ZeeSess.charSwitchCancelAutologin("char slept");
        }else if (petalName.equals("Pick leaf")) {
            if (ZeeConfig.lastMapViewClickGobName.equals("gfx/terobjs/trees/mulberry")
                || ZeeConfig.lastMapViewClickGobName.equals("gfx/terobjs/trees/laurel")) {
                gobSource = ZeeConfig.lastMapViewClickGob;
            }
        }else if(petalName.equals("Chop into blocks")){
            gobSource = ZeeConfig.lastMapViewClickGob;
            lastTreelogChopped = ZeeConfig.lastMapViewClickGob;
        }else if(petalName.equals("Make boards")){
            gobSource = ZeeConfig.lastMapViewClickGob;
            lastTreelogSawed = ZeeConfig.lastMapViewClickGob;
        }else if(petalName.equals("Chip stone")){
            try {
                gobSource = ZeeConfig.lastMapViewClickGob;
                lastBoulderChipped = ZeeConfig.lastMapViewClickGob;
                lastBoulderChippedStoneName = lastBoulderChipped.getres().basename().replaceAll("\\d$", "");
            }catch (Exception e){
                e.printStackTrace();//auto-chip while lifting object?
            }
        }else if(petalName.equals("Collect coal")){
            gobSource = ZeeConfig.lastMapViewClickGob;
        }
    }

    private static void showWindow(String task) {
        Widget wdg;
        if(windowManager ==null) {
            windowManager = new ZeeWindow(new Coord(150, 60), "Stockpile manager") {
                public void wdgmsg(String msg, Object... args) {
                    if (msg.equals("close")) {
                        busy = false;
                        exitManager();
                    }else
                        super.wdgmsg(msg, args);
                }
            };
            wdg = windowManager.add(new ZeeWindow.ZeeButton(UI.scale(75),"auto pile"){
                public void wdgmsg(String msg, Object... args) {
                    if(msg.equals("activate")){
                        new ZeeManagerStockpile(task).start();
                    }
                }
            }, 5,5);
            ZeeConfig.gameUI.add(windowManager, new Coord(100,100));
        }else{
            windowManager.show();
        }
    }

    private void pileAndExit() throws InterruptedException {
        pileItems();
        exitManager();
    }

    public static void exitManager(String msg) {
        println(msg);
        exitManager();
    }

    public static void exitManager() {
        busy = false;
        ZeeConfig.stopMovingEscKey();
        if (ZeeConfig.pilerMode)
            ZeeInvMainOptionsWdg.cbPiler.set(false);
        ZeeConfig.removePlayerText();
        if (gobPile!=null)
            ZeeConfig.removeGobText(gobPile);
        if (gobSource!=null)
            ZeeConfig.removeGobText(gobSource);
        gobPile = gobSource = null;

        //TODO check for window.visible in isTransferOpen
        windowManager.reqdestroy();
        windowManager = null;
    }


    private static void pileItems() throws InterruptedException {
        ZeeConfig.cancelFlowerMenu();
        waitNoFlowerMenu();
        // if not holding item, pickup from inventory
        if (!ZeeConfig.isPlayerHoldingItem()) {
            List<WItem> invItems = mainInv.getWItemsByNameContains(ZeeConfig.lastInvItemBaseName);
            if(invItems.size()==0) {
                return;//inv has no more items
            }
            WItem wItem = invItems.get(0);
            if (ZeeManagerItemClick.pickUpItem(wItem)) { //pickup source item
                ZeeManagerGobClick.itemActGob(gobPile, UI.MOD_SHIFT);//shift+right click stockpile
                if (!waitNotHoldingItem()) {
                    exitManager("pileItems > pile full?");
                }
            } else {
                exitManager("pileItems > couldn't pickup item?");
            }
        }
        // holding item
        else if (gobPile!=null){
            ZeeManagerGobClick.itemActGob(gobPile, UI.MOD_SHIFT);//shift+right click stockpile
            if (!waitNotHoldingItem()) {
                exitManager("pileItems > pile full? 2");
            }
        }
    }


    public static void useWheelbarrowAtStockpile(Gob gobStockpile) {
        try {
            ZeeConfig.addPlayerText("wheeling");
            Coord stockpileCoord = ZeeConfig.lastMapViewClickMc.floor(posres);
            if (ZeeConfig.isPlayerMountingHorse() || ZeeConfig.isPlayerDrivingingKicksled()) {
                //move to stockpile
                double dist = ZeeConfig.distanceToPlayer(gobStockpile);
                if (dist > 100){
                    ZeeConfig.moveToGobTile(gobStockpile);
                    waitPlayerDistToGob(gobStockpile,90);
                }
                //disembark/unmount
                ZeeManagerGobClick.disembarkVehicle(stockpileCoord);
                sleep(999);
            }
            Coord pc = ZeeConfig.getPlayerCoord();
            Coord subc = ZeeConfig.getGobCoord(gobStockpile).sub(pc);
            int xsignal, ysignal;
            xsignal = subc.x >= 0 ? 1 : -1;
            ysignal = subc.y >= 0 ? 1 : -1;
            //try to drop wheelbarrow towards stockpile
            ZeeConfig.clickCoord(pc.add(xsignal * 500, ysignal * 500), 3);
            sleep(555);
            if (!ZeeConfig.isPlayerCarryingWheelbarrow()){
                Gob wb = ZeeConfig.getClosestGobByNameContains("gfx/terobjs/vehicle/wheelbarrow");
                //activate wheelbarrow
                ZeeManagerGobClick.gobClick(wb, 3);
                sleep(PING_MS);
                //use wheelbarrow on stockpile
                ZeeManagerGobClick.gobClick(gobStockpile, 3);
                waitPlayerIdleVelocity();
            }else
                ZeeConfig.msg("couldn't drop wheelbarrow");
        }catch (Exception e){
            e.printStackTrace();
        }
        ZeeConfig.removePlayerText();
    }


    public static void unloadWheelbarrowStockpileAtGround(Coord mcFloorPosres) {
        try {
            ZeeConfig.addPlayerText("wheeling");
            Coord pc = ZeeConfig.getPlayerCoord();
            Coord subc = mcFloorPosres.sub(pc);
            int xsignal, ysignal;
            xsignal = subc.x >= 0 ? 1 : -1;
            ysignal = subc.y >= 0 ? 1 : -1;
            //drop wheelbarrow
            ZeeConfig.clickCoord(pc.add(xsignal*500,ysignal*500), 3);
            sleep(500);
            if (!ZeeConfig.isPlayerCarryingWheelbarrow()) {
                Gob wb = ZeeConfig.getClosestGobByNameContains("gfx/terobjs/vehicle/wheelbarrow");
                //activate wheelbarrow
                ZeeManagerGobClick.gobClick(wb, 3);
                sleep(PING_MS);
                //use wheelbarrow at stockpile
                ZeeConfig.clickCoord(mcFloorPosres, 3);
                waitPlayerIdleVelocity();
            }else
                ZeeConfig.msg("couldn't drop wheelbarrow");
        } catch (Exception e){
            e.printStackTrace();
        }
        ZeeConfig.removePlayerText();
    }

    static boolean isGroundTileSource(String tileResName) {
        String[] list = new String[]{ZeeConfig.TILE_BEACH, ZeeConfig.TILE_SANDCLIFF, ZeeConfig.TILE_MOUNTAIN};
        //println(tileResName);
        for (int i = 0; i < list.length; i++) {
            if (list[i].contentEquals(tileResName))
                return true;
        }
        return false;
    }

    static void checkTileSourcePiling(Coord2d mc) {
        if ( ZeeConfig.pilerMode && ZeeConfig.getCursorName().contentEquals(ZeeConfig.CURSOR_DIG) ) {
            String tileName = ZeeConfig.getTileResName(mc);
            if(ZeeManagerStockpile.isGroundTileSource(tileName)) {
                ZeeManagerStockpile.diggingTileSource = true;
                ZeeManagerStockpile.lastTileStoneSourceCoordMc = mc;
                ZeeManagerStockpile.lastTileStoneSourceTileName = tileName;
            } else
                ZeeManagerStockpile.diggingTileSource = false;
        } else
            ZeeManagerStockpile.diggingTileSource = false;
    }

    static boolean isGobPileable(Gob gob) {
        String resname = gob.getres().name;
        String[] keys = mapItemPileRegex.keySet().toArray(new String[0]);
        for (int i = 0; i < keys.length; i++) {
            if(resname.matches(keys[i])) {
                return true;
            }
        }
        println("isGobPileable false");
        return false;
    }

    static boolean selAreaPile;
    static Gob selAreaPileGobItem;
    static ZeeWindow selAreaWindow;
    static void areaPilerWindow(Gob gobItem) {
        String title = "Area piler";
        Widget wdg;

        selAreaPileGobItem = gobItem;

        //remove prev windows
        if (selAreaWindow!=null)
            selAreaWindow.destroy();

        //create new window
        selAreaWindow = new ZeeWindow(Coord.of(330,170),title){
            public void wdgmsg(String msg, Object... args) {
                if (msg.contentEquals("close")){
                    exitAreaPiler();
                }
                super.wdgmsg(msg, args);
            }
        };

        //button select area
        wdg = selAreaWindow.add(new Button(UI.scale(160),"select pile items area"){
            public void wdgmsg(String msg, Object... args) {
                if (msg.contentEquals("activate")){
                    ZeeConfig.addPlayerText("Select pile items area");
                    selAreaPile = true;
                    //creates new MapView.Selector
                    ZeeConfig.gameUI.map.uimsg("sel", 1);
                    //show grid lines
                    ZeeConfig.gameUI.map.showgrid(true);
                    ZeeConfig.keepMapViewOverlay = true;
                }
            }
        });

        //label instructions
        wdg = selAreaWindow.add(new Label("Note: piles will be created outside area"),0,wdg.c.y+wdg.sz.y+7);

        selAreaWindow.pack();
        ZeeConfig.gameUI.add(selAreaWindow,300,300);
    }

    static void exitAreaPiler() {
        selAreaPile = false;
        ZeeConfig.removePlayerText();
        ZeeConfig.gameUI.map.uimsg("sel", 0);
        ZeeConfig.resetTileSelection();
        ZeeConfig.gameUI.map.showgrid(false);
        // TODO exit thread piler stop all activity and cleanup
    }

    static ZeeThread threadAreaPiler;
    static ZeeThread areaPilerStart() {
        if (selAreaPileGobItem==null){
            println("areaPilerStart > selAreaPileGobItem null");
            return null;
        }
        ZeeConfig.gameUI.map.showgrid(false);
        threadAreaPiler = new ZeeThread(){
            public void run() {
                try{
                    String itemGobName = selAreaPileGobItem.getres().name;
                    String itemInvName = "gfx/invobjs/" + selAreaPileGobItem.getres().basename();
                    Coord olStart = ZeeConfig.lastSavedOverlayStartCoord;
                    Coord olEnd = ZeeConfig.lastSavedOverlayEndCoord;
                    int minX,maxX,minY,maxY;
                    minX = Math.min(olStart.x,olEnd.x);
                    minY = Math.min(olStart.y,olEnd.y);
                    maxX = Math.max(olStart.x,olEnd.x);
                    maxY = Math.max(olStart.y,olEnd.y);
                    Gob latestPile = null;

                    // get border tiles for placing piles
                    List<Coord> borderTiles = getPilesTiles(olStart,olEnd);
                    if (borderTiles.size()==0){
                        println("no pile tiles found");
                        return;
                    }

                    ZeeConfig.addPlayerText("pilan");

                    // create pile(s) at border tiles
                    while(selAreaPile && borderTiles.size() > 0) {

                        //pickup items until idle
                        if (!ZeeConfig.isPlayerHoldingItem()) {
                            Gob closestItem = ZeeConfig.getClosestGobByNameContains(itemGobName);
                            if (closestItem == null) {
                                break;
                            }
                            //click closest item
                            ZeeManagerGobClick.gobClick(closestItem, 3, UI.MOD_SHIFT);
                            //wait approaching item
                            waitPlayerIdleVelocity();
                            //pickup itemns until inventory idle
                            waitInvIdleMs(1000);
                            //check if user closed piler window
                            if (!selAreaPile) {
                                break;
                            }
                        }

                        //move to center tile before creating pile
                        ZeeConfig.moveToAreaCenter(ZeeConfig.lastSavedOverlay.a);
                        waitPlayerIdleVelocity();

                        // pickup inv item
                        if (!ZeeConfig.isPlayerHoldingItem()) {
                            List<WItem> items = ZeeConfig.getMainInventory().getWItemsByNameContains(itemInvName);
                            if (items==null || items.size()==0){
                                println("no more inventory items to pile");
                                continue;
                            }
                            if (!ZeeManagerItemClick.pickUpItem(items.get(0))){
                                println("couldnt pickup item ..... 123");
                                continue;
                            }
                        }

                        // use latest pile
                        if (latestPile!=null){
                            ZeeManagerGobClick.itemActGob(latestPile,UI.MOD_SHIFT);
                            waitPlayerIdleVelocity();//wait approach pile
                            sleep(500);//wait transf items
                            List<WItem> invItems = ZeeConfig.getMainInventory().getWItemsByNameContains(itemInvName);
                            if (invItems!=null && invItems.size()>0){
                                println("latest pile is full");
                                if (latestPile!=null)
                                    ZeeConfig.removeGobText(latestPile);
                                latestPile = null;
                            }
                        }
                        //new pile
                        else {

                            areaPilerCreateNewPile(borderTiles,itemGobName,minX,minY);

                            if (borderTiles.isEmpty()) {
                                println("out of tiles 2");
                                break;
                            }

                            //update next pile
                            if (latestPile!=null)
                                ZeeConfig.removeGobText(latestPile);
                            latestPile = ZeeConfig.getClosestGobByNameContains("/stockpile-");
                            ZeeConfig.addGobText(latestPile,"pile");
                        }
                    }

                    // pile remaining inv items
                    List<WItem> items = ZeeConfig.getMainInventory().getWItemsByNameContains(itemInvName);
                    if (items.size() > 0){
                        println("pile remaining inv items");
                        ZeeConfig.moveToAreaCenter(ZeeConfig.lastSavedOverlay.a);
                        waitPlayerIdleVelocity();
                        ZeeManagerItemClick.pickUpItem(items.get(0));
                        areaPilerCreateNewPile(borderTiles,itemGobName,minX,minY);
                    }

                    ZeeConfig.removePlayerText();

                    if (latestPile!=null)
                        ZeeConfig.removeGobText(latestPile);

                }catch (Exception e){
                    e.printStackTrace();
                    ZeeConfig.removePlayerText();
                }

                //destroy area selection
                selAreaPile = false;
                ZeeConfig.gameUI.map.uimsg("sel",0);
                ZeeConfig.resetTileSelection();

                //close window
                if (selAreaWindow!=null)
                    selAreaWindow.destroy();
            }
        };
        threadAreaPiler.start();
        return threadAreaPiler;
    }

    private static void areaPilerCreateNewPile(List<Coord> borderTiles, String itemGobName, int minX, int minY) throws InterruptedException {
        // get non farming tile (gfx/tiles/field)
        Coord nonFarmingTile = Coord.of(minX - 1, minY - 1);
        String tileResName;
        do{
            nonFarmingTile = nonFarmingTile.sub(1,1);
            tileResName = ZeeConfig.getTileResName(nonFarmingTile);
        }while(tileResName.contentEquals("gfx/tiles/field"));
        if (tileResName.isBlank()){
            println("blank tile");
            nonFarmingTile = borderTiles.get(0);
        }

        //rclick nonfarming tile to create virtual pile
        ZeeConfig.gameUI.map.wdgmsg("itemact", nonFarmingTile, ZeeConfig.tileToCoord(nonFarmingTile), 0);
        sleep(500);

        // try placing pile until run out of border tiles
        areaPilerTryPlacingOnTiles(borderTiles,itemGobName);
    }

    private static void areaPilerTryPlacingOnTiles(List<Coord> borderTiles, String itemGobName) {
        do {
            //get new tile
            if (borderTiles.isEmpty()) {
                println("out of border tiles for placing piles");
                break;
            }
            Coord coordNewPile = borderTiles.remove(0);

            //reserve space for big piles
            if (itemGobName.contains("/straw")) {
                borderTiles.remove(0);
                if (itemGobName.contains("/board-"))
                    borderTiles.remove(0);
            }

            //try placing pile
            Coord playerTile = ZeeConfig.getPlayerTile();
            ZeeManagerGobClick.gobPlace(lastPlob, ZeeConfig.tileToCoord(coordNewPile), UI.MOD_SHIFT);
            waitPlayerIdleFor(1);

            //player didnt move? tile occupied, terrain not flat...
            if (playerTile.compareTo(ZeeConfig.getPlayerTile()) == 0){
                println("tile can't be used for pile, trying next");
            }else{
                //tile free, pile placed?
                break;
            }

        }while (!borderTiles.isEmpty());
    }

    // FIXME second line of piles should not be parallel, but perpendicular
    static List<Coord> getPilesTiles(Coord olStart, Coord olEnd) {
        List<Coord> listRowTiles = new ArrayList<>();
        int minX,maxX,minY,maxY;
        minX = Math.min(olStart.x,olEnd.x);
        minY = Math.min(olStart.y,olEnd.y);
        maxX = Math.max(olStart.x,olEnd.x);
        maxY = Math.max(olStart.y,olEnd.y);
        //top row tiles
        for (int i = minX-1; i <= maxX+1; i++) {
            listRowTiles.add(Coord.of(i,minY-1));
        }
        //bottom row tiles
        for (int i = minX-1; i <= maxX+1; i++) {
            listRowTiles.add(Coord.of(i,minY+1));
        }
        return listRowTiles;
    }

    static boolean isTileInsideArea(Coord gobCoord, Coord olStart, Coord olEnd) {
        int minX,maxX,minY,maxY;
        minX = Math.min(olStart.x,olEnd.x);
        minY = Math.min(olStart.y,olEnd.y);
        maxX = Math.max(olStart.x,olEnd.x);
        maxY = Math.max(olStart.y,olEnd.y);
        return (gobCoord.x >= minX && gobCoord.x <= maxX && gobCoord.y >= minY && gobCoord.y <= maxY);
    }


    public static void pileInvStonesAndChipMore(Gob existingPile) {
        new ZeeThread(){
            public void run() {
                try {
                    Inventory inv = ZeeConfig.getMainInventory();
                    // pickup inv stone (if player was holding item a generic method would already been called)
                    if (ZeeManagerItemClick.pickUpInvItem(inv, lastBoulderChippedStoneName)) {
                        // pile stone
                        ZeeManagerGobClick.itemActGob(existingPile,UI.MOD_SHIFT);
                        //wait piling
                        if(waitNotHoldingItem()){
                            // try make stone
                            if (lastBoulderChipped!=null)
                                ZeeManagerGobClick.clickGobPetal(lastBoulderChipped,"Chip stone");
                        }
                    } else {
                        println("pile all block > couldnt get block from inv");
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public static void pileInvBlocksAndChopMore(Gob existingPile) {
        new ZeeThread(){
            public void run() {
                try {
                    Inventory inv = ZeeConfig.getMainInventory();
                    // pickup inv block (if player was holding item a generic method would already been called)
                    if (ZeeManagerItemClick.pickUpInvItem(inv, "/wblock-")) {
                        // pile block
                        ZeeManagerGobClick.itemActGob(existingPile,UI.MOD_SHIFT);
                        //wait piling
                        if(waitNotHoldingItem()){
                            // try make block
                            if (lastTreelogChopped!=null)
                                ZeeManagerGobClick.clickGobPetal(lastTreelogChopped,"Chop into blocks");
                        }
                    } else {
                        println("pile all block > couldnt get block from inv");
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }.start();
    }

    static void pileInvBoardsSawMore(Gob existingPile) {
        new ZeeThread(){
            public void run() {
                try {
                    Inventory inv = ZeeConfig.getMainInventory();
                    // pickup inv board (if player was holding item a generic method would already been called)
                    if (ZeeManagerItemClick.pickUpInvItem(inv, "/board-")) {
                        // pile board
                        ZeeManagerGobClick.itemActGob(existingPile,UI.MOD_SHIFT);
                        //wait piling
                        if(waitNotHoldingItem()){
                            // try make boards
                            if (lastTreelogSawed!=null)
                                ZeeManagerGobClick.clickGobPetal(lastTreelogSawed,"Make boards");
                        }
                    } else {
                        println("pile all board > couldnt get board from inv");
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }.start();
    }

    static void pileInvClays(Gob existingPile) {
        new ZeeThread(){
            public void run() {
                try {
                    Inventory inv = ZeeConfig.getMainInventory();
                    // pickup inv clay (if player was holding item a generic method would already been called)
                    if (ZeeManagerItemClick.pickUpInvItem(inv, "/clay-ball","/clay-acre")) {
                        // pile clay
                        ZeeManagerGobClick.itemActGob(existingPile,UI.MOD_SHIFT);
                    } else {
                        println("pile all clay > couldnt get clay from inv");
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public static void startPilesMover() {
        println("start piles mover");
    }
}
