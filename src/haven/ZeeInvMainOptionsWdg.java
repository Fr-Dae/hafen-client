package haven;

public class ZeeInvMainOptionsWdg extends Widget {

    Label labelDrop, labelCount;
    public static CheckBox cbSeeds, cbSoil, cbButcher, cbPiler, cbAutomenu;
    Widget invSlots;

    public ZeeInvMainOptionsWdg(String windowCap) {

        if(windowCap.trim().equalsIgnoreCase("Inventory"))
            invMain();
        invSlots = ZeeConfig.windowInvMain.getchild(Inventory.class);
    }

    private void invMain() {

        int x = 0;

        add(cbAutomenu = new CheckBox("automenu") {
            {
                a = ZeeConfig.autoClickMenuOption;
            }

            public void set(boolean val) {
                Utils.setprefb("autoClickMenuOption", val);
                ZeeConfig.autoClickMenuOption = val;
                a = val;
            }
        }, x, 0);

        x += cbAutomenu.sz.x + 5;

        add(cbButcher = new CheckBox("butch") {
            {
                a = ZeeConfig.butcherMode;
            }

            public void set(boolean val) {
                ZeeConfig.butcherMode = val;
                a = val;
            }
        }, x, 0);

        x = 0;

        add(cbSeeds = new CheckBox("dropseed") {
            {
                a = ZeeConfig.dropSeeds;
            }

            public void set(boolean val) {
                ZeeConfig.dropSeeds = val;
                a = val;
            }
        }, x, 15);

        x += cbSeeds.sz.x + 5;

        add(cbSoil = new CheckBox("dropsoil") {
            {
                a = ZeeConfig.dropSoil;
            }

            public void set(boolean val) {
                ZeeConfig.dropSoil = val;
                a = val;
            }
        }, x, 15);

        add(labelCount = new Label(""), 0, 0);
        pack();
    }

    public void updateLabelCount(String itemName, Integer count) {
        //update counter text
        labelCount.settext( itemName.replaceAll(".+/","")+ "(" + count +")");
        repositionLabelCount();
    }

    public void repositionLabelCount() {
        //position counter at bottom right
        int x = invSlots.sz.x - labelCount.sz.x;
        int y = invSlots.sz.y + 20;
        labelCount.c = new Coord(x, y);
        pack();
    }
}
