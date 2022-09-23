package haven;


import haven.resutil.WaterTile;

import java.util.ArrayList;
import java.util.List;

import static haven.OCache.posres;

public class ZeeManagerGobClick extends ZeeThread{

    public static final int OVERLAY_ID_AGGRO = 1341;

    Coord coordPc;
    Coord2d coordMc;
    Gob gob;
    String gobName;
    boolean isGroundClick;

    public static float camAngleStart, camAngleEnd, camAngleDiff;
    public static long clickStartMs, clickEndMs, clickDiffMs;
    public static boolean barrelLabelOn = false;
    public static boolean isRemovingAllTrees, isDestroyingAllTreelogs;
    private static ArrayList<Gob> treesForRemoval, treelogsForDestruction;
    private static Gob currentRemovingTree, currentDestroyingTreelog;

    public ZeeManagerGobClick(Coord pc, Coord2d mc, Gob gobClicked) {
        coordPc = pc;
        coordMc = mc;
        gob = gobClicked;
        isGroundClick = (gob==null);
        gobName = isGroundClick ? "" : gob.getres().name;
        ZeeConfig.getMainInventory();
    }

    public static void checkMidClick(Coord pc, Coord2d mc, Gob gob, String gobName) {

        clickDiffMs = clickEndMs - clickStartMs;

        //println(clickDiffMs+"ms > "+gobName + (gob==null ? "" : " dist="+ZeeConfig.distanceToPlayer(gob)));
        //if (gob!=null) println(gobName + " poses = "+ZeeConfig.getGobPoses(gob));

        if (isLongMidClick()) {
            /*
                long mid-clicks
             */
            new ZeeManagerGobClick(pc,mc,gob).start();
        }
        else {
            /*
                short mid-clicks
             */
            if(gob==null) {//ground clicks
                if (ZeeConfig.getCursorName().contentEquals(ZeeConfig.CURSOR_INSPECT))
                    ZeeConfig.msg(ZeeConfig.getTileResName(mc));
            } else if (ZeeConfig.isPlayerHoldingItem()) {
                clickedGobHoldingItem(gob,gobName);
            } else if (isGobTrellisPlant(gobName)) {
                new ZeeThread() {
                    public void run() {
                        harvestOneTrellis(gob);
                    }
                }.start();
            } else if (isGobGroundItem(gobName)) {
                gobClick(gob,3, UI.MOD_SHIFT);//pick up all items (shift + rclick)
                if (ZeeConfig.pilerMode)
                    ZeeManagerStockpile.checkGroundItemClicked(gobName);
            } else if (isGobFireSource(gobName)) {
                new ZeeThread() {
                    public void run() {
                        if (pickupTorch())
                            itemActGob(gob,0);
                    }
                }.start();
            } else if (isGobHorse(gobName)) {
                new ZeeThread() {
                    public void run() {
                        mountHorse(gob);
                    }
                }.start();
            } else if (gobName.endsWith("/barrel")) {
                if (barrelLabelOn)
                    ZeeManagerFarmer.testBarrelsTilesClear();
                else
                    ZeeManagerFarmer.testBarrelsTiles(true);
                barrelLabelOn = !barrelLabelOn;
            } else if (gobName.endsWith("/dreca")) { // dream catcher
                new ZeeThread() {
                    public void run() {
                        twoDreamsPlease(gob);
                    }
                }.start();
            } else if (isGobMineSupport(gobName)) {
                ZeeConfig.toggleMineSupport();
            } else if(gobName.endsWith("/knarr") || gobName.endsWith("/snekkja")) {
                new ZeeThread() {
                    public void run() {
                        clickGobPetal(gob,"Cargo");
                    }
                }.start();
            }else if(ZeeConfig.isAggressive(gobName)){
                toggleOverlayAggro(gob);
            }else if (isInspectGob(gobName)) {
                inspectGob(gob);
            }
        }
    }

    public void run() {
        try {
            if (isLongMidClick()){//unnecessary check?
                // clicked ground
                if (isGroundClick){
                    //dismount horse
                    if (ZeeConfig.isPlayerMountingHorse()) {
                        dismountHorse(coordMc);
                    }
                    //clicked water
                    else if (isWaterTile(coordMc)) {
                        if (ZeeManagerItemClick.isCoracleEquipped() && !ZeeConfig.isPlayerMountingHorse())
                            dropEmbarkCoracle(coordMc);
                        else
                            inspectWaterAt(coordMc);
                    }
                    //disembark water vehicles
                    else if (ZeeConfig.isPlayerOnCoracle()) {
                        disembarkEquipCoracle(coordMc);
                    }
                    else if(ZeeConfig.isPlayerOnDugout()  || ZeeConfig.isPlayerOnRowboat()) {
                        disembarkBoatAtShore(coordMc);
                    }
                    //disembark kicksled
                    else if(ZeeConfig.isPlayerDrivingingKicksled()){
                        disembarkVehicle(coordMc);
                    }
                    //unload wheelbarrow at tile
                    else if (ZeeConfig.isPlayerCarryingWheelbarrow()) {
                        ZeeManagerStockpile.unloadWheelbarrowStockpileAtGround(coordMc.floor(posres));
                        if (ZeeConfig.autoToggleGridLines)
                            ZeeConfig.gameUI.map.showgrid(true);
                    }
                    // clear snow area
                    else if (ZeeConfig.getTileResName(coordMc).endsWith("/snow")){
                        //haven.MapView@11460448 ; click ; [(629, 490), (1014904, 1060429), 3, 1]
                        ZeeConfig.clickCoord(coordMc.floor(posres),3,UI.MOD_SHIFT);
                    }
                }
                // clear reeds
                else if (gobName.contentEquals("gfx/terobjs/bushes/reeds")) {
                    clearReeds(gob);
                }
                // schedule tree removal
                else if (isRemovingAllTrees && isGobTree(gobName)) {
                    scheduleRemoveTree(gob);
                }
                // schedule treelog destruction
                else if (isDestroyingAllTreelogs && isGobTreeLog(gobName)) {
                    scheduleDestroyTreelog(gob);
                }
                // show ZeeFlowerMenu
                else if (!isGroundClick && !ZeeConfig.isPlayerHoldingItem() && showGobFlowerMenu()) {

                }
                // activate cursor harvest
                else if (isGobCrop(gobName)) {
                    if (!ZeeConfig.getCursorName().equals(ZeeConfig.CURSOR_HARVEST))
                        gobClick(gob, 3, UI.MOD_SHIFT);
                }
                // use wheelbarrow at stockpile
                else if (isGobStockpile(gobName) && ZeeConfig.isPlayerCarryingWheelbarrow()) {
                    ZeeManagerStockpile.useWheelbarrowAtStockpile(gob);
                    if (ZeeConfig.autoToggleGridLines)
                        ZeeConfig.gameUI.map.showgrid(true);
                }
                // pick up all dryingframe items
                else if (isGobStockpile(gobName) || gobName.endsWith("/dframe")) {
                    gobClick(gob,3, UI.MOD_SHIFT);
                }
                // remove tree stump
                else if (isGobTreeStump(gobName)) {
                    removeStumpMaybe(gob);
                }
                // item act barrel
                else if (ZeeConfig.isPlayerHoldingItem() && gobName.endsWith("/barrel")) {
                    if (ZeeManagerFarmer.isBarrelEmpty(gob))
                        itemActGob(gob,UI.MOD_SHIFT);//shift+rclick
                    else
                        itemActGob(gob,3);//ctrl+shift+rclick
                }
                // player lifting wheelbarrow
                else if (ZeeConfig.isPlayerCarryingWheelbarrow()) {
                    // mount horse and liftup wb
                    if (isGobHorse(gobName)) {
                        mountHorseCarryingWheelbarrow(gob);
                    }
                    // unload wb at gob
                    else {
                        unloadWheelbarrowAtGob(gob);
                        if (ZeeConfig.autoToggleGridLines)
                            ZeeConfig.gameUI.map.showgrid(true);
                    }
                }
                // player driving wheelbarrow
                else if (!gobName.endsWith("/wheelbarrow") && ZeeConfig.isPlayerDrivingWheelbarrow()) {
                    // mount horse and liftup wb
                    if (isGobHorse(gobName))
                        mountHorseDrivingWheelbarrow(gob);
                    // lift up wb and open gate
                    else if (isGobGate(gobName))
                        openGateWheelbarrow(gob);
                    // lift up wb and store in cart
                    else if (gobName.endsWith("/cart")) {
                        Gob wb = ZeeConfig.getClosestGobByNameContains("/wheelbarrow");
                        liftGobAndClickTarget(wb,gob);
                    }
                }
                // drive ship
                else if(gobName.endsWith("/knarr") || gobName.endsWith("/snekkja")) {
                    clickGobPetal(gob,"Man the helm");
                }
                // lift up gob
                else if (isGobLiftable(gobName) || isGobBush(gobName)) {
                    liftGob(gob);
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void clearReeds(Gob reed) {
        try{
            ZeeConfig.addPlayerText("clearing");
            do {
                clickGobPetal(reed, "Clear");
                // wait harvesting end, or cancel click returns false
                if (!waitPlayerPoseNotInList(ZeeConfig.POSE_PLAYER_HARVESTING,ZeeConfig.POSE_PLAYER_DRINK))
                    break;
                sleep(PING_MS);
                reed = ZeeConfig.getClosestGobByNameContains("gfx/terobjs/bushes/reeds");
            }while(!ZeeConfig.isPlayerHoldingItem() && !ZeeConfig.isTaskCanceledByGroundClick());
        }catch (Exception e){
            e.printStackTrace();
        }
        ZeeConfig.removePlayerText();
    }

    void disembarkBoatAtShore(Coord2d mc){
        try {
            ZeeConfig.addPlayerText("boatin");
            //move to shore
            ZeeConfig.clickTile(ZeeConfig.coordToTile(coordMc), 1);
            waitPlayerPoseNotInList(
                    ZeeConfig.POSE_PLAYER_DUGOUT_ACTIVE,
                    ZeeConfig.POSE_PLAYER_ROWBOAT_ACTIVE,
                    ZeeConfig.POSE_PLAYER_CORACLE_ACTIVE
            );//TODO add snekkja, knarr?
            //disembark
            ZeeConfig.clickTile(ZeeConfig.coordToTile(mc), 1, UI.MOD_CTRL);
        }catch (Exception e){
            e.printStackTrace();
        }
        ZeeConfig.removePlayerText();
    }

    void disembarkEquipCoracle(Coord2d coordMc){
        try {
            ZeeConfig.addPlayerText("coracling");
            //move to shore
            ZeeConfig.clickTile(ZeeConfig.coordToTile(coordMc),1);
            waitPlayerPose(ZeeConfig.POSE_PLAYER_CORACLE_IDLE);
            //disembark
            ZeeConfig.clickTile(ZeeConfig.coordToTile(coordMc),1,UI.MOD_CTRL);
            sleep(PING_MS*2);
            if (ZeeConfig.isPlayerOnCoracle()){
                println("couldn't dismount coracle");
                ZeeConfig.removePlayerText();
                return;
            }
            //find coracle
            Gob coracle = ZeeConfig.getClosestGobByNameContains("/coracle");
            if (coracle == null) {
                println("couldn't find gob coracle");
                ZeeConfig.removePlayerText();
                return;
            }
            //try pickup coracle, if cape slot empty
            clickGobPetal(coracle,"Pick up");
        }catch (Exception e){
            e.printStackTrace();
        }
        ZeeConfig.removePlayerText();
    }

    void dropEmbarkCoracle(Coord2d waterMc) {
        try {
            ZeeConfig.addPlayerText("coracling");

            //wait player reach water
            Gob player = ZeeConfig.getPlayerGob();
            long timeout = 5000;
            ZeeConfig.clickTile(ZeeConfig.coordToTile(waterMc),1);
            waitNotPlayerPose(ZeeConfig.POSE_PLAYER_IDLE);
            while(!ZeeConfig.playerHasAnyPose(ZeeConfig.POSE_PLAYER_IDLE) && !isWaterTile(player.rc)){
                if (timeout<=0){
                    println("couldn't reach water tile");
                    ZeeConfig.removePlayerText();
                    return;
                }
                timeout -= PING_MS;
                sleep(PING_MS);
            }

            if (ZeeConfig.isTaskCanceledByGroundClick()){
                ZeeConfig.removePlayerText();
                return;
            }

            //drop coracle at shalow water or terrain
            ZeeManagerItemClick.getEquipory().dropItemByNameContains("gfx/invobjs/small/coracle");
            ZeeConfig.stopMovingEscKey();
            waitNotPlayerPose(ZeeConfig.POSE_PLAYER_CORACLE_CAPE);


            //find coracle gob
            Gob coracle = ZeeConfig.getClosestGobByNameContains("/coracle");
            if (coracle == null) {
                println("couldn't find gob coracle");
                ZeeConfig.removePlayerText();
                return;
            }

            //if dropped tile is not water
            if (!isWaterTile(ZeeConfig.getGobTile(coracle))){
                //lift up coracle
                liftGob(coracle);
                sleep(PING_MS);
                if (ZeeConfig.isTaskCanceledByGroundClick()){
                    ZeeConfig.removePlayerText();
                    return;
                }
                // place coracle at water tile
                ZeeConfig.clickTile(ZeeConfig.coordToTile(waterMc),3);
                waitPlayerIdlePose();
                if (ZeeConfig.distanceToPlayer(coracle)==0){
                    // player blocked by deep water tile
                    Coord pc = ZeeConfig.getPlayerCoord();
                    Coord subc = ZeeConfig.coordToTile(waterMc).sub(pc);
                    int xsignal, ysignal;
                    xsignal = subc.x >= 0 ? -1 : 1;
                    ysignal = subc.y >= 0 ? -1 : 1;
                    //try to drop coracle torwards clicked water coord
                    ZeeConfig.clickCoord(pc.add(xsignal * 300, ysignal * 300), 3);
                    sleep(PING_MS*2);
                    if (ZeeConfig.isTaskCanceledByGroundClick()){
                        ZeeConfig.removePlayerText();
                        return;
                    }
                    if (ZeeConfig.distanceToPlayer(coracle)==0) {
                        println("failed dropping to deep water?");
                        ZeeConfig.removePlayerText();
                        return;
                    }
                }
            }

            //mount coracle
            clickGobPetal(coracle, "Into the blue yonder!");
            waitPlayerPose(ZeeConfig.POSE_PLAYER_CORACLE_IDLE);

        }catch (Exception e){
            e.printStackTrace();
        }
        ZeeConfig.removePlayerText();
    }

    private void inspectWaterAt(Coord2d coordMc) {

        // require wooden cup
        Inventory inv = ZeeConfig.getMainInventory();
        List<WItem> cups = inv.getWItemsByName("/woodencup");
        if (cups==null || cups.size()==0){
            ZeeConfig.msg("need woodencup to inspect water");
            return;
        }

        // pickup inv cup, click water, return cup
        WItem cup = cups.get(0);
        ZeeManagerItemClick.pickUpItem(cup);
        ZeeConfig.itemActTile(coordMc.floor(posres));
        waitPlayerIdleFor(1);

        // show msg
        String msg = ZeeManagerItemClick.getHoldingItemContentsNameQl();
        ZeeConfig.msg(msg);
        new ZeeThread(){
            public void run() {
                ZeeConfig.addPlayerText(msg);
                // wait click before removing player text
                waitMapClick();
                ZeeConfig.removePlayerText();
            }
        }.start();
        //haven.ChatUI$MultiChat@dd1ed65 ; msg ; ["hello world"]

        //empty cup
        Coord cupSlot = ZeeManagerItemClick.dropHoldingItemToInvAndRetCoord(inv);
        if (cupSlot!=null) {
            cup = inv.getItemBySlotCoord(cupSlot);
            ZeeManagerItemClick.clickItemPetal(cup, "Empty");
        }
    }

    public static boolean isWaterTile(Coord2d coordMc) {
        return isWaterTile(coordMc.floor(MCache.tilesz));
    }

    public static boolean isWaterTile(Coord tile) {
        Tiler t = ZeeConfig.getTilerAt(tile);
        return t!=null && t instanceof WaterTile;
    }


    public static void checkRightClickGob(Coord pc, Coord2d mc, Gob gob, String gobName) {

        // click barrel transfer
        if (gobName.endsWith("/barrel") && ZeeConfig.getPlayerPoses().contains(ZeeConfig.POSE_PLAYER_LIFT)) {
            new ZeeThread() {
                public void run() {
                    try {
                        if(!waitPlayerDistToGob(gob,15))
                            return;
                        sleep(555);
                        String barrelName = ZeeConfig.getBarrelOverlayBasename(gob);
                        if (!barrelName.isEmpty())
                            ZeeConfig.addGobTextTemp(gob, barrelName);
                        Gob carryingBarrel = ZeeConfig.isPlayerLiftingGob("/barrel");
                        if (carryingBarrel!=null) {
                            barrelName = ZeeConfig.getBarrelOverlayBasename(carryingBarrel);
                            if (!barrelName.isEmpty())
                                ZeeConfig.addGobTextTemp(carryingBarrel, barrelName);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
        // clicked wheelbarrel
        else if(gobName.endsWith("/wheelbarrow")  && !ZeeConfig.isPlayerLiftingGob(gob)){
            new ZeeThread() {
                public void run() {
                    try {
                        if (ZeeConfig.isPlayerMountingHorse()) {
                            //dismount horse
                            dismountHorse(mc);
                            //re-drive wheelbarrow
                            gobClick(gob,3);
                        }
                        if (ZeeConfig.isPlayerDrivingingKicksled()) {
                            //disembark kicksled
                            disembarkVehicle(mc);
                            //re-drive wheelbarrow
                            gobClick(gob,3);
                        }
                        //show gridline
                        if(ZeeConfig.autoToggleGridLines && waitPlayerPose(ZeeConfig.POSE_PLAYER_DRIVE_WHEELBARROW)){
                            ZeeConfig.gameUI.map.showgrid(true);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
        // while driving wheelbarrow: lift and click
        else if (ZeeConfig.isPlayerDrivingWheelbarrow() &&
                ( isGobInListEndsWith(gobName,"/cart,/rowboat,/snekkja,/knarr,/wagon,/spark,/gardenshed,/upstairs,/downstairs,/cellardoor,/minehole,/ladder,/cavein,/caveout,/burrow,/igloo,gate")
                  || isGobHouse(gobName) || isGobHouseInnerDoor(gobName)))
        {
            new ZeeThread() {
                public void run() {
                    Gob wb = ZeeConfig.getClosestGobByNameContains("/wheelbarrow");
                    if (isGobHouse(gobName)) {
                        try {
                            liftGob(wb);
                            sleep(100);
                            gobClick(gob, 3, 0, 16);//gob's door?
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    else{
                        liftGobAndClickTarget(wb, gob);
                    }
                }
            }.start();
        }
        // gob requires unmounting horse/kicksled
        else if (isGobRequireDisembarkVehicle(gobName) && !ZeeConfig.isPlayerLiftingGob(gob)){
            // unmount horse
            if (ZeeConfig.isPlayerMountingHorse() && ZeeConfig.getMainInventory().countItemsByName("/rope") > 0) {
                new ZeeThread() {
                    public void run() {
                        dismountHorse(mc);
                        if (isGobHouse(gobName))
                            gobClick(gob,3,0,16);//gob's door?
                        else
                            gobClick(gob,3);
                    }
                }.start();
            }
            // disembark kicksled
            else if(ZeeConfig.isPlayerDrivingingKicksled()){
                new ZeeThread() {
                    public void run() {
                        try {
                            disembarkVehicle(mc);
                            waitPlayerPoseNotInList(ZeeConfig.POSE_PLAYER_KICKSLED_IDLE, ZeeConfig.POSE_PLAYER_KICKSLED_ACTIVE);
                            sleep(100);//lagalagalaga
                            gobClick(gob,3);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        }
        // use wheelbarrow on stockpile, dismount if necessary
        else if ( isGobStockpile(gobName) && ZeeConfig.isPlayerCarryingWheelbarrow()){
            new ZeeThread() {
                public void run() {
                    unloadWheelbarrowAtGob(gob);
                    if (ZeeConfig.autoToggleGridLines)
                        ZeeConfig.gameUI.map.showgrid(true);
                }
            }.start();
        }
        // mount horse while carrying/driving wheelbarrow
        else if ( isGobHorse(gobName) && (ZeeConfig.isPlayerCarryingWheelbarrow() || ZeeConfig.isPlayerDrivingWheelbarrow())){
            new ZeeThread() {
                public void run() {
                    if (ZeeConfig.isPlayerMountingHorse())
                        dismountHorse(mc);//horse to horse?
                    if (ZeeConfig.isPlayerDrivingWheelbarrow())
                        mountHorseDrivingWheelbarrow(gob);
                    else
                        mountHorseCarryingWheelbarrow(gob);
                }
            }.start();
        }

    }

    static boolean isGobRequireDisembarkVehicle(String gobName) {
        return isGobHouseInnerDoor(gobName) || isGobHouse(gobName) || isGobChair(gobName)
                || isGobInListEndsWith(gobName,
                    "/upstairs,/downstairs,/cavein,/caveout,/burrow,/igloo," +
                        "/wheelbarrow,/loom,/cauldron,/churn,/swheel,/ropewalk," +
                        "/meatgrinder,/potterswheel,/quern,/plow,/winepress"
                );
    }

    static boolean isGobChair(String gobName) {
        String list = "/chair-rustic,/stonethrone,/royalthrone,/thatchedchair";
        return isGobInListEndsWith(gobName,list);
    }

    public static boolean isGobHouseInnerDoor(String gobName){
        return gobName.endsWith("-door");
    }

    public static boolean isGobHouse(String gobName) {
        String list = "/logcabin,/timberhouse,/stonestead,/stonemansion,/stonetower,/greathall,/windmill";
        return isGobInListEndsWith(gobName,list);
    }

    private void scheduleDestroyTreelog(Gob treelog) {
        if (treelogsForDestruction==null) {
            treelogsForDestruction = new ArrayList<Gob>();
        }

        if (treelogsForDestruction.contains(treelog)) {
            // remove treelog from queue
            removeScheduledTreelog(treelog);
        } else if (!currentDestroyingTreelog.equals(treelog)){
            // add treelog to queue
            treelogsForDestruction.add(treelog);
            ZeeConfig.addGobText(treelog,"des "+treelogsForDestruction.size());
        }
    }

    private static Gob removeScheduledTreelog(Gob treelog) {
        // remove treelog from queue
        treelogsForDestruction.remove(treelog);
        ZeeConfig.removeGobText(treelog);
        // update queue gob's texts
        for (int i = 0; i < treelogsForDestruction.size(); i++) {
            ZeeConfig.addGobText(treelogsForDestruction.get(i),"des "+(i+1));
        }
        return treelog;
    }


    private void scheduleRemoveTree(Gob tree) {
        if (treesForRemoval==null) {
            treesForRemoval = new ArrayList<Gob>();
        }

        if (treesForRemoval.contains(tree)) {
            // remove tree from queue
            removeScheduledTree(tree);
        } else if (!currentRemovingTree.equals(tree)){
            // add tree to queue
            treesForRemoval.add(tree);
            ZeeConfig.addGobText(tree,"rem "+treesForRemoval.size());
        }
    }

    private static Gob removeScheduledTree(Gob tree) {
        // remove tree from queue
        treesForRemoval.remove(tree);
        ZeeConfig.removeGobText(tree);
        // update queue gob's texts
        for (int i = 0; i < treesForRemoval.size(); i++) {
            ZeeConfig.addGobText(treesForRemoval.get(i),"rem "+(i+1));
        }
        return tree;
    }

    private static void toggleOverlayAggro(Gob gob) {
        Gob.Overlay ol = gob.findol(OVERLAY_ID_AGGRO);
        if (ol!=null) {
            //remove all aggro radius
            ZeeConfig.findGobsByNameStartsWith("gfx/kritter/").forEach(gob1 -> {
                if (ZeeConfig.isAggressive(gob1.getres().name)) {
                    Gob.Overlay ol1 = gob1.findol(OVERLAY_ID_AGGRO);
                    if (ol1!=null)
                        ol1.remove();
                }
            });
        }
        else if (ZeeConfig.aggroRadiusTiles > 0) {
            //add all aggro radius
            ZeeConfig.findGobsByNameStartsWith("gfx/kritter/").forEach(gob1 -> {
                if (ZeeConfig.isAggressive(gob1.getres().name)) {
                    gob1.addol(new Gob.Overlay(gob1, new ZeeGobRadius(gob1, null, ZeeConfig.aggroRadiusTiles * MCache.tilesz2.y), ZeeManagerGobClick.OVERLAY_ID_AGGRO));
                }
            });
        }
    }

    private static void unloadWheelbarrowAtGob(Gob gob) {
        ZeeManagerStockpile.useWheelbarrowAtStockpile(gob);
    }

    public static void disembarkVehicle(Coord2d coordMc) {
        ZeeConfig.clickCoord(coordMc.floor(posres),1,UI.MOD_CTRL);
    }

    public static void dismountHorse(Coord2d coordMc) {
        Gob horse = ZeeConfig.getClosestGobByNameContains("gfx/kritter/horse/");
        ZeeConfig.clickCoord(coordMc.floor(posres),1,UI.MOD_CTRL);
        waitPlayerDismounted(horse);
        if (!ZeeConfig.isPlayerMountingHorse()) {
            ZeeConfig.setPlayerSpeed(ZeeConfig.PLAYER_SPEED_2);
        }
    }

    public static void mountHorse(Gob horse){
        int playerSpeed = ZeeConfig.getPlayerSpeed();
        clickGobPetal(horse,"Giddyup!");
        waitPlayerMounted(horse);
        if (ZeeConfig.isPlayerMountingHorse()) {
            if (playerSpeed <= ZeeConfig.PLAYER_SPEED_1)
                ZeeConfig.setPlayerSpeed(ZeeConfig.PLAYER_SPEED_1);//min auto horse speed
            else
                ZeeConfig.setPlayerSpeed(ZeeConfig.PLAYER_SPEED_2);//max auto horse speed
        }
    }

    private static void clickedGobHoldingItem(Gob gob, String gobName) {
        if (isGobStockpile(gobName))
            itemActGob(gob,UI.MOD_SHIFT);//try piling all items
        else
            gobClick(gob,3,0); // try ctrl+click simulation
    }

    private static void twoDreamsPlease(Gob gob) {
        if(clickGobPetal(gob,"Harvest")) {
            waitPlayerDistToGob(gob,15);
            waitNoFlowerMenu();
            if(clickGobPetal(gob,"Harvest"))
                waitNoFlowerMenu();
        }
    }

    public static boolean pickupTorch() {
        if (ZeeManagerItemClick.pickupBeltItem("/torch")) {
            return true;
        }else if(ZeeManagerItemClick.pickupHandItem("/torch")){
            return true;
        }else if (ZeeManagerItemClick.pickUpInvItem(ZeeConfig.getMainInventory(),"/torch")){
            return true;
        }
        return false;
    }


    public static void gobZeeMenuClicked(Gob gob, String petalName){

        String gobName = gob.getres().name;

        if (petalName.contentEquals(ZeeFlowerMenu.STRPETAL_AUTOBUTCH_BIGDEADANIMAL)){
            autoButchBigDeadAnimal(gob);
        }
        else if (petalName.contentEquals(ZeeFlowerMenu.STRPETAL_LIFTUPGOB)){
            liftGob(gob);
        }
        else if(gobName.endsWith("terobjs/oven")) {
            addFuelGobMenu(gob,petalName);
        }
        else if(gobName.endsWith("terobjs/smelter")){
            addFuelGobMenu(gob,petalName);
        }
        else if (isGobTrellisPlant(gobName)){
            if(petalName.contentEquals(ZeeFlowerMenu.STRPETAL_REMOVEPLANT)) {
                destroyGob(gob);
            }
            else if (petalName.contentEquals(ZeeFlowerMenu.STRPETAL_REMOVEALLPLANTS)){
                removeAllTrellisPlants(gob);
            }
            else if(petalName.contentEquals(ZeeFlowerMenu.STRPETAL_CURSORHARVEST)){
                if (!ZeeConfig.getCursorName().equals(ZeeConfig.CURSOR_HARVEST))
                    gobClick(gob, 3, UI.MOD_SHIFT);
            }
        }
        else if(isGobTree(gobName)){
            if (petalName.contentEquals(ZeeFlowerMenu.STRPETAL_REMOVETREEANDSTUMP)
                || petalName.contentEquals(ZeeFlowerMenu.STRPETAL_REMOVEALLTREES))
            {
                removeTreeAndStump(gob, petalName);
            }
            else if (petalName.contentEquals(ZeeFlowerMenu.STRPETAL_INSPECT)) {//towercap case
                inspectGob(gob);
            }
        }
        else if (isGobCrop(gobName)) {
            if (petalName.equals(ZeeFlowerMenu.STRPETAL_SEEDFARMER)) {
                ZeeManagerFarmer.showWindow(gob);
            }
            else if (petalName.equals(ZeeFlowerMenu.STRPETAL_CURSORHARVEST)) {
                if (!ZeeConfig.getCursorName().equals(ZeeConfig.CURSOR_HARVEST))
                    gobClick(gob, 3, UI.MOD_SHIFT);
            }
        }
        else if (isBarrelTakeAll(gob)) {
            if (petalName.equals(ZeeFlowerMenu.STRPETAL_BARRELTAKEALL)) {
                barrelTakeAllSeeds(gob);
            }
        }
        else if ( petalName.equals(ZeeFlowerMenu.STRPETAL_DESTROYTREELOG3)
            || petalName.equals(ZeeFlowerMenu.STRPETAL_DESTROYTREELOG5)
            || petalName.contentEquals(ZeeFlowerMenu.STRPETAL_DESTROYALL))
        {
            destroyTreelogs(gob,petalName);
        }
        else{
            println("chooseGobFlowerMenu > unkown case");
        }
    }

    public static void autoButchBigDeadAnimal(Gob deadAnimal) {
        new ZeeThread() {
            public void run() {
                boolean butcherBackup = ZeeConfig.butcherMode;
                ZeeConfig.butcherAutoList = ZeeConfig.DEF_LIST_BUTCH_AUTO;
                try{
                    ZeeConfig.addPlayerText("autobutch");
                    ZeeConfig.lastMapViewClickButton = 2;//prepare for clickCancelTask()
                    while (!ZeeConfig.isTaskCanceledByGroundClick() && gobExistsBecauseFlowermenu(deadAnimal)) {

                        //prepare settings
                        ZeeConfig.lastInvItemMs = 0;
                        ZeeConfig.butcherMode = true;
                        ZeeConfig.autoClickMenuOption = false;

                        //click gob
                        gobClick(deadAnimal,3);

                        //wait not butching
                        waitNotPlayerPose(ZeeConfig.POSE_PLAYER_BUTCH);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
                ZeeConfig.butcherMode = butcherBackup;
                ZeeConfig.autoClickMenuOption = Utils.getprefb("autoClickMenuOption", true);
                ZeeConfig.removePlayerText();
            }
        }.start();
    }

    private static void destroyTreelogs(Gob firstTreelog, String petalName) {
        if (!ZeeManagerItemClick.isItemInHandSlot("/bonesaw") || ZeeManagerItemClick.isItemInHandSlot("/saw-m")){
            ZeeConfig.msg("need bone saw equipped, no metal saw");
            return;
        }
        Gob treelog = firstTreelog;
        int logs = 2;
        try {
            waitNoFlowerMenu();
            String treelogName = treelog.getres().name;
            if (petalName.equals(ZeeFlowerMenu.STRPETAL_DESTROYTREELOG3)) {
                logs = 3;
            } else if (petalName.equals(ZeeFlowerMenu.STRPETAL_DESTROYTREELOG5)) {
                logs = 5;
            } else if (petalName.equals(ZeeFlowerMenu.STRPETAL_DESTROYALL)) {
                isDestroyingAllTreelogs = true;
                logs = 999;
            }
            ZeeConfig.dropBoards = true;
            ZeeConfig.lastMapViewClickButton = 2;//prepare for cancel click
            while ( logs > 0  &&  !ZeeConfig.isTaskCanceledByGroundClick() ) {
                ZeeConfig.addPlayerText("treelogs "+logs);
                if (!clickGobPetal(treelog,"Make boards")){
                    println("can't click treelog = "+treelog);
                    logs = -1;
                    currentDestroyingTreelog = null;
                    continue;
                }
                currentDestroyingTreelog = treelog;
                waitPlayerIdlePose();
                if (!ZeeConfig.isTaskCanceledByGroundClick()){
                    logs--;
                    if (isDestroyingAllTreelogs){
                        // destroy all, treelog queue is present
                        if (treelogsForDestruction != null) {
                            if (treelogsForDestruction.size() > 0) {
                                treelog = removeScheduledTreelog(treelogsForDestruction.remove(0));
                            } else {
                                //stop destroying when queue consumed
                                println("logs -1, treelogsForDestruction empty");
                                logs = -1;
                            }
                        }else{
                            // destroy all, no treelog queue
                            treelog = getClosestTreeLog();
                        }
                    } else {
                        // destroy 3 or 5 same type treelogs
                        treelog = ZeeConfig.getClosestGobByNameContains(treelogName);
                    }
                }else{
                    if (ZeeConfig.isTaskCanceledByGroundClick()) {
                        ZeeConfig.msg("destroy treelog canceled by click");
                        println("destroy treelog canceled by click");
                    }else
                        println("destreelog canceled by gobHasFlowermenu?");
                    logs = -1;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        isDestroyingAllTreelogs = false;
        ZeeConfig.dropBoards = false;
        currentDestroyingTreelog = null;
        if (treelogsForDestruction!=null)
            treelogsForDestruction.clear();
        treelogsForDestruction = null;
        ZeeConfig.removePlayerText();
    }

    public static Gob getClosestTree() {
        List<Gob> list = ZeeConfig.findGobsByNameContains("/trees/");
        list.removeIf(gob1 -> !isGobTree(gob1.getres().name));
        return ZeeConfig.getClosestGob(list);
    }

    public static Gob getClosestTreeLog() {
        List<Gob> list = ZeeConfig.findGobsByNameContains("/trees/");
        list.removeIf(gob1 -> !isGobTreeLog(gob1.getres().name));
        return ZeeConfig.getClosestGob(list);
    }

    private boolean showGobFlowerMenu(){

        boolean showMenu = true;
        ZeeFlowerMenu menu = null;
        ArrayList<String> opts;//petals array

        if (isGobButchable(gobName) && isGobKnocked(gob)) {
            menu = new ZeeFlowerMenu(gob, ZeeFlowerMenu.STRPETAL_AUTOBUTCH_BIGDEADANIMAL, ZeeFlowerMenu.STRPETAL_LIFTUPGOB);
        }
        else if(gobName.endsWith("terobjs/oven")){
            menu = new ZeeFlowerMenu(gob, ZeeFlowerMenu.STRPETAL_ADD4BRANCH);
        }
        else if(gobName.endsWith("terobjs/smelter")){
            menu = new ZeeFlowerMenu(gob,ZeeFlowerMenu.STRPETAL_ADD9COAL, ZeeFlowerMenu.STRPETAL_ADD12COAL);
        }
        else if (isGobTrellisPlant(gobName)){
            menu = new ZeeFlowerMenu(gob,ZeeFlowerMenu.STRPETAL_REMOVEPLANT, ZeeFlowerMenu.STRPETAL_REMOVEALLPLANTS,ZeeFlowerMenu.STRPETAL_CURSORHARVEST);
        }
        else if (isGobTree(gobName)){
            opts = new ArrayList<String>();
            opts.add(ZeeFlowerMenu.STRPETAL_REMOVETREEANDSTUMP);
            opts.add(ZeeFlowerMenu.STRPETAL_REMOVEALLTREES);
            if (gobName.endsWith("/towercap"))
                opts.add(ZeeFlowerMenu.STRPETAL_INSPECT);
            menu = new ZeeFlowerMenu(gob, opts.toArray(String[]::new));
        }
        else if (isGobCrop(gobName)) {
            menu = new ZeeFlowerMenu(gob,ZeeFlowerMenu.STRPETAL_SEEDFARMER, ZeeFlowerMenu.STRPETAL_CURSORHARVEST);
        }
        else if (isBarrelTakeAll(gob)) {
            menu = new ZeeFlowerMenu(gob,ZeeFlowerMenu.STRPETAL_BARRELTAKEALL, ZeeFlowerMenu.STRPETAL_LIFTUPGOB);
        }
        else if (isDestroyTreelog()) {
            menu = new ZeeFlowerMenu( gob, ZeeFlowerMenu.STRPETAL_LIFTUPGOB,
                ZeeFlowerMenu.STRPETAL_DESTROYTREELOG3,
                ZeeFlowerMenu.STRPETAL_DESTROYTREELOG5,
                ZeeFlowerMenu.STRPETAL_DESTROYALL
            );
        }else{
            showMenu = false;
            //println("showGobFlowerMenu() > unkown case");
        }

        if (showMenu) {
            ZeeConfig.gameUI.ui.root.add(menu, coordPc);
        }

        return showMenu;
    }

    public static boolean isGobKnocked(Gob gob){
        String poses = ZeeConfig.getGobPoses(gob);
        //println("isGobKnocked > "+poses);
        return poses.contains("/knock") || poses.endsWith("-knock");
    }

    static boolean isGobDeadAnimal;
    private boolean isGobBigDeadAnimal_thread() {
        try{
            ZeeThread zt = new ZeeThread() {
                public void run() {
                    gobClick(gob, 3);
                    if (!waitFlowerMenu()) {//no menu detected
                        isGobDeadAnimal = false;
                        return;
                    }
                    FlowerMenu fm = getFlowerMenu();
                    for (int i = 0; i < fm.opts.length; i++) {
                        //if animal gob has butch menu, means is dead
                        if (ZeeConfig.DEF_LIST_BUTCH_AUTO.contains(fm.opts[i].name)){
                            isGobDeadAnimal = true;
                            break;
                        }
                    }
                    //close menu before returning
                    ZeeConfig.cancelFlowerMenu();
                    waitNoFlowerMenu();
                }
            };

            //disable automenu settings before thread clicks gob
            ZeeConfig.autoClickMenuOption = false;
            boolean butchBackup = ZeeConfig.butcherMode;
            ZeeConfig.butcherMode = false;

            //start thread and wait it finish
            isGobDeadAnimal = false;
            zt.start();
            zt.join();//wait thread

            //restore automenu settings
            ZeeConfig.autoClickMenuOption = Utils.getprefb("autoClickMenuOption", true);
            ZeeConfig.butcherMode = butchBackup;

            return isGobDeadAnimal;

        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    private boolean isDestroyTreelog() {
        if(isGobTreeLog(gobName) && ZeeManagerItemClick.isItemInHandSlot("bonesaw"))
            return true;
        return false;
    }

    private static void mountHorseDrivingWheelbarrow(Gob gob){
        Gob horse = gob;
        try{
            //waitNoFlowerMenu();
            ZeeConfig.addPlayerText("mounting");
            Gob wb = ZeeConfig.getClosestGobByNameContains("gfx/terobjs/vehicle/wheelbarrow");
            if (wb == null) {
                ZeeConfig.msg("no wheelbarrow close 1");
            } else {
                Coord pc = ZeeConfig.getPlayerCoord();
                Coord subc = ZeeConfig.getGobCoord(horse).sub(pc);
                int xsignal, ysignal;
                xsignal = subc.x >= 0 ? -1 : 1;//switch 1s to change direction relative to horse
                ysignal = subc.y >= 0 ? -1 : 1;
                //try position wheelbarrow away from horse direction
                ZeeConfig.clickCoord(pc.add(xsignal * 500, ysignal * 500), 1);
                sleep(PING_MS);
                gobClick(wb,3);//stop driving wheelbarrow
                sleep(PING_MS);
                mountHorse(horse);
                waitPlayerMounted(horse);
                liftGob(wb);// lift wheelbarrow
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        ZeeConfig.removePlayerText();
    }

    private static void mountHorseCarryingWheelbarrow(Gob gob) {
        Gob horse = gob;
        try {
            //waitNoFlowerMenu();
            ZeeConfig.addPlayerText("mounting");
            Gob wb = ZeeConfig.getClosestGobByNameContains("gfx/terobjs/vehicle/wheelbarrow");
            if (wb == null) {
                ZeeConfig.msg("no wheelbarrow close 2");
            } else {
                Coord pc = ZeeConfig.getPlayerCoord();
                Coord subc = ZeeConfig.getGobCoord(horse).sub(pc);
                int xsignal, ysignal;
                xsignal = subc.x >= 0 ? -1 : 1;
                ysignal = subc.y >= 0 ? -1 : 1;
                //try to drop wheelbarrow away from horse direction
                ZeeConfig.clickCoord(pc.add(xsignal * 500, ysignal * 500), 3);
                sleep(500);
                //if drop wb success
                if (!ZeeConfig.isPlayerCarryingWheelbarrow()) {
                    ZeeConfig.clickRemoveCursor();//remove hand cursor
                    mountHorse(horse);
                    waitPlayerMounted(horse);
                    liftGob(wb);//lift wheelbarrow
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        ZeeConfig.removePlayerText();
    }

    private static void liftGobAndClickTarget(Gob liftGob, Gob target){
        try {
            waitNoFlowerMenu();
            ZeeConfig.addPlayerText("lift and click");
            double dist;
            //remove hand cursor
            ZeeConfig.clickRemoveCursor();
            liftGob(liftGob);
            dist = ZeeConfig.distanceToPlayer(liftGob);
            if (dist==0) {
                // click target
                gobClick(target, 3);
                //waitPlayerIdleVelocity();
            }else{
                ZeeConfig.msg("couldnt lift gob?");//impossible case?
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        ZeeConfig.removePlayerText();
    }

    private static void openGateWheelbarrow(Gob gob) {
        // gfx/terobjs/vehicle/wheelbarrow
        Gob gate = gob;
        try {
            waitNoFlowerMenu();
            ZeeConfig.addPlayerText("wheeling");
            Gob wb = ZeeConfig.getClosestGobByNameContains("gfx/terobjs/vehicle/wheelbarrow");
            if (wb==null){
                ZeeConfig.msg("no wheelbarrow close 4");
            }else {
                double dist;
                liftGob(wb);
                sleep(PING_MS);
                dist = ZeeConfig.distanceToPlayer(wb);
                if (dist==0) {//lifted wb
                    gobClick(gate, 3);
                    waitPlayerIdleVelocity();
                }else{
                    //impossible case?
                    ZeeConfig.msg("wheelbarrow unreachable?");
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        ZeeConfig.removePlayerText();
    }

    public static boolean isGobGate(String gobName) {
        if (gobName.startsWith("gfx/terobjs/arch/") && gobName.endsWith("gate"))
            return true;
        return false;
    }


    // barrel is empty if has no overlays ("gfx/terobjs/barrel-flax")
    public static boolean isBarrelEmpty(Gob barrel){
        return ZeeManagerGobClick.getOverlayNames(barrel).isEmpty();
    }

    private static void removeAllTrellisPlants(Gob firstPlant) {
        Gob closestPlant = null;
        try{
            String gobName = firstPlant.getres().basename();
            ZeeConfig.addGobText(ZeeConfig.getPlayerGob(),"rem "+gobName);
            waitNoFlowerMenu();
            waitPlayerIdleFor(1);
            closestPlant = firstPlant;
            double dist;
            do{
                if (ZeeConfig.isTaskCanceledByGroundClick()) {
                    // cancel if clicked right/left button
                    println("cancel click");
                    break;
                }
                ZeeConfig.addGobText(closestPlant,"plant");
                destroyGob(closestPlant);
                if(!waitGobRemovedOrCancelClick(closestPlant))
                    break;
                closestPlant = ZeeConfig.getClosestGob(ZeeConfig.findGobsByNameContains(gobName));
                dist = ZeeConfig.distanceToPlayer(closestPlant);
                //println("dist "+dist);
            }while(dist < 25);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ZeeConfig.removePlayerText();
        ZeeConfig.removeGobText(closestPlant);
    }

    public static void removeTreeAndStump(Gob tree, String petalName){
        try{
            if (petalName.contentEquals(ZeeFlowerMenu.STRPETAL_REMOVEALLTREES)) {
                ZeeConfig.addPlayerText("removing all trees");
                isRemovingAllTrees = true;
            }else {
                ZeeConfig.addPlayerText("removing tree & stump");
            }
            waitNoFlowerMenu();
            ZeeManagerItemClick.equipAxeChopTree();
            ZeeConfig.lastMapViewClickButton = 2;//prepare for cancel click
            Coord2d treeCoord;
            while (tree!=null && !ZeeConfig.isTaskCanceledByGroundClick()) {
                //start chopping
                clickGobPetal(tree, "Chop");
                waitPlayerPose(ZeeConfig.POSE_PLAYER_CHOPTREE);
                currentRemovingTree = tree;
                treeCoord = new Coord2d(tree.rc.x, tree.rc.y);
                //wait idle
                if (waitPlayerIdlePose() && !ZeeConfig.isTaskCanceledByGroundClick()) {//waitPlayerIdleFor(2)
                    sleep(2500);//wait new stump loading
                    Gob stump = ZeeConfig.getClosestGob(ZeeConfig.findGobsByNameEndsWith("stump"));
                    if (stump != null  &&  stump.rc.compareTo(treeCoord) == 0) {
                        ZeeConfig.addGobText(stump, "stump");
                        removeStumpMaybe(stump);
                        waitPlayerIdlePose();
                    } else {
                        println("stump not found");
                    }
                    if (isRemovingAllTrees) {
                        if (treesForRemoval!=null){
                            if (treesForRemoval.size()>0)
                                tree = removeScheduledTree(treesForRemoval.remove(0));
                            else
                                tree = null; // stop removing trees if queue was consumed
                        }else{
                            // remove all trees until player blocked or something
                            tree = getClosestTree();
                        }
                    }else {
                        tree = null;
                    }
                    //println("next tree = "+tree);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        isRemovingAllTrees = false;
        currentRemovingTree = null;
        if (treesForRemoval!=null)
            treesForRemoval.clear();
        treesForRemoval = null;
        ZeeConfig.removePlayerText();
    }

    public static boolean removeStumpMaybe(Gob stump) throws InterruptedException {
        boolean droppedBucket = false;

        //move closer to stump
        gobClick(stump,1);
        if(!waitPlayerDistToGob(stump,25)){
            println("couldn't get close to stump?");
            return false;
        }

        //drop bucket if present
        if (ZeeManagerItemClick.isItemEquipped("bucket")) {
            if (ZeeConfig.getStamina() < 100) {
                ZeeManagerItemClick.drinkFromBeltHandsInv();
                sleep(PING_MS*2);
                waitNotPlayerPose(ZeeConfig.POSE_PLAYER_DRINK);
            }
            ZeeManagerItemClick.getEquipory().dropItemByNameContains("/bucket");
            droppedBucket = true;
        }

        //equip shovel
        ZeeManagerItemClick.equipBeltItem("shovel");
        if (!waitItemEquipped("shovel")){
            println("couldnt equip shovel ?");
            return false;
        }
        waitNotHoldingItem();//wait possible switched item go to belt?

        //remove stump
        destroyGob(stump);

        //reequip bucket if dropped
        if (droppedBucket){
            waitPlayerPose(ZeeConfig.POSE_PLAYER_IDLE);
            Gob bucket = ZeeConfig.getClosestGobByNameContains("/bucket");
            if (bucket!=null){
                if (ZeeManagerItemClick.pickupHandItem("shovel")) {
                    if(ZeeManagerItemClick.dropHoldingItemToBeltOrInv()) {
                        sleep(PING_MS);
                        ZeeConfig.clickRemoveCursor();
                        waitCursor(ZeeConfig.CURSOR_ARW);
                        sleep(PING_MS);
                        gobClick(bucket, 3);
                        if (waitHoldingItem())
                            ZeeManagerItemClick.equipEmptyHand();
                        else
                            println("couldnt pickup da bucket");
                    }
                }else {
                    println("couldnt return shovel to belt?");
                }
            }else{
                println("bucket gob not found");
            }
        }

        // maybe stump was removed
        return true;
    }

    public static void addItemsToGob(List<WItem> invItens, int num, Gob gob){
        new ZeeThread(){
            public void run() {
                try{
                    ZeeConfig.addPlayerText("adding");
                    if(invItens.size() < num){
                        ZeeConfig.msgError("Need "+num+" item(s)");
                        return;
                    }
                    boolean exit = false;
                    int added = 0;
                    ZeeConfig.lastMapViewClickButton = 2;//prepare for cancel click
                    while(  !ZeeConfig.isTaskCanceledByGroundClick()
                            && !exit
                            && added < num
                            && invItens.size() > 0)
                    {
                        if(ZeeManagerItemClick.pickUpItem(invItens.get(0))){
                            itemActGob(gob,0);
                            if(waitNotHoldingItem()){
                                invItens.remove(0);
                                added++;
                            }else{
                                ZeeConfig.msgError("Couldn't right click "+gob.getres().basename());
                                exit = true;
                            }
                        }else {
                            ZeeConfig.msgError("Couldn't pickup inventory item");
                            exit = true;
                        }
                    }
                    ZeeConfig.removePlayerText();
                    ZeeConfig.addGobTextTempMs(gob,"Added "+added+" item(s)",3000);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public static void addFuelGobMenu(Gob gob, String petalName) {
        String gobName = gob.getres().name;
        if(gobName.endsWith("oven") && petalName.equals(ZeeFlowerMenu.STRPETAL_ADD4BRANCH)){
            /*
                fuel oven with 4 branches
             */
           List<WItem> branches = ZeeConfig.getMainInventory().getWItemsByName("branch");
           if(branches.size() < 4){
               ZeeConfig.gameUI.msg("Need 4 branches to fuel oven");
               return;
           }
           boolean exit = false;
           int added = 0;
           while(!exit && added<4 && branches.size() > 0){
               if(ZeeManagerItemClick.pickUpItem(branches.get(0))){
                   itemActGob(gob,0);
                   if(waitNotHoldingItem()){
                       branches.remove(0);
                       added++;
                   }else{
                       ZeeConfig.gameUI.msg("Couldn't right click oven");
                       exit = true;
                   }
               }else {
                   ZeeConfig.gameUI.msg("Couldn't pickup branch");
                   exit = true;
               }
           }
           ZeeConfig.gameUI.msg("Added "+added+" branches");
        }
        else if(gobName.endsWith("smelter")){
            /*
                fuel smelter with 9 or 12 coal
             */
            int num = 12;
            if (petalName.equals(ZeeFlowerMenu.STRPETAL_ADD9COAL))
                num = 9;
            final int numCoal = num;
            List<WItem> coal = ZeeConfig.getMainInventory().getWItemsByName("coal");
            if(coal.size() < numCoal){
                ZeeConfig.gameUI.msg("Need "+numCoal+" coal to fuel smelter");
                return;
            }
            boolean exit = false;
            int added = 0;
            while(!exit && added<numCoal && coal.size() > 0){
                if(ZeeManagerItemClick.pickUpItem(coal.get(0))){
                    itemActGob(gob,0);
                    if(waitNotHoldingItem()){
                        coal.remove(0);
                        added++;
                    }else{
                        ZeeConfig.gameUI.msg("Couldn't right click smelter");
                        exit = true;
                    }
                }else {
                    ZeeConfig.gameUI.msg("Couldn't pickup coal");
                    exit = true;
                }
            }
            ZeeConfig.gameUI.msg("Added "+added+" coal");
        }
    }

    private boolean isFuelAction(String gobName) {
        if (gobName.endsWith("oven") || gobName.endsWith("smelter")){
            return true;
        }
        return false;
    }

    private static void harvestOneTrellis(Gob gob) {
        if(ZeeManagerItemClick.pickupBeltItem("scythe")){
            //hold scythe for user unequip it
        }else if(ZeeManagerItemClick.getLeftHandName().endsWith("scythe")){
            //hold scythe for user unequip it
            ZeeManagerItemClick.unequipLeftItem();
        }else{
            //no scythe around, just harvest
            clickGobPetal(gob,"Harvest");
        }
    }

    public static boolean isGobStockpile(String gobName) {
        return gobName.startsWith("gfx/terobjs/stockpile");
    }

    private static boolean isGobGroundItem(String gobName) {
        return gobName.startsWith("gfx/terobjs/items/");
    }

    public static boolean isLongMidClick() {
        return clickDiffMs >= LONG_CLICK_MS;
    }

    public static boolean isShortMidClick() {
        return clickDiffMs < LONG_CLICK_MS;
    }

    private static boolean isInspectGob(String gobName){
        if(isGobTree(gobName) || isGobBush(gobName) || isGobBoulder(gobName))
            return true;
        String list = "/meatgrinder,/potterswheel,/well,/dframe,/smokeshed,"
                +"/smelter,/primsmelter,/crucible,/steelcrucible,/fineryforge,/kiln,/tarkiln,/oven,"
                +"/compostbin,/gardenpot,/beehive,/htable,/bed-sturdy,/boughbed,/alchemiststable,"
                +"/gemwheel,/spark,/cauldron,/churn,/chair-rustic,"
                +"/royalthrone,curdingtub,log,/still,/oldtrunk,/anvil,"
                +"/loom,/swheel,knarr,snekkja,dock,/ropewalk,"
                +"/ttub,/cheeserack,/dreca,/glasspaneframe,/castingmold";
        return isGobInListContains(gobName, list);
    }


    public static boolean isGobMineSupport(String gobName) {
        String list = "/minebeam,/column,/minesupport,/naturalminesupport,/towercap";
        return isGobInListEndsWith(gobName, list);
    }


    private static boolean isGobLiftable(String gobName) {
        if(isGobBoulder(gobName))
            return true;
        String endList = "/meatgrinder,/potterswheel,/iconsign,/rowboat,/dugout,/wheelbarrow,"
                +"/compostbin,/gardenpot,/beehive,/htable,/bed-sturdy,/boughbed,/alchemiststable,"
                +"/gemwheel,/ancestralshrine,/spark,/cauldron,/churn,/wardrobe,"
                +"/table-rustic,/table-stone,/chair-rustic,/stonethrone,/royalthrone,"
                +"/trough,curdingtub,/plow,/barrel,/still,log,/oldtrunk,chest,/anvil,"
                +"/cupboard,/studydesk,/demijohn,/quern,/wreckingball-fold,/loom,/swheel,"
                +"/ttub,/cheeserack,/archerytarget,/dreca,/glasspaneframe,/runestone,"
                +"/crate,/foodtrough";
        return isGobInListEndsWith(gobName,endList);
    }

    private static boolean isGobBoulder(String gobName) {
        return gobName.startsWith("gfx/terobjs/bumlings/") &&
               !gobName.startsWith("gfx/terobjs/bumlings/ras");
    }

    public static boolean isGobBush(String gobName) {
        return gobName.startsWith("gfx/terobjs/bushes");
    }

    public static boolean isGobTreeStump(String gobName) {
        return gobName.startsWith("gfx/terobjs/trees/") && gobName.endsWith("stump");
    }

    public static boolean isGobTree(String gobName) {
        return gobName.startsWith("gfx/terobjs/trees/") && !gobName.endsWith("log") && !gobName.endsWith("stump") && !gobName.endsWith("oldtrunk");
    }

    public static boolean isGobTreeLog(String gobName){
        return gobName.startsWith("gfx/terobjs/trees/") && gobName.endsWith("log");
    }

    public static boolean isBarrelTakeAll(Gob gob) {
        String gobName = gob.getres().name;
        if(!gobName.endsWith("barrel") || isBarrelEmpty(gob)){
            return false;
        }
        String list = "barley,carrot,cucumber,flax,grape,hemp,leek,lettuce,millet"
                +",pipeweed,poppy,pumpkin,wheat,turnip,wheat,barley,wheatflour,barleyflour,milletflour"
                +",ashes,gelatin,cavedust,caveslime,chitinpowder"
                +",colorred,coloryellow,colorblue,colorgreen,colorblack,colorwhite,colorgray"
                +",colororange,colorbeige,colorbrown,colorlime,colorturquoise,colorteal,colorpurple";
        return getOverlayNames(gob).stream().anyMatch(overlayName -> {
            return list.contains(overlayName.replace("gfx/terobjs/barrel-",""));
        });
    }

    public static void barrelTakeAllSeeds(Gob gob){
        try{
            // shift+rclick last barrel
            ZeeManagerGobClick.gobClick(gob, 3, UI.MOD_SHIFT);

            //wait getting to the barrel
            waitPlayerIdleFor(1);

            if (ZeeConfig.distanceToPlayer(gob) > ZeeManagerFarmer.MIN_ACCESSIBLE_DIST) {
                ZeeConfig.msg("barrel unreachable");
                return;
            }

            ZeeConfig.addPlayerText("taking contents...");

            while (!ZeeManagerGobClick.isBarrelEmpty(gob) && !isInventoryFull()) {
                ZeeManagerGobClick.gobClick(gob, 3, UI.MOD_SHIFT);
                Thread.sleep(PING_MS);
                if (ZeeConfig.isTaskCanceledByGroundClick())
                    break;
            }

            //if holding seed, store in barrel
            waitHoldingItem();
            ZeeManagerGobClick.itemActGob(gob, 0);

            if (isInventoryFull())
                ZeeConfig.msg("Inventory full");
            else if (!ZeeConfig.isTaskCanceledByGroundClick())
                ZeeConfig.msg("Took everything");

        }catch(Exception e){
            e.printStackTrace();
        }
        ZeeConfig.removePlayerText();
    }

    private void barrelTakeAllSeeds() {
        barrelTakeAllSeeds(gob);
    }

    public static boolean isInventoryFull() {
        return ZeeConfig.getMainInventory().getNumberOfFreeSlots() == 0;
    }

    public static void destroyGob(Gob gob) {
        ZeeConfig.gameUI.menu.wdgmsg("act","destroy","0");
        gobClick(gob,1);
    }
    private void destroyGob() {
        destroyGob(gob);
    }

    public static void liftGob(Gob gob) {
        if(isGobBush(gob.getres().name)) {
            ZeeManagerItemClick.equipBeltItem("shovel");
            waitItemEquipped("shovel");
        }
        ZeeConfig.gameUI.menu.wdgmsg("act", "carry","0");
        waitCursor(ZeeConfig.CURSOR_HAND);
        gobClick(gob,1);
        waitPlayerDistToGob(gob,0);
    }

    static boolean isMidclickInspecting = false; // used by inspect tooltip feature
    public static void inspectGob(Gob gob){
        isMidclickInspecting = true;
        ZeeConfig.gameUI.menu.wdgmsg("act","inspect","0");
        gobClick(gob, 1);
        ZeeConfig.clickRemoveCursor();
        isMidclickInspecting = false;
    }

    public static boolean isGobTrellisPlant(String gobName) {
        return isGobInListEndsWith(gobName, "plants/wine,plants/hops,plants/pepper,plants/peas,plants/cucumber");
    }

    public static boolean isGobCrop(String gobName){
        return isGobInListEndsWith(gobName,"plants/carrot,plants/beet,plants/yellowonion,plants/redonion,"
                +"plants/leek,plants/lettuce,plants/pipeweed,plants/hemp,plants/flax,"
                +"plants/turnip,plants/millet,plants/barley,plants/wheat,plants/poppy,"
                +"plants/pumpkin,plants/fallowplant"
        );
    }

    public static boolean isGobCookContainer(String gobName) {
        String containers ="cupboard,chest,crate,basket,box,coffer,cabinet";
        return isGobInListEndsWith(gobName,containers);
    }


    private static boolean isGobInListContains(String gobName, String list) {
        String[] names = list.split(",");
        for (int i = 0; i < names.length; i++) {
            if (gobName.contains(names[i])){
                return true;
            }
        }
        return false;
    }

    private static boolean isGobInListEndsWith(String gobName, String list) {
        String[] names = list.split(",");
        for (int i = 0; i < names.length; i++) {
            if (gobName.endsWith(names[i])){
                return true;
            }
        }
        return false;
    }

    private boolean isGobInListStartsWith(String gobName, String list) {
        String[] names = list.split(",");
        for (int i = 0; i < names.length; i++) {
            if (gobName.startsWith(names[i])){
                return true;
            }
        }
        return false;
    }

    public static boolean clickGobPetal(Gob gob, String petalName) {
        if (gob==null){
            //println(">clickGobPetal gob null");
            return false;
        }
        //make sure cursor is arrow before clicking gob
        if (!ZeeConfig.getCursorName().contentEquals(ZeeConfig.CURSOR_ARW)){
            ZeeConfig.clickRemoveCursor();
            if (!waitCursor(ZeeConfig.CURSOR_ARW))
                return false;
        }
        gobClick(gob,3);
        if(waitFlowerMenu()){
            choosePetal(getFlowerMenu(), petalName);
            return waitNoFlowerMenu();
        }else{
            //println("clickGobPetal > no flower menu?");
            return false;
        }
    }

    // if gob has flowermenu returns true
    public static boolean gobExistsBecauseFlowermenu(Gob gob) {

        boolean ret;

        //no gob, no menu
        if (ZeeConfig.isGobRemoved(gob)) {
            //println(">gobHasFlowermenu gob is inexistent");
            return false;
        }

        //select arrow cursor if necessary
        if (!ZeeConfig.getCursorName().contentEquals(ZeeConfig.CURSOR_ARW)) {
            ZeeConfig.clickRemoveCursor();
            waitCursor(ZeeConfig.CURSOR_ARW);
        }

        //disable auto options before clicking gob
        boolean butchBackup = ZeeConfig.butcherMode;
        ZeeConfig.butcherMode = false;
        ZeeConfig.autoClickMenuOption = false;

        //click gob and wait menu
        gobClick(gob, 3);
        if (waitFlowerMenu()) {
            // menu opened means gob exist
            ZeeConfig.cancelFlowerMenu();
            waitNoFlowerMenu();
            //println("gobHasFlowermenu > true");
            ret = true;
        } else {
            //println("gobHasFlowermenu > cant click gob");
            ret = false;
        }

        //restore settings and return
        ZeeConfig.butcherMode = butchBackup;
        ZeeConfig.autoClickMenuOption = Utils.getprefb("autoClickMenuOption", true);
        return ret;
    }

    private boolean isGobButchable(String gobName){
        return isGobInListEndsWith(
            gobName,
            "/stallion,/mare,/foal,/hog,/sow,/piglet,"
            +"/billy,/nanny,/kid,/sheep,/lamb,/cattle,/calf,"
            +"/wildhorse,/aurochs,/mouflon,/wildgoat,"
            +"/adder,/badger,/bear,/boar,/beaver,/deer,/reindeer,/reddeer,/fox,"
            +"/greyseal,/otter,/caveangler,/boreworm,/caverat,"
            +"/lynx,/mammoth,/moose,/troll,/walrus,/wolf,/wolverine"
        );
    }

    public static boolean isGobHorse(String gobName) {
        return isGobInListEndsWith(gobName, "stallion,mare,horse");
    }

    private static boolean isGobFireSource(String gobName) {
        return isGobInListEndsWith(gobName,"brazier,pow,snowlantern,/bonfire");
    }

    /**
     * Itemact with gob, to fill trough with item in hand for example
     * @param mod 1 = shift, 2 = ctrl, 4 = alt  (3 = ctrl+shift ?)
     */
    public static void itemActGob(Gob g, int mod) {
        ZeeConfig.gameUI.map.wdgmsg("itemact", Coord.z, g.rc.floor(OCache.posres), mod, 0, (int) g.id, g.rc.floor(OCache.posres), 0, -1);
    }

    public static void gobClick(Gob g, int btn, int mod, int x) {
        ZeeConfig.gameUI.map.wdgmsg("click", ZeeConfig.getCenterScreenCoord(), g.rc.floor(OCache.posres), btn, mod, 0, (int)g.id, g.rc.floor(OCache.posres), 0, x);
    }

    public static void gobClick(Gob g, int btn) {
        ZeeConfig.gameUI.map.wdgmsg("click", ZeeConfig.getCenterScreenCoord(), g.rc.floor(OCache.posres), btn, 0, 0, (int)g.id, g.rc.floor(OCache.posres), 0, -1);
    }

    public static void gobClick(Gob g, int btn, int mod) {
        ZeeConfig.gameUI.map.wdgmsg("click", ZeeConfig.getCenterScreenCoord(), g.rc.floor(OCache.posres), btn, mod, 0, (int)g.id, g.rc.floor(OCache.posres), 0, -1);
    }

    public static double distanceCoordGob(Coord2d c, Gob gob) {
        return c.dist(gob.rc);
    }

    // return Gob or null
    public static Gob findGobById(long id) {
        return ZeeConfig.gameUI.ui.sess.glob.oc.getgob(id);
    }

    // "gfx/terobjs/barrel-flax"
    public static List<String> getOverlayNames(Gob gob) {
        List<String> ret = new ArrayList<>();
        for (Gob.Overlay ol : gob.ols) {
            if(ol.res != null)
                ret.add(ol.res.get().name);
        }
        return ret;
    }

    public static Gob getGobFromClickable(Clickable ci) {
        if(ci instanceof Gob.GobClick) {
            return ((Gob.GobClick) ci).gob;
        } else if(ci instanceof Composited.CompositeClick) {
            Gob.GobClick gi = ((Composited.CompositeClick) ci).gi;
            return gi != null ? gi.gob : null;
        }
        return null;
    }
}
