package haven;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class ZeeMouseoverActionMenu {

    static Widget menu;
    static boolean isMouseOver = false;

    public static void mouseMoved(GameUI.MenuButton menuButton, Coord c) {
        if (menuButton.checkhit(c)) {
            if (!isMouseOver) {
                println("root ismouseover true");
                isMouseOver = true;
                menuStart();
            }
        } else {
            if (isMouseOver && !MenuWdgGroup.hit) {
                println("root ismouseover false");
                isMouseOver = false;
            }
        }
    }

    private static void menuStart() {
        if (!isMouseOver || menu!=null)
            return;
        new ZeeThread(){
            public void run() {
                int countMs = 0;
                try {
                    while (isMouseOver){
                        if (countMs > 950)
                            break;
                        sleep(50);
                        countMs += 50;
                    }
                    if (isMouseOver)
                        menuStart2();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }.start();
    }


    private static void menuStart2() {
        if (menu==null){
            menu = ZeeConfig.gameUI.add(new MenuWdgGroup(curbtns));
        }
    }

    private static void setBottomRightCoord(Coord br,Widget menu) {
        int x = br.x - menu.sz.x + 33;
        int y = br.y - menu.sz.y;
        menu.c = Coord.of(x,y);
    }

    static List<MenuGrid.PagButton> curbtns;
    static void menuGridButtons(List<MenuGrid.PagButton> btns) {
        curbtns = btns;
    }

    public static void exitMenu(){
        if (menu!=null) {
            menu.destroy();
            menu = null;
            isMouseOver = false;
        }
    }

    public static void println(String s) {
        System.out.println(s);
    }



    static class MenuWdgGroup extends Widget{

        List<MenuGrid.PagButton> btns;
        BufferedImage bg;
        static boolean hit;

        public MenuWdgGroup(List<MenuGrid.PagButton> curbtns){
            this.btns = curbtns;
            for (int i = 0; i < curbtns.size(); i++) {
                this.add(new MenuWdgLine(curbtns.get(i), i));
            }
            this.pack();
            this.bg = ZeeManagerIcons.imgRect( this.sz.x, this.sz.y, ZeeConfig.intToColor(ZeeConfig.simpleWindowColorInt), ZeeConfig.simpleWindowBorder, 0);
            GameUI.Hidepanel brpanel = ZeeConfig.gameUI.brpanel;
            setBottomRightCoord(Coord.of(brpanel.c.x, ZeeConfig.gameUI.sz.y),this);
        }

        public void draw(GOut g) {
            g.image(this.bg,Coord.z);
            super.draw(g);
        }

        public void mousemove(Coord c) {
            hit = c.isect(Coord.z, this.sz);
            if(!hit){
                exitMenu();
                println("exit");
                return;
            }
            super.mousemove(c);
        }
    }

    static class MenuWdgLine extends Widget{

        final int i, btnWidth, btnHeight;
        MenuGrid.PagButton btn;
        boolean isHoverLine;
        static BufferedImage bgHoverLine = ZeeManagerIcons.imgRect( 200, 32, Color.blue, false, 0);
        Label label;
        Coord lineTopRight;

        public MenuWdgLine(MenuGrid.PagButton btn, int i) {
            this.btn = btn;
            this.i =  i;
            this.btnWidth = btn.img().getWidth();
            this.btnHeight = btn.img().getHeight();
            int x=0;
            int y = i * this.btnHeight;
            this.add(new IButton(btn.img(), btn.img()), Coord.of(x,y));
            x += btn.img().getWidth();
            this.add(label=new Label(btn.name()), Coord.of(x,y));
            this.pack();
            this.lineTopRight = Coord.of(0,y);
        }

        public void draw(GOut g) {
            if (isHoverLine)
                g.image(bgHoverLine, this.lineTopRight);
            super.draw(g);
        }

        public void mousemove(Coord c) {
            int y = this.i * this.btn.img().getHeight();
            if( c.y > y   &&  c.y < (y + this.btn.img().getHeight()) ) {
                isHoverLine = true;
                //println(this.btn.name() + " " + c + " " + isHoverLine);
            }else{
                isHoverLine = false;
            }
            //super.mousemove(c);
        }
    }
}
