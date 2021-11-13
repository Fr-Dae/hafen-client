package haven;

import java.util.List;
import java.util.stream.Collectors;

/*
    Mid-click auto-equips items from belt/hands.
    Drinks from vessels: waterskin, bucket.
 */
public class ZeeClickItemManager extends ZeeThread{

    private final WItem wItem;
    String itemName;
    String leftHandItemName, rightHandItemName, itemSourceWindow;
    boolean cancelManager = false;
    public static Equipory equipory;
    static Inventory invBelt = null;
    public static long clickStartMs, clickEndMs, clickDiffMs;
    

    public ZeeClickItemManager(WItem wItem) {
        clickDiffMs = clickEndMs - clickStartMs;
        this.wItem = wItem;
        equipory = ZeeConfig.windowEquipment.getchild(Equipory.class);
        leftHandItemName = (equipory.leftHand==null ? "" : equipory.leftHand.item.getres().name);
        rightHandItemName = (equipory.rightHand==null ? "" : equipory.rightHand.item.getres().name);
        try{
            itemName = wItem.item.getres().name;//clicked item, started manager
            itemSourceWindow = wItem.getparent(Window.class).cap.text;//save source window name before pickup
        }catch (NullPointerException e){
            //error caused by midClicking again before task ending
            cancelManager = true;
        }
        //println(itemName);
    }

    @Override
    public void run() {

        if(cancelManager)
            return;

        try{

            //kill all, eat all, etc...
            if(actOnAllInventoryItems()){
                return;
            }

            //sort-transfer
            if(!itemSourceWindow.equalsIgnoreCase("Belt") && !itemSourceWindow.equalsIgnoreCase("Equipment")){
                if(transferWindowOpen()) { //avoid belt transfer?
                    if(isLongClick())
                        wItem.wdgmsg("transfer-sort", wItem.item, true);//ascending order
                    else
                        wItem.wdgmsg("transfer-sort", wItem.item, false);//descending order
                    return;
                }
            }

            //check for windows belt/equips ?
            if(ZeeConfig.getWindow("Belt")==null){
                ZeeConfig.gameUI.msg("no belt window");
                return;
            }
            if(ZeeConfig.getWindow("Equipment")==null){
                ZeeConfig.gameUI.msg("no equips window");
                return;
            }

            if(isItemDrinkingVessel()) {
                drinkFrom();
            }
            else if (isItemSack()) { // travellersack or bindle

                if(isSourceBeltWindow()) {//send to equipory
                    if(isLeftHandEmpty() || isRightHandEmpty()) {
                        pickUpItem();
                        equipEmptyHand();
                    }else if (!isItemSack(leftHandItemName)) {//avoid switching sack for sack
                        pickUpItem();
                        equipLeftOccupiedHand();
                        trySendItemToBelt();
                    }else if(!isItemSack(rightHandItemName)) {
                        pickUpItem();
                        equipRightOccupiedHand();
                        trySendItemToBelt();
                    }else { //both hands are sacks?
                        ZeeConfig.gameUI.msg("both hand sacks");
                    }

                    if(isHoldingItem()) {//equip was a switch or failed
                        ZeeConfig.gameUI.msg("couldn't switch sack");
                        trySendItemToBelt();
                    }
                }else if(isSourceEquipsWindow()){//send to belt
                    pickUpItem();
                    if(isHoldingItem()){ //unequip sack was successfull
                        if(!trySendItemToBelt())
                            println("belt full?");
                    }
                }

            }
            else if(isTwoHandedItem()) {//2 handed item

                if(isSourceBeltWindow()) {
                    if(!isLeftHandEmpty() && isTwoHandedItem(leftHandItemName)) {
                        //switch 2handed item for another 2handed item
                        pickUpItem();
                        equipLeftOccupiedHand();
                        trySendItemToBelt();
                    }else if(isLeftHandEmpty() || isRightHandEmpty()) {
                        //switch for 2handed item for 1handed equipped, or none equipped
                        pickUpItem();
                        if(!isLeftHandEmpty())
                            equipLeftOccupiedHand();
                        else if(!isRightHandEmpty())
                            equipRightOccupiedHand();
                        else
                            equipLeftEmptyHand();
                        trySendItemToBelt();
                    }else if(!isLeftHandEmpty() && !isRightHandEmpty()){
                        //switch 2handed item for 2 separate items
                        if (ZeeClickItemManager.getInvBelt().getNumberOfFreeSlots() > 0) {
                            unequipLeftItem();//unequip 1st item
                            if(trySendItemToBelt()){
                                pickUpItem();
                                equipRightOccupiedHand();//switch for 2nd item
                                trySendItemToBelt();
                            }
                        }
                    }
                }
                else if(isSourceEquipsWindow()) {
                    if (ZeeClickItemManager.getInvBelt().getNumberOfFreeSlots() > 0) {
                        //send to belt if possible
                        pickUpItem();
                        trySendItemToBelt();
                    }
                }

            }
            else{// 1handed item

                if(isSourceBeltWindow()) { // send to equipory
                    if(isLeftHandEmpty() || isRightHandEmpty()) {//1 item equipped
                        pickUpItem();
                        equipEmptyHand();
                    }else { // 2 hands occupied
                        if(isTwoHandedItem(getLeftHandName())) {
                            //switch 1handed for 2handed
                            pickUpItem();
                            equipLeftOccupiedHand();
                            trySendItemToBelt();
                        }if(isShield()) {
                            //avoid replacing 1handed swords
                            pickUpItem();
                            if (!isOneHandedSword(leftHandItemName)){
                                equipLeftOccupiedHand();
                                trySendItemToBelt();
                            }else if (!isOneHandedSword(rightHandItemName)){
                                equipRightOccupiedHand();
                                trySendItemToBelt();
                            }else
                                println("2 swords equipped? let user decide...");
                        }else if(isOneHandedSword()) {
                            //avoid replacing shields
                            pickUpItem();
                            if (!isShield(leftHandItemName)){
                                equipLeftOccupiedHand();
                                trySendItemToBelt();
                            }else if (!isShield(rightHandItemName)){
                                equipRightOccupiedHand();
                                trySendItemToBelt();
                            }else//2 shields?
                                println("2 shields equipped? let user decide...");
                        }else if(!isItemSack(leftHandItemName)) {
                            //switch 1handed item for left hand
                            pickUpItem();
                            equipLeftOccupiedHand();
                            trySendItemToBelt();
                        }else if(!isItemSack(rightHandItemName)) {
                            //switch 1handed item for right hand
                            pickUpItem();
                            equipRightOccupiedHand();
                            trySendItemToBelt();
                        }else{
                            // switch 1handed item for one of both sacks equipped
                            pickUpItem();
                            equipLeftOccupiedHand();
                            if (!isItemSack(getHoldingItemName())){
                                //couldn't switch, try other sack
                                equipRightOccupiedHand();
                            }
                            trySendItemToBelt();
                        }
                    }

                }else if(isSourceEquipsWindow()){//send to belt
                    pickUpItem();
                    if(!trySendItemToBelt()) {
                        ZeeConfig.gameUI.msg("Belt is full");
                    }
                }

            }

        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getHoldingItemName() {
        if(ZeeConfig.gameUI.vhand==null)
            return "";
        try {
            return ZeeConfig.gameUI.vhand.item.getres().name;
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    private boolean actOnAllInventoryItems() {

        if(!wItem.ui.modctrl || !isLongClick())// require Ctrl + LongClick
            return false;

        // kill all inventory cocoons
        if(itemName.endsWith("silkcocoon") || itemName.endsWith("chrysalis")){
            Inventory inv = wItem.getparent(Inventory.class);
            List<WItem> items = inv.children(WItem.class).stream()
                    .filter(wItem1 -> wItem1.item.getres().name.endsWith("silkcocoon") || wItem1.item.getres().name.endsWith("chrysalis"))
                    .collect(Collectors.toList());
            for (WItem w: items) {
                ZeeConfig.scheduleClickPetal("Kill");
                try {
                    clickWItem(w);
                    int max = (int) TIMEOUT_MS;
                    while(max>0 && ZeeConfig.clickPetal){//wait FlowerMenu end and set clickPetal to false
                        max -= SLEEP_MS;
                        Thread.sleep(SLEEP_MS);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    ZeeConfig.resetClickPetal();
                    ZeeConfig.gameUI.msg("Kill cocoons: "+e.getMessage());
                    return false;
                }
            }
            ZeeConfig.gameUI.msg(items.size()+" cocoons clicked");
            return true;
        }

        return false;
    }

    public static void clickWItem(WItem item){
        item.item.wdgmsg("iact", item.item.c.div(2), item.ui.modflags());
    }

    private boolean isLongClick() {
        return clickDiffMs > LONG_CLICK_MS;
    }

    private boolean transferWindowOpen() {
        String windowsNames = getWindowsNames();
        String[] containers = (
            "Knarr,Snekkja,Wagon,Cupboard,Chest,Table,Crate,Saddlebags,Basket,Box,"
            +"Furnace,Smelter,Desk,Trunk,Shed,Coffer,Packrack,Strongbox,Stockpile,"
            +"Tub,Compost Bin,Extraction Press,Cheese Rack,Herbalist Table,Frame,"
            +"Chicken Coop,Rabbit Hutch,Archery Target,Creel,Oven,Steel crucible,"
            +"Cauldron,Pane mold,Kiln"
        ).split(",");
        for (String contName: containers) {
            if (windowsNames.contains(contName))
                return true;
        }
        return false;
    }

    private String getWindowsNames() {
        return ZeeConfig.gameUI.children(Window.class).stream().map(window -> window.cap.text).collect(Collectors.joining(","));
    }

    private void drinkFrom() {
        ZeeConfig.scheduleClickPetal("Drink");
        clickWItem(wItem);
    }

    private boolean isItemDrinkingVessel() {
        return isItemDrinkingVessel(itemName);
    }
    private boolean isItemDrinkingVessel(String name) {
        String[] items = {"waterskin","waterflask","bucket"};
        for (int i = 0; i < items.length; i++) {
            if (name.contains(items[i])){
                return true;
            }
        }
        return false;
    }

    private boolean isOneHandedSword() {
        return isOneHandedSword(itemName);
    }
    private boolean isOneHandedSword(String name) {
        String[] items = {"fyrdsword","hirdsword","bronzesword"};
        for (int i = 0; i < items.length; i++) {
            if (name.contains(items[i])){
                return true;
            }
        }
        return false;
    }

    private boolean isOneHandedWeapon() {
        return isOneHandedWeapon(itemName);
    }
    private boolean isOneHandedWeapon(String name) {
        String[] items = {"fyrdsword","hirdsword","bronzesword","axe-m","woodsmansaxe","stoneaxe","butcherscleaver","sling"};
        for (int i = 0; i < items.length; i++) {
            if (name.contains(items[i])){
                return true;
            }
        }
        return false;
    }

    private boolean isShield() {
        return isShield(itemName);
    }
    private boolean isShield(String name) {
        return name.contains("roundshield");
    }

    public static boolean trySendItemToBelt() {
        if(!isHoldingItem())
            return false;
        try{
            List<Coord> freeSlots = ZeeClickItemManager.getInvBelt().getFreeSlots();
            if (freeSlots.size()==0)
                return false;//belt full
            Coord c = freeSlots.get(0);
            ZeeClickItemManager.getInvBelt().wdgmsg("drop", c);
            return waitFreeHand();
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isHoldingItem() {
        return (ZeeConfig.gameUI.vhand != null);
    }

    private boolean isSourceBeltWindow() {
        return (itemSourceWindow.equalsIgnoreCase("Belt"));
    }

    private boolean isSourceEquipsWindow() {
        return (itemSourceWindow.equalsIgnoreCase("Equipment"));
    }

    /*
        equip occupied hand and wait
     */
    private void equipLeftOccupiedHand() {
        equipory.wdgmsg("drop", 6);
        waitOccupiedHand();
    }
    private void equipRightOccupiedHand() {
        equipory.wdgmsg("drop", 7);
        waitOccupiedHand();
    }

    /*
        equip empty hand and wait
     */
    private void equipLeftEmptyHand() {
        equipory.wdgmsg("drop", 6);
        waitFreeHand();
    }
    private void equipRightEmptyHand() {
        equipory.wdgmsg("drop", 7);
        waitFreeHand();
    }

    private boolean equipEmptyHand() {
        if(isLeftHandEmpty())
            equipLeftEmptyHand();
        else if(isRightHandEmpty())
            equipRightEmptyHand();
        return waitFreeHand();
    }

    private boolean isLeftHandEmpty() {
        return (equipory.leftHand==null);
    }

    private boolean isRightHandEmpty() {
        return (equipory.rightHand==null);
    }


    private boolean pickUpItem() {
        return pickUpItem(wItem);
    }
    public static boolean pickUpItem(WItem wItem) {
        wItem.item.wdgmsg("take", new Coord(wItem.sz.x / 2, wItem.sz.y / 2));
        return waitOccupiedHand();
    }

    public static boolean unequipLeftItem() {
        if(equipory.leftHand==null)
            return true;
        equipory.leftHand.item.wdgmsg("take", new Coord(equipory.leftHand.sz.x/2, equipory.leftHand.sz.y/2));
        return waitOccupiedHand();
    }

    public static boolean unequipRightItem() {
        if(equipory.rightHand==null)
            return true;
        equipory.rightHand.item.wdgmsg("take", new Coord(equipory.rightHand.sz.x/2, equipory.rightHand.sz.y/2));
        return waitOccupiedHand();
    }

    private boolean isItemSack() {
        return isItemSack(itemName);
    }

    private boolean isItemSack(String name) {
        return name.endsWith("travellerssack") || name.endsWith("bindle");
    }


    private boolean isTwoHandedItem() {
        return isTwoHandedItem(itemName);
    }
    public static boolean isTwoHandedItem(String name) {
        String[] items = {"scythe","pickaxe","shovel","b12axe",
                "boarspear","cutblade","sledgehammer",
                "huntersbow","rangersbow","dowsingrod"};
        for (int i = 0; i < items.length; i++) {
            if (name.contains(items[i])){
                return true;
            }
        }
        return false;
    }

    public static Inventory getInvBelt() {
        if (invBelt==null) {
            Window w = ZeeConfig.getWindow("Belt");
            if(w!=null)
                invBelt = w.getchild(Inventory.class);
        }
        return  invBelt;
    }

    public static boolean pickupBeltItem(String name) {
        try {
            WItem witem = getInvBelt().getWItemsByName(name).get(0);
            return pickUpItem(witem);
        }catch (Exception e){
            return false;
        }
    }

    public static boolean pickupHandItem(String name) {
        try {
            if(getLeftHandName().contains(name))
                return pickUpItem(getLeftHand());
            else if(getRightHandName().contains(name))
                return pickUpItem(getRightHand());
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    public static WItem getBeltWItem(String name) {
        try {
            WItem witem = getInvBelt().getWItemsByName(name).get(0);
            return witem;
        }catch (Exception e){
            return null;
        }
    }

    public static boolean isItemEquipped(String name){
        try {
            Equipory eq = ZeeConfig.windowEquipment.getchild(Equipory.class);
            return eq.leftHand.item.getres().name.contains(name)
                    || eq.rightHand.item.getres().name.contains(name);
        }catch (Exception e){
            return false;
        }
    }

    public static Equipory getEquipory(){
        if (equipory==null)
            equipory = ZeeConfig.windowEquipment.getchild(Equipory.class);
        return equipory;
    }

    public static WItem getLeftHand() {
        return getEquipory().leftHand;
    }
    public static WItem getRightHand() {
        return getEquipory().rightHand;
    }

    public static String getLeftHandName() {
        if(getEquipory().leftHand==null)
            return "";
        else
            return getEquipory().leftHand.item.getres().name;
    }
    public static String getRightHandName() {
        if(getEquipory().rightHand==null)
            return "";
        else
            return getEquipory().rightHand.item.getres().name;
    }

    public static void equipItem(String name) {
        if(ZeeClickItemManager.isItemEquipped(name))
            return;
        WItem item = ZeeClickItemManager.getBeltWItem(name);
        new ZeeClickItemManager(item).start();//use equipManager logic
    }
}
